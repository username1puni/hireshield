package com.hireshield.ml.training;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.mnb.MultinomialNaiveBayesTrainer;
import org.tribuo.classification.sgd.linear.LinearSGDTrainer;
import org.tribuo.classification.sgd.objectives.Hinge;
import org.tribuo.classification.sgd.objectives.LogMulticlass;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.math.optimisers.AdaGrad;
import org.tribuo.provenance.SimpleDataSourceProvenance;

import com.hireshield.ml.LinearScamModel;
import com.hireshield.ml.TfidfVectorizer;
import com.hireshield.ml.training.EmscadLoader.Posting;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Offline training job. Run from the backend folder:
 *
 * <pre>
 *   ./mvnw -Ptrain compile exec:java -Dexec.args="--data ../data/fake_job_postings.csv --out src/main/resources/model"
 * </pre>
 *
 * Optional: {@code --extra a.csv,b.csv} adds more labelled data (columns text,label,source) to
 * training only, and {@code --tribuo-out dir} keeps the full Tribuo model file.
 *
 * <p>Protocol: de-duplicate, stratified 80/20 split of EMSCAD, 5-fold cross validation on the
 * training part to pick the model and its threshold, then one evaluation on the untouched 20%.
 */
public final class TrainModel {

    private static final long SEED = 42L;
    private static final int FOLDS = 5;
    private static final int MIN_DOC_FREQUENCY = 3;
    private static final double MAX_DOC_RATIO = 0.6;
    // 232k terms without a cap gave a 50 MB model for almost the same F1.
    private static final int MAX_FEATURES = 75_000;

    private static final LabelFactory LABELS = new LabelFactory();
    private static final Label FRAUD = new Label(TribuoClassifier.FRAUD);
    private static final Label REAL = new Label(TribuoClassifier.REAL);

    /** A way to score documents, built from a training fold. */
    private interface Scorer {
        double score(Map<String, Double> vector);
    }

    private record Candidate(String name, boolean linear, Supplier<List<Trainer<Label>>> trainers, boolean weighted) {
    }

    private record CvResult(Candidate candidate, double threshold, BinaryMetrics metrics) {
    }

    private TrainModel() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parseArgs(args);
        Path csv = Path.of(options.getOrDefault("data", "../data/fake_job_postings.csv"));
        Path outDir = Path.of(options.getOrDefault("out", "src/main/resources/model"));
        Files.createDirectories(outDir);

        EmscadLoader.LoadResult loaded = EmscadLoader.load(csv);
        List<Posting> all = loaded.postings();
        log("EMSCAD rows=%d, after de-duplication=%d (removed %d), fraudulent=%d",
                loaded.rawRows(), all.size(), loaded.duplicatesRemoved(), countFraud(all));

        List<Posting> train = new ArrayList<>();
        List<Posting> test = new ArrayList<>();
        stratifiedSplit(all, 0.2, train, test, SEED);
        log("train=%d (fraud %d), test=%d (fraud %d)", train.size(), countFraud(train), test.size(), countFraud(test));

        // Extra datasets go into training only. Their own 20% is reported separately.
        Map<String, List<Posting>> extraTests = new LinkedHashMap<>();
        List<Map<String, Object>> extraInfo = new ArrayList<>();
        if (options.containsKey("extra")) {
            for (String file : options.get("extra").split(",")) {
                ExtraDataLoader.Result extra = ExtraDataLoader.load(Path.of(file.trim()), all);
                List<Posting> extraTrain = new ArrayList<>();
                List<Posting> extraTest = new ArrayList<>();
                stratifiedSplit(extra.postings(), 0.2, extraTrain, extraTest, SEED);
                train.addAll(extraTrain);
                extraTests.put(extra.name(), extraTest);
                extraInfo.add(Map.of("name", extra.name(), "rows", extra.postings().size(),
                        "fraudulent", countFraud(extra.postings()), "droppedAsDuplicates", extra.dropped(),
                        "addedToTraining", extraTrain.size(), "heldOut", extraTest.size()));
                log("extra %s: kept=%d (fraud %d), dropped %d duplicates, train +%d, held out %d",
                        extra.name(), extra.postings().size(), countFraud(extra.postings()), extra.dropped(),
                        extraTrain.size(), extraTest.size());
            }
            Collections.shuffle(train, new Random(SEED));
        }

        List<Candidate> candidates = List.of(
                new Candidate("Logistic Regression (lr 0.5, 10 epochs)", true,
                        () -> List.of(logistic(0.5, 10)), true),
                new Candidate("Logistic Regression (lr 0.2, 20 epochs)", true,
                        () -> List.of(logistic(0.2, 20)), true),
                new Candidate("Logistic Regression (lr 1.0, 5 epochs)", true,
                        () -> List.of(logistic(1.0, 5)), true),
                new Candidate("Linear SVM (hinge loss)", false,
                        () -> List.of(new LinearSGDTrainer(new Hinge(), new AdaGrad(0.5, 0.1), 10, Integer.MAX_VALUE, 1, SEED)), true),
                new Candidate("Multinomial Naive Bayes", false,
                        () -> List.of(new MultinomialNaiveBayesTrainer(0.1)), false),
                new Candidate("Ensemble: average of Logistic Regression and Naive Bayes", false,
                        () -> List.of(logistic(0.5, 10), new MultinomialNaiveBayesTrainer(0.1)), true));

        List<CvResult> cvResults = new ArrayList<>();
        for (Candidate candidate : candidates) {
            CvResult result = crossValidate(candidate, train);
            cvResults.add(result);
            log("CV %-58s threshold=%.2f precision=%.3f recall=%.3f f1=%.3f prAuc=%.3f",
                    candidate.name(), result.threshold(), result.metrics().precision(),
                    result.metrics().recall(), result.metrics().f1(), result.metrics().averagePrecision());
        }

        // The served model has to be linear (see LinearScamModel), so pick the best linear one
        // by cross-validation F1. Every candidate is still evaluated on the test set for the report.
        CvResult best = cvResults.stream()
                .filter(r -> r.candidate().linear())
                .max((a, b) -> Double.compare(a.metrics().f1(), b.metrics().f1()))
                .orElseThrow();
        log("selected: %s", best.candidate().name());

        TfidfVectorizer vectorizer = TfidfVectorizer.fit(terms(train), MIN_DOC_FREQUENCY, MAX_DOC_RATIO, MAX_FEATURES);
        boolean[] actual = labels(test);
        Map<String, Object> testResults = new LinkedHashMap<>();
        Map<String, Object> extraResults = new LinkedHashMap<>();
        Model<Label> bestModel = null;
        for (CvResult result : cvResults) {
            List<Model<Label>> models = trainModels(result.candidate(), vectorizer, train);
            Scorer scorer = scorer(models);
            BinaryMetrics metrics = BinaryMetrics.of(score(scorer, vectorizer, test), actual, result.threshold());
            testResults.put(result.candidate().name(), metrics.toMap(result.threshold()));
            log("TEST %-58s precision=%.3f recall=%.3f f1=%.3f acc=%.3f prAuc=%.3f  TP=%d FP=%d TN=%d FN=%d",
                    result.candidate().name(), metrics.precision(), metrics.recall(), metrics.f1(), metrics.accuracy(),
                    metrics.averagePrecision(), metrics.truePositive(), metrics.falsePositive(),
                    metrics.trueNegative(), metrics.falseNegative());
            if (result == best) {
                bestModel = models.get(0);
            }
        }

        TribuoClassifier tribuo = new TribuoClassifier(bestModel);
        double[] bestScores = score(tribuo::fraudScore, vectorizer, test);
        BinaryMetrics bestTest = BinaryMetrics.of(bestScores, actual, best.threshold());
        BinaryMetrics atHalf = BinaryMetrics.of(bestScores, actual, 0.5);
        for (Map.Entry<String, List<Posting>> extra : extraTests.entrySet()) {
            BinaryMetrics m = BinaryMetrics.of(score(tribuo::fraudScore, vectorizer, extra.getValue()),
                    labels(extra.getValue()), best.threshold());
            extraResults.put(extra.getKey(), m.toMap(best.threshold()));
            log("EXTRA TEST %s: precision=%.3f recall=%.3f f1=%.3f (n=%d)", extra.getKey(),
                    m.precision(), m.recall(), m.f1(), extra.getValue().size());
        }

        // Export the compact serving model and check it agrees with Tribuo.
        TribuoClassifier.LinearExport export = tribuo.exportLinear();
        LinearScamModel serving = new LinearScamModel(vectorizer, export.weights(), export.bias(), best.threshold());
        double maxDiff = 0;
        for (Posting p : test) {
            Map<String, Double> v = vectorizer.transform(p.terms());
            maxDiff = Math.max(maxDiff, Math.abs(serving.probability(v) - tribuo.fraudScore(v)));
        }
        log("export check: max probability difference on the test set = %.2e", maxDiff);
        if (maxDiff > 1e-6) {
            throw new IllegalStateException("Exported model does not match Tribuo, max diff " + maxDiff);
        }

        ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        ObjectMapper compact = JsonMapper.builder().build();
        try (OutputStream out = Files.newOutputStream(outDir.resolve("vectorizer.json"))) {
            vectorizer.save(out, compact);
        }
        try (OutputStream out = Files.newOutputStream(outDir.resolve("scam-model.json"))) {
            LinearScamModel.saveWeights(out, compact, export.weights(), export.bias(), best.threshold());
        }
        if (options.containsKey("tribuo-out")) {
            Path tribuoDir = Path.of(options.get("tribuo-out"));
            Files.createDirectories(tribuoDir);
            bestModel.serializeToFile(tribuoDir.resolve("scam-model.tribuo"));
        }

        int realInTest = test.size() - countFraud(test);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("version", "emscad-" + Instant.now().toString().substring(0, 10) + (extraTests.isEmpty() ? "" : "-plus"));
        report.put("trainedAt", Instant.now().toString());
        report.put("dataset", Map.of(
                "name", "EMSCAD (Kaggle: Real or Fake Job Posting Prediction)",
                "rows", loaded.rawRows(),
                "afterDeduplication", all.size(),
                "duplicatesRemoved", loaded.duplicatesRemoved(),
                "fraudulent", countFraud(all),
                "trainSize", train.size(),
                "testSize", test.size(),
                "testFraudulent", countFraud(test)));
        report.put("extraDatasets", extraInfo);
        report.put("features", Map.of(
                "type", "TF-IDF unigrams + bigrams, sublinear tf, L2 normalised",
                "vocabularySize", vectorizer.vocabularySize(),
                "minDocFrequency", MIN_DOC_FREQUENCY,
                "maxFeatures", MAX_FEATURES));
        report.put("algorithm", best.candidate().name());
        report.put("threshold", best.threshold());
        report.put("crossValidation", cvTable(cvResults));
        report.put("test", bestTest.toMap(best.threshold()));
        report.put("testAtDefaultThreshold", atHalf.toMap(0.5));
        report.put("testAllModels", testResults);
        report.put("extraHeldOut", extraResults);
        report.put("baselineAlwaysReal", Map.of(
                "accuracy", Math.round(10000.0 * realInTest / test.size()) / 10000.0,
                "recall", 0.0));
        report.put("exportMaxDifference", maxDiff);
        mapper.writeValue(outDir.resolve("metrics.json").toFile(), report);
        log("wrote model, vectorizer and metrics to %s", outDir.toAbsolutePath());
    }

    private static LinearSGDTrainer logistic(double learningRate, int epochs) {
        return new LinearSGDTrainer(new LogMulticlass(), new AdaGrad(learningRate, 0.1), epochs, Integer.MAX_VALUE, 1, SEED);
    }

    private static CvResult crossValidate(Candidate candidate, List<Posting> train) {
        List<List<Posting>> folds = stratifiedFolds(train, FOLDS);
        List<Double> scores = new ArrayList<>();
        List<Boolean> actual = new ArrayList<>();
        for (int k = 0; k < FOLDS; k++) {
            List<Posting> fitPart = new ArrayList<>();
            for (int j = 0; j < FOLDS; j++) {
                if (j != k) {
                    fitPart.addAll(folds.get(j));
                }
            }
            TfidfVectorizer vectorizer = TfidfVectorizer.fit(terms(fitPart), MIN_DOC_FREQUENCY, MAX_DOC_RATIO, MAX_FEATURES);
            Scorer scorer = scorer(trainModels(candidate, vectorizer, fitPart));
            for (Posting p : folds.get(k)) {
                scores.add(scorer.score(vectorizer.transform(p.terms())));
                actual.add(p.fraudulent());
            }
        }
        double[] s = scores.stream().mapToDouble(Double::doubleValue).toArray();
        boolean[] a = new boolean[actual.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = actual.get(i);
        }
        double threshold = BinaryMetrics.bestThreshold(s, a);
        return new CvResult(candidate, threshold, BinaryMetrics.of(s, a, threshold));
    }

    /** Averages the fraud scores of one or more models. */
    private static Scorer scorer(List<Model<Label>> models) {
        List<TribuoClassifier> classifiers = models.stream().map(TribuoClassifier::new).toList();
        return vector -> classifiers.stream().mapToDouble(c -> c.fraudScore(vector)).average().orElse(0);
    }

    private static List<Model<Label>> trainModels(Candidate candidate, TfidfVectorizer vectorizer, List<Posting> postings) {
        Map<Posting, Map<String, Double>> vectors = new HashMap<>();
        postings.forEach(p -> vectors.put(p, vectorizer.transform(p.terms())));
        List<Model<Label>> models = new ArrayList<>();
        for (Trainer<Label> trainer : candidate.trainers().get()) {
            boolean weighted = candidate.weighted() && trainer instanceof LinearSGDTrainer;
            models.add(trainer.train(dataset(postings, vectors, weighted)));
        }
        return models;
    }

    private static MutableDataset<Label> dataset(List<Posting> postings, Map<Posting, Map<String, Double>> vectors,
                                                 boolean weighted) {
        long fraud = countFraud(postings);
        float fraudWeight = weighted ? (float) ((postings.size() - fraud) / (double) fraud) : 1f;
        var source = new ListDataSource<Label>(new ArrayList<>(), LABELS,
                new SimpleDataSourceProvenance("job-postings", LABELS));
        MutableDataset<Label> dataset = new MutableDataset<>(source);
        for (Posting p : postings) {
            var example = TribuoClassifier.toExample(vectors.get(p), p.fraudulent() ? FRAUD : REAL);
            if (p.fraudulent()) {
                example.setWeight(fraudWeight);
            }
            dataset.add(example);
        }
        return dataset;
    }

    private static double[] score(Scorer scorer, TfidfVectorizer vectorizer, List<Posting> postings) {
        double[] scores = new double[postings.size()];
        for (int i = 0; i < postings.size(); i++) {
            scores[i] = scorer.score(vectorizer.transform(postings.get(i).terms()));
        }
        return scores;
    }

    private static void stratifiedSplit(List<Posting> all, double testRatio, List<Posting> train, List<Posting> test,
                                        long seed) {
        Random random = new Random(seed);
        for (boolean fraud : new boolean[] {true, false}) {
            List<Posting> group = new ArrayList<>(all.stream().filter(p -> p.fraudulent() == fraud).toList());
            Collections.shuffle(group, random);
            int testCount = (int) Math.round(group.size() * testRatio);
            test.addAll(group.subList(0, testCount));
            train.addAll(group.subList(testCount, group.size()));
        }
        Collections.shuffle(train, random);
        Collections.shuffle(test, random);
    }

    private static List<List<Posting>> stratifiedFolds(List<Posting> postings, int k) {
        List<List<Posting>> folds = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            folds.add(new ArrayList<>());
        }
        Random random = new Random(SEED + 1);
        for (boolean fraud : new boolean[] {true, false}) {
            List<Posting> group = new ArrayList<>(postings.stream().filter(p -> p.fraudulent() == fraud).toList());
            Collections.shuffle(group, random);
            for (int i = 0; i < group.size(); i++) {
                folds.get(i % k).add(group.get(i));
            }
        }
        return folds;
    }

    private static List<Map<String, Object>> cvTable(List<CvResult> results) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CvResult r : results) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("model", r.candidate().name());
            row.putAll(r.metrics().toMap(r.threshold()));
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> terms(List<Posting> postings) {
        return postings.stream().map(Posting::terms).toList();
    }

    private static boolean[] labels(List<Posting> postings) {
        boolean[] labels = new boolean[postings.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = postings.get(i).fraudulent();
        }
        return labels;
    }

    private static int countFraud(List<Posting> postings) {
        return (int) postings.stream().filter(Posting::fraudulent).count();
    }

    private static Map<String, String> parseArgs(String[] args) throws IOException {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IOException("Unexpected argument " + args[i]);
            }
            options.put(args[i].substring(2), args[i + 1]);
        }
        return options;
    }

    private static void log(String format, Object... args) {
        System.out.printf(format + "%n", args);
    }
}
