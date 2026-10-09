package com.hireshield.ml.training;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.hireshield.ml.TextNormalizer;

/**
 * Reads fake_job_postings.csv (EMSCAD, the Kaggle "Real or Fake Job Posting" dataset).
 */
final class EmscadLoader {

    private static final String[] TEXT_COLUMNS = {
            "title", "company_profile", "description", "requirements", "benefits"};

    record Posting(String text, List<String> terms, boolean fraudulent) {
    }

    record LoadResult(List<Posting> postings, int rawRows, int duplicatesRemoved) {
    }

    private EmscadLoader() {
    }

    static LoadResult load(Path csv) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
        List<Posting> postings = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int rows = 0;
        int duplicates = 0;
        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                rows++;
                StringBuilder text = new StringBuilder();
                for (String column : TEXT_COLUMNS) {
                    String value = record.get(column);
                    if (value != null && !value.isBlank()) {
                        text.append(value).append('\n');
                    }
                }
                boolean fraud = "1".equals(record.get("fraudulent").trim());
                // The dataset has many re-posted ads. Keeping copies would let the same
                // ad sit in both train and test and make the scores look better than they are.
                String key = TextNormalizer.normalize(text.toString());
                if (!seen.add(key)) {
                    duplicates++;
                    continue;
                }
                postings.add(new Posting(text.toString(), TextNormalizer.terms(text.toString()), fraud));
            }
        }
        return new LoadResult(postings, rows, duplicates);
    }
}
