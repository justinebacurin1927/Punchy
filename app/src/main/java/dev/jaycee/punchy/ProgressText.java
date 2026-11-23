package dev.jaycee.punchy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Percentage parsing only; callers decide whether text belongs to a progress notification. */
final class ProgressText {
    private static final Pattern PERCENT = Pattern.compile("(?<![\\p{N}.,-])([\\p{N}]{1,3}(?:[.,][\\p{N}]{1,2})?)\\s*[%٪％]");
    private ProgressText() {}
    static int percent(CharSequence text) {
        if (text == null) return -1;
        Matcher matcher = PERCENT.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1).replace(',', '.');
            StringBuilder normalized = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                normalized.append(c == '.' ? '.' : (char) ('0' + Character.digit(c, 10)));
            }
            try {
                double number = Double.parseDouble(normalized.toString());
                if (number >= 0 && number <= 100) return (int) number;
            } catch (NumberFormatException ignored) { }
        }
        return -1;
    }
}
