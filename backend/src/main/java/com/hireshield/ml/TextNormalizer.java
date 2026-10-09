package com.hireshield.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cleans raw job text before it is turned into features. The same steps run
 * during training and at prediction time, otherwise the model sees different
 * tokens than it was trained on.
 */
public final class TextNormalizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]{1,200}>");
    private static final Pattern URL = Pattern.compile("(https?://\\S+|www\\.\\S+|#url_[0-9a-f]+#)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("([\\w.+-]+@[\\w-]+\\.[\\w.]+|#email_[0-9a-f]+#)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s-]{8,}\\d|#phone_[0-9a-f]+#)");
    private static final Pattern MONEY = Pattern.compile("(₹|rs\\.?|inr|\\$|usd|£|€)\\s?\\d[\\d,]*(\\.\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9_ ]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    // Short list on purpose. Words like "no", "free" and "only" carry signal here.
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "with", "at", "by",
            "is", "are", "was", "were", "be", "been", "this", "that", "these", "those", "it", "its",
            "as", "from", "we", "our", "you", "your", "will", "can", "have", "has", "they", "their",
            "i", "us", "he", "she", "them", "which", "who", "what", "also", "all", "any", "into");

    private TextNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = HTML_TAG.matcher(raw).replaceAll(" ");
        text = URL.matcher(text).replaceAll(" urltoken ");
        text = EMAIL.matcher(text).replaceAll(" emailtoken ");
        text = MONEY.matcher(text).replaceAll(" moneytoken ");
        text = PHONE.matcher(text).replaceAll(" phonetoken ");
        text = text.toLowerCase(Locale.ROOT);
        text = NON_WORD.matcher(text).replaceAll(" ");
        return SPACES.matcher(text).replaceAll(" ").trim();
    }

    public static List<String> tokens(String normalized) {
        List<String> result = new ArrayList<>();
        if (normalized.isBlank()) {
            return result;
        }
        for (String token : normalized.split(" ")) {
            if (token.length() < 2 || token.length() > 30 || STOP_WORDS.contains(token)) {
                continue;
            }
            if (token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    /** Unigrams plus bigrams of neighbouring tokens. */
    public static List<String> terms(String raw) {
        List<String> tokens = tokens(normalize(raw));
        List<String> terms = new ArrayList<>(tokens.size() * 2);
        terms.addAll(tokens);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            terms.add(tokens.get(i) + " " + tokens.get(i + 1));
        }
        return terms;
    }
}
