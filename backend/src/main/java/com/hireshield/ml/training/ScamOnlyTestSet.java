package com.hireshield.ml.training;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.hireshield.ml.TextNormalizer;
import com.hireshield.ml.training.EmscadLoader.Posting;

/**
 * A second test set that only has scam rows, for example the job-scam messages from the
 * IMC'25 smishing dataset. It is never used for training, only to measure recall on a
 * different kind of text (short SMS / WhatsApp messages instead of full job ads).
 *
 * <p>The dataset authors replaced personal data with tags like {@code <URL>} and
 * {@code <PHONE_NUMBER>}. Each tag is swapped for something the normalizer turns into the
 * same token a real link, email or phone number would give, so the model sees what it would
 * see in the app. Tags with no such token (names, dates, places) are dropped.
 */
final class ScamOnlyTestSet {

    private static final Pattern URL_TAG = Pattern.compile("<URL>");
    private static final Pattern PHONE_TAG = Pattern.compile("<PHONE_NUMBER>");
    private static final Pattern EMAIL_TAG = Pattern.compile("<EMAIL_ADDRESS>");
    private static final Pattern OTHER_TAG = Pattern.compile("<[A-Z_]+>");

    private ScamOnlyTestSet() {
    }

    static List<Posting> load(Path csv) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        List<Posting> postings = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                if (!"1".equals(record.get("label").trim())) {
                    continue;
                }
                String text = mapTags(record.get("text"));
                postings.add(new Posting(text, TextNormalizer.terms(text), true));
            }
        }
        return postings;
    }

    static String mapTags(String text) {
        String result = URL_TAG.matcher(text).replaceAll(" #URL_0# ");
        result = PHONE_TAG.matcher(result).replaceAll(" 9000000000 ");
        result = EMAIL_TAG.matcher(result).replaceAll(" #EMAIL_0# ");
        return OTHER_TAG.matcher(result).replaceAll(" ");
    }
}
