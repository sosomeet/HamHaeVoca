package com.example.hamhaevoca.util;

import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 뜻 문자열에 포함된 8품사 태그를 읽고 화면 표시용 문자열로 변환한다.
 *
 * 지원 태그:
 * (n), (pron), (v), (adj), (adv), (prep), (conj), (interj)
 */
public final class PosTagParser {
    public static final String NOUN = "n";
    public static final String PRONOUN = "pron";
    public static final String VERB = "v";
    public static final String ADJECTIVE = "adj";
    public static final String ADVERB = "adv";
    public static final String PREPOSITION = "prep";
    public static final String CONJUNCTION = "conj";
    public static final String INTERJECTION = "interj";

    private static final String[] ORDER = new String[]{
            NOUN, PRONOUN, VERB, ADJECTIVE,
            ADVERB, PREPOSITION, CONJUNCTION, INTERJECTION
    };

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "\\((n|pron|v|adj|adv|prep|conj|interj)\\)",
            Pattern.CASE_INSENSITIVE);

    private PosTagParser() { }

    public static LinkedHashSet<String> parseTags(String meaning) {
        Set<String> found = new LinkedHashSet<String>();
        Matcher matcher = TAG_PATTERN.matcher(safe(meaning));
        while (matcher.find()) {
            found.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }

        LinkedHashSet<String> ordered = new LinkedHashSet<String>();
        for (String tag : ORDER) {
            if (found.contains(tag)) ordered.add(tag);
        }
        return ordered;
    }

    public static boolean hasSupportedTag(String meaning) {
        return !parseTags(meaning).isEmpty();
    }

    public static String stripTags(String meaning) {
        String stripped = TAG_PATTERN.matcher(safe(meaning)).replaceAll("");
        stripped = stripped.replaceAll("\\s+([,;])", "$1");
        stripped = stripped.replaceAll("([,;])(?=\\S)", "$1 ");
        stripped = stripped.replaceAll("\\s{2,}", " ");
        return stripped.trim();
    }

    public static ArrayList<VocabItem> filterTaggedItems(List<VocabItem> source) {
        ArrayList<VocabItem> result = new ArrayList<VocabItem>();
        if (source == null) return result;
        for (VocabItem item : source) {
            if (item != null && hasSupportedTag(item.getMeaning())) result.add(item);
        }
        return result;
    }

    public static List<String> allTags() {
        ArrayList<String> result = new ArrayList<String>();
        Collections.addAll(result, ORDER);
        return result;
    }

    public static String displayName(String tag) {
        if (NOUN.equals(tag)) return "명사 (n)";
        if (PRONOUN.equals(tag)) return "대명사 (pron)";
        if (VERB.equals(tag)) return "동사 (v)";
        if (ADJECTIVE.equals(tag)) return "형용사 (adj)";
        if (ADVERB.equals(tag)) return "부사 (adv)";
        if (PREPOSITION.equals(tag)) return "전치사 (prep)";
        if (CONJUNCTION.equals(tag)) return "접속사 (conj)";
        if (INTERJECTION.equals(tag)) return "감탄사 (interj)";
        return tag;
    }

    public static String compactLabel(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return "선택 없음";
        StringBuilder builder = new StringBuilder();
        for (String tag : ORDER) {
            if (!tags.contains(tag)) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append('(').append(tag).append(')');
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
