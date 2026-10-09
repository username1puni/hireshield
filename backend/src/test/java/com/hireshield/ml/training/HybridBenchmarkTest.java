package com.hireshield.ml.training;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hireshield.dto.AnalysisResponse;
import com.hireshield.dto.DomainReport;
import com.hireshield.dto.LinkReport;
import com.hireshield.entity.InputType;
import com.hireshield.ml.training.EmscadLoader.Posting;
import com.hireshield.service.AnalysisService;
import com.hireshield.service.DomainService;
import com.hireshield.service.LinkService;
import com.hireshield.service.ModelService;
import com.hireshield.util.TextPatterns;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the whole pipeline (rules + ML + trust, the same code the API uses) over the
 * held-out EMSCAD test ads and the scam-only SMS test set. The training job only measures
 * the ML model; this measures what a user actually sees. Skipped unless asked for:
 *
 * <pre>
 *   ./mvnw test -Dtest=HybridBenchmarkTest -Dbenchmark=true \
 *       -Dbenchmark.data=../data/fake_job_postings.csv -Dbenchmark.sms=../data/imc25_smishing.csv
 * </pre>
 *
 * Network lookups are stubbed, so domain age and Safe Browsing do not contribute here.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class HybridBenchmarkTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ModelService modelService;

    @MockitoBean
    private DomainService domainService;

    @MockitoBean
    private LinkService linkService;

    @Test
    void scoreHeldOutSets() throws Exception {
        when(domainService.check(anyString())).thenAnswer(inv ->
                new DomainReport(inv.getArgument(0), null, null, null, null, false, null, false));
        when(linkService.check(anyString())).thenAnswer(inv -> new LinkReport(inv.getArgument(0),
                TextPatterns.domainOf(inv.getArgument(0)), LinkReport.LinkVerdict.SAFE, List.of()));

        List<Posting> test = TrainModel.heldOutTest(Path.of(System.getProperty("benchmark.data")));
        List<Posting> sms = ScamOnlyTestSet.load(Path.of(System.getProperty("benchmark.sms")));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("emscadTest", score(test));
        report.put("smsTest", score(sms));
        ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        Path out = Path.of("target", "hybrid-benchmark.json");
        Files.createDirectories(out.getParent());
        mapper.writeValue(out.toFile(), report);
        System.out.println(mapper.writeValueAsString(report));
    }

    private Map<String, Object> score(List<Posting> postings) {
        int[] high = new int[4];
        int[] cautionOrHigh = new int[4];
        int[] mlOnly = new int[4];
        for (Posting p : postings) {
            AnalysisResponse r = analysisService.analyze(p.text(), InputType.TEXT, null, null, null, null);
            count(high, r.score() >= 60, p.fraudulent());
            count(cautionOrHigh, r.score() >= 30, p.fraudulent());
            count(mlOnly, modelService.predict(p.text()).flagged(), p.fraudulent());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", postings.size());
        result.put("scams", postings.stream().filter(Posting::fraudulent).count());
        result.put("mlModelAlone", table(mlOnly));
        result.put("hybridHigh", table(high));
        result.put("hybridCautionOrHigh", table(cautionOrHigh));
        return result;
    }

    /** tp, fp, tn, fn */
    private static void count(int[] cells, boolean flagged, boolean scam) {
        cells[scam ? (flagged ? 0 : 3) : (flagged ? 1 : 2)]++;
    }

    private static Map<String, Object> table(int[] c) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("truePositive", c[0]);
        t.put("falsePositive", c[1]);
        t.put("trueNegative", c[2]);
        t.put("falseNegative", c[3]);
        t.put("recall", round(c[0], c[0] + c[3]));
        if (c[1] + c[2] > 0) {
            t.put("precision", round(c[0], c[0] + c[1]));
            t.put("falseAlarmRate", round(c[1], c[1] + c[2]));
        }
        return t;
    }

    private static double round(int a, int b) {
        return b == 0 ? 0 : Math.round(10000.0 * a / b) / 10000.0;
    }
}
