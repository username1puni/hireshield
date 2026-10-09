package com.hireshield.ml.training;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

import com.hireshield.ml.ScamClassifier;
import com.hireshield.ml.TfidfVectorizer;
import com.hireshield.ml.training.EmscadLoader.Posting;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Offline training job. Run from the backend folder:
 *
 * <pre>
 *   ./mvnw -Ptrain compile exec:java -Dexec.args="../data/fake_job_postings.csv src/main/resources/model"
 * </pre>
 *
 * Steps: de-duplicate, stratified 80/20 split, 5-fold cross validation on the 80% to pick
 * the model and its threshold, then a single evaluation on the untouched 20%.
 */
public final class TrainModel {

    private static final long SEED = 42L;
    private static final int FOLDS = 5;
    private static final int MIN_DOC_FREQUENCY = 3;
    // 232k terms without a cap gave a 50 MB model for almost the same F1.
    private static final int MAX_FEATURES = 75_000;
    private static final double MAX_DOC_RATIO = 0.6;

    private static final LabelFactory LABELS = new LabelFactory();
    private static final Label FRAUD = new Label(ScamClassifier.FRAUD);
    private static final Label REAL = new Label(ScamClassifier.REAL);

    private record Candidate(String name, Supplier<Trainer<Label>> trainer, boolean weighted) {
    }

    private record CvResult(Candidate candidate, double threshold, BinaryMetrics metrics) {
    }

    private TrainModel() {
    }

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "../data/fake_job_postings.csv");
        Path outDir = Path.of(args.length > 1 ? args[1] : "src/main/resources/model");
        Files.createDirectories(outDir);

        EmscadLoader.LoadResult loaded = EmscadLoader.load(csv);
        List<Posting> all = loaded.postings();
        log("rows=%d, after de-duplication=%d (removed %d), fraudulent=%d",
                loaded.rawRows(), all.size(), loaded.duplicatesRemoved(), countFraud(all));

        List<Posting> train = new ArrayList<>();
        List<Posting> test = new ArrayList<>();
        stratifiedSplit(all, 0.2, train, test);
        log("train=%d (fraud %d), test=%d (fraud %d)", train.size(), countFraud(train), test.size(), countFraud(test));

        List<Candidate> candidates = List.of(
                new Candidate("Logistic Regression (SGD, class-weighted)",
                        () -> new LinearSGDTrainer(new LogMulticlass(), new AdaGrad(0.5, 0.1), 10, Integer.MAX_VALUE, 1, SEED), true),
                new Candidate("Linear SVM (hinge loss, class-weighted)",
                        () -> new LinearSGDTrainer(new Hinge(), new AdaGrad(0.5, 0.1), 10, Integer.MAX_VALUE, 1, SEED), true),
                new Candidate("Multinomial Naive Bayes",
                        () -> new MultinomialNaiveBayesTrainer(0.1), false));

        List<CvResult> cvResults = new ArrayList<>();
        for (Candidate candidate : candidates) {
            CvResult result = crossValidate(candidate, train);
            cvResults.add(result);
            log("CV %-45s threshold=%.2f precision=%.3f recall=%.3f f1=%.3f prAuc=%.3f",
                    candidate.name(), result.threshold(), result.metrics().precision(),
                    result.metrics().recall(), result.metrics().f1(), result.metrics().averagePrecision());
        }

        // Pick on cross-validation F1 only. The test set is not looked at until the end.
        CvResult best = cvResults.stream()
                .max((a, b) -> Double.compare(a.metrics().f1(), b.metrics().f1()))
                .orElseThrow();
        log("selected: %s", best.candidate().name());

        TfidfVectorizer vectorizer = TfidfVectorizer.fit(texts(train), MIN_DOC_FREQUENCY, MAX_DOC_RATIO, MAX_FEATURES);
        Model<Label> model = trainModel(best.candidate(), vectorizer, train);
        ScamClassifier classifier = new ScamClassifier(model, vectorizer, best.threshold());

        Map<String, Object> testResults = new LinkedHashMap<>();
        boolean[] actual = labels(test);
        for (CvResult result : cvResults) {
            Model<Label> m = result == best ? model : trainModel(result.candidate(), vectorizer, train);
            double[] scores = score(new ScamClassifier(m, vectorizer, result.threshold()), vectorizer, test);
            BinaryMetrics metrics = BinaryMetrics.of(scores, actual, result.threshold());
            testResults.put(result.candidate().name(), metrics.toMap(result.threshold()));
            log("TEST %-45s precision=%.3f recall=%.3f f1=%.3f acc=%.3f prAuc=%.3f rocAuc=%.3f  TP=%d FP=%d TN=%d FN=%d",
                    result.candidate().name(), metrics.precision(), metrics.recall(), metrics.f1(),
                    metrics.accuracy(), metrics.averagePrecision(), metrics.rocAuc(),
                    metrics.truePositive(), metrics.falsePositive(), metrics.trueNegative(), metrics.falseNegative());
        }
        double[] bestScores = score(classifier, vectorizer, test);
        BinaryMetrics bestTest = BinaryMetrics.of(bestScores, actual, best.threshold());
        BinaryMetrics atHalf = BinaryMetrics.of(bestScores, actual, 0.5);
        int realInTest = test.size() - countFraud(test);

        model.serializeToFile(outDir.resolve("scam-model.tribuo"));
        ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        try (OutputStream out = Files.newOutputStream(outDir.resolve("vectorizer.json"))) {
            vectorizer.save(out, mapper);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("version", "emscad-" + Instant.now().toString().substring(0, 10));
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
        report.put("baselineAlwaysReal", Map.of(
                "accuracy", Math.round(10000.0 * realInTest / test.size()) / 10000.0,
                "recall", 0.0));
        report.put("topScamTerms", topTerms(classifier));
        mapper.writeValue(outDir.resolve("metrics.json").toFile(), report);
        log("wrote model, vectorizer and metrics to %s", outDir.toAbsolutePath());
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
            TfidfVectorizer vectorizer = TfidfVectorizer.fit(texts(fitPart), MIN_DOC_FREQUENCY, MAX_DOC_RATIO, MAX_FEATURES);
            Model<Label> model = trainModel(candidate, vectorizer, fitPart);
            ScamClassifier classifier = new ScamClassifier(model, vectorizer, 0.5);
            for (Posting p : folds.get(k)) {
                scores.add(classifier.fraudScore(vectorizer.transform(p.terms())));
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

    private static Model<Label> trainModel(Candidate candidate, TfidfVectorizer vectorizer, List<Posting> postings) {
        long fraud = countFraud(postings);
        float fraudWeight = candidate.weighted() ? (float) ((postings.size() - fraud) / (double) fraud) : 1f;
        var source = new ListDataSource<Label>(new ArrayList<>(), LABELS,
                new SimpleDataSourceProvenance("emscad", LABELS));
        MutableDataset<Label> dataset = new MutableDataset<>(source);
        for (Posting p : postings) {
            var example = ScamClassifier.toExample(vectorizer.transform(p.terms()), p.fraudulent() ? FRAUD : REAL);
            if (p.fraudulent()) {
                example.setWeight(fraudWeight);
            }
            dataset.add(example);
        }
        return candidate.trainer().get().train(dataset);
    }

    private static double[] score(ScamClassifier classifier, TfidfVectorizer vectorizer, List<Posting> postings) {
        double[] scores = new double[postings.size()];
        for (int i = 0; i < postings.size(); i++) {
            scores[i] = classifier.fraudScore(vectorizer.transform(postings.get(i).terms()));
        }
        return scores;
    }

    private static void stratifiedSplit(List<Posting> all, double testRatio, List<Posting> train, List<Posting> test) {
        Random random = new Random(SEED);
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

    private static List<String> topTerms(ScamClassifier classifier) {
        // The terms with the strongest pull towards "fraud", for the report.
        var result = classifier.predict(
                "work from home data entry earn money weekly payment no experience required apply now", 10);
        return result.topTerms().stream().map(ScamClassifier.TermContribution::term).toList();
    }

    private static List<List<String>> texts(List<Posting> postings) {
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

    private static void log(String format, Object... args) {
        System.out.printf(format + "%n", args);
    }
}
