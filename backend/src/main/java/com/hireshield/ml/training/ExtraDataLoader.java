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
import com.hireshield.ml.training.EmscadLoader.Posting;

/**
 * Reads an extra labelled dataset with columns text,label,source. Rows that repeat an
 * EMSCAD posting (or each other) are dropped so nothing leaks into the test set.
 */
final class ExtraDataLoader {

    record Result(String name, List<Posting> postings, int dropped) {
    }

    private ExtraDataLoader() {
    }

    static Result load(Path csv, List<Posting> emscad) throws IOException {
        Set<String> seen = new HashSet<>();
        emscad.forEach(p -> seen.add(TextNormalizer.normalize(p.text())));
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        List<Posting> postings = new ArrayList<>();
        int dropped = 0;
        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                String text = record.get("text");
                if (text == null || text.isBlank() || !seen.add(TextNormalizer.normalize(text))) {
                    dropped++;
                    continue;
                }
                boolean fraud = "1".equals(record.get("label").trim());
                postings.add(new Posting(text, TextNormalizer.terms(text), fraud));
            }
        }
        String name = csv.getFileName().toString().replaceFirst("\\.csv$", "");
        return new Result(name, postings, dropped);
    }
}
