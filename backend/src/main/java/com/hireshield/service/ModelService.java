package com.hireshield.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.hireshield.entity.ModelVersion;
import com.hireshield.ml.LinearScamModel;
import com.hireshield.ml.MlResult;
import com.hireshield.repository.ModelVersionRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the trained Tribuo model once at startup and keeps the matching row in model_versions.
 */
@Slf4j
@Service
public class ModelService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ModelVersionRepository modelVersionRepository;
    private final String location;

    private volatile LinearScamModel model;
    private volatile JsonNode metrics;
    private volatile Long activeVersionId;

    public ModelService(ResourceLoader resourceLoader, ObjectMapper objectMapper,
                        ModelVersionRepository modelVersionRepository,
                        @Value("${app.model.location}") String location) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.modelVersionRepository = modelVersionRepository;
        this.location = location.endsWith("/") ? location : location + "/";
    }

    @PostConstruct
    void load() throws IOException {
        JsonNode metricsJson;
        try (InputStream in = resource("metrics.json").getInputStream()) {
            metricsJson = objectMapper.readTree(in);
        }
        try (InputStream weights = resource("scam-model.json").getInputStream();
             InputStream vectorizer = resource("vectorizer.json").getInputStream()) {
            this.model = LinearScamModel.load(weights, vectorizer, objectMapper);
        }
        this.metrics = metricsJson;
        this.activeVersionId = registerVersion(metricsJson);
        log.info("Loaded model {} ({} terms, threshold {})",
                metricsJson.path("version").asString(), model.vocabularySize(), model.threshold());
    }

    public MlResult predict(String text) {
        return model.predict(text, 8);
    }

    public JsonNode metrics() {
        return metrics;
    }

    public Long activeVersionId() {
        return activeVersionId;
    }

    private Long registerVersion(JsonNode m) {
        String version = m.path("version").asString();
        ModelVersion row = modelVersionRepository.findByVersion(version).orElseGet(ModelVersion::new);
        if (row.getId() == null) {
            JsonNode test = m.path("test");
            row.setVersion(version);
            row.setAlgorithm(m.path("algorithm").asString());
            row.setTrainedAt(Instant.parse(m.path("trainedAt").asString()));
            row.setPrecision(decimal(test, "precision"));
            row.setRecall(decimal(test, "recall"));
            row.setF1(decimal(test, "f1"));
            row.setPrAuc(decimal(test, "prAuc"));
            row.setThreshold(decimal(m, "threshold"));
            row.setFilePath(location + "scam-model.json");
        }
        modelVersionRepository.deactivateAll();
        row.setActive(true);
        return modelVersionRepository.save(row).getId();
    }

    private Resource resource(String name) {
        return resourceLoader.getResource(location + name);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.has(field) ? BigDecimal.valueOf(node.path(field).asDouble()) : null;
    }

    public Map<String, Object> summary() {
        return Map.of(
                "version", metrics.path("version").asString(),
                "algorithm", metrics.path("algorithm").asString(),
                "threshold", metrics.path("threshold").asDouble());
    }
}
