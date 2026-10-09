package com.hireshield.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls contact details and links out of free text: emails, URLs, phone numbers,
 * UPI IDs and money amounts.
 */
public final class TextPatterns {

    public static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");
    public static final Pattern URL = Pattern.compile(
            "\\b((https?://|www\\.)[^\\s<>\"')\\]]+|[a-z0-9-]+(\\.[a-z0-9-]+)*\\.(com|in|co|org|net|info|xyz|online|site|top|live|work|jobs|io|co\\.in|org\\.in|gov\\.in|nic\\.in)(/[^\\s<>\"')\\]]*)?)",
            Pattern.CASE_INSENSITIVE);
    // Indian mobile numbers with or without +91, plus generic international numbers.
    public static final Pattern PHONE = Pattern.compile("(\\+91[\\s-]?)?\\b[6-9]\\d{4}[\\s-]?\\d{5}\\b|\\+\\d{1,3}[\\s-]?\\d{6,12}\\b");
    // UPI handles look like name@okaxis or 98xxxxxx@ybl. They have no dot after the @.
    public static final Pattern UPI = Pattern.compile("\\b[A-Za-z0-9._-]{2,}@(ok[a-z]+|ybl|ibl|axl|paytm|upi|apl|yapl|icici|sbi|hdfcbank|axisbank|kotak|airtel|jio|fbl|pthdfc|ptsbi|ptyes|ptaxis)\\b",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern MONEY = Pattern.compile(
            "(₹|rs\\.?|inr|rupees?)\\s?(\\d[\\d,]*(\\.\\d+)?)\\s?(k|lakh|lakhs|lpa)?|(\\d[\\d,]*(\\.\\d+)?)\\s?(k|lakh|lakhs)?\\s?(₹|rs\\.?|inr|rupees|/-)",
            Pattern.CASE_INSENSITIVE);

    private TextPatterns() {
    }

    public static List<String> emails(String text) {
        List<String> result = new ArrayList<>();
        for (String match : findAll(EMAIL, text)) {
            if (!UPI.matcher(match).matches()) {
                result.add(match.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    public static List<String> upiIds(String text) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = UPI.matcher(text);
        while (matcher.find()) {
            // An email like a@ybl.com is not a UPI ID
            int end = matcher.end();
            if (end < text.length() && text.charAt(end) == '.' && end + 1 < text.length()
                    && Character.isLetter(text.charAt(end + 1))) {
                continue;
            }
            result.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(result);
    }

    public static List<String> urls(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = URL.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            // Skip the domain part of an email address
            if (start > 0 && text.charAt(start - 1) == '@') {
                continue;
            }
            // ...and the part before the @ (hr.jobs@gmail.com is not a link to hr.jobs)
            if (matcher.end() < text.length() && text.charAt(matcher.end()) == '@') {
                continue;
            }
            String url = matcher.group().replaceAll("[.,;:!?]+$", "");
            if (!result.contains(url)) {
                result.add(url);
            }
        }
        return result;
    }

    public static List<String> phones(String text) {
        List<String> result = new ArrayList<>();
        for (String phone : findAll(PHONE, text)) {
            String digits = phone.replaceAll("[^0-9+]", "");
            if (!result.contains(digits)) {
                result.add(digits);
            }
        }
        return result;
    }

    /** Domain of a URL or an email address, lower case, without "www.". */
    public static String domainOf(String urlOrEmail) {
        String value = urlOrEmail.toLowerCase(Locale.ROOT).trim();
        int at = value.lastIndexOf('@');
        if (at >= 0) {
            value = value.substring(at + 1);
        }
        value = value.replaceFirst("^[a-z]+://", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        if (value.startsWith("www.")) {
            value = value.substring(4);
        }
        return value;
    }

    /** Parses "₹5,000", "Rs 25k", "2 lakh" into rupees. Returns -1 when it cannot. */
    public static long parseAmount(String number, String suffix) {
        try {
            double value = Double.parseDouble(number.replace(",", ""));
            if (suffix != null) {
                String s = suffix.toLowerCase(Locale.ROOT);
                if (s.equals("k")) {
                    value *= 1_000;
                } else if (s.startsWith("lakh") || s.equals("lpa")) {
                    value *= 100_000;
                }
            }
            return (long) value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A short piece of text around a match, used as evidence in the result. */
    public static String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        while (from > 0 && !Character.isWhitespace(text.charAt(from - 1))) {
            from--;
            if (start - from > 60) {
                break;
            }
        }
        while (to < text.length() && !Character.isWhitespace(text.charAt(to))) {
            to++;
            if (to - end > 60) {
                break;
            }
        }
        String piece = text.substring(from, to).replaceAll("\\s+", " ").trim();
        return (from > 0 ? "..." : "") + piece + (to < text.length() ? "..." : "");
    }

    private static List<String> findAll(Pattern pattern, String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
}
