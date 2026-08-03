package com.example.hamhaevoca.util;

import com.example.hamhaevoca.model.QuizQuestion;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class QuizEngine {
    public static final String TERM_TO_MEANING = "term-to-meaning";
    public static final String MEANING_TO_TERM = "meaning-to-term";
    public static final String MIXED = "mixed";

    private QuizEngine() {}

    public static ArrayList<QuizQuestion> createQuestions(List<VocabItem> source, String mode, Random random) {
        ArrayList<QuizQuestion> result = new ArrayList<QuizQuestion>();
        if (source == null || source.size() < 5) return result;

        Map<String, Integer> termCounts = countTerms(source);
        Map<String, Integer> meaningCounts = countMeanings(source);
        List<VocabItem> shuffled = new ArrayList<VocabItem>(source);
        Collections.shuffle(shuffled, random);

        for (VocabItem item : shuffled) {
            String direction = mode;
            if (MIXED.equals(mode)) direction = random.nextBoolean() ? TERM_TO_MEANING : MEANING_TO_TERM;

            QuizQuestion question = buildQuestion(item, source, direction, termCounts, meaningCounts, random);
            if (question == null && MIXED.equals(mode)) {
                String alternative = TERM_TO_MEANING.equals(direction) ? MEANING_TO_TERM : TERM_TO_MEANING;
                question = buildQuestion(item, source, alternative, termCounts, meaningCounts, random);
            }
            if (question != null) result.add(question);
        }
        return result;
    }

    private static QuizQuestion buildQuestion(VocabItem item, List<VocabItem> source, String direction,
                                              Map<String, Integer> termCounts, Map<String, Integer> meaningCounts,
                                              Random random) {
        boolean termToMeaning = TERM_TO_MEANING.equals(direction);
        String prompt = termToMeaning ? item.getTerm() : item.getMeaning();
        String correct = termToMeaning ? item.getMeaning() : item.getTerm();

        if (termToMeaning && getCount(termCounts, normalized(item.getTerm())) > 1) return null;
        if (!termToMeaning && getCount(meaningCounts, normalized(item.getMeaning())) > 1) return null;

        Set<String> unique = new HashSet<String>();
        for (VocabItem candidate : source) {
            String answer = termToMeaning ? candidate.getMeaning() : candidate.getTerm();
            unique.add(answer.trim());
        }
        if (unique.size() < 5) return null;

        ArrayList<String> pool = new ArrayList<String>();
        for (String answer : unique) if (!answer.equals(correct)) pool.add(answer);
        Collections.shuffle(pool, random);
        ArrayList<String> choices = new ArrayList<String>();
        choices.add(correct);
        for (int i = 0; i < pool.size() && choices.size() < 5; i++) choices.add(pool.get(i));
        if (choices.size() < 5) return null;
        Collections.shuffle(choices, random);

        return new QuizQuestion(item.getId(), direction, prompt, choices, correct,
                item.getTerm(), item.getMeaning());
    }

    private static Map<String, Integer> countTerms(List<VocabItem> items) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (VocabItem item : items) add(counts, normalized(item.getTerm()));
        return counts;
    }

    private static Map<String, Integer> countMeanings(List<VocabItem> items) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (VocabItem item : items) add(counts, normalized(item.getMeaning()));
        return counts;
    }

    private static void add(Map<String, Integer> map, String key) {
        Integer count = map.get(key);
        map.put(key, count == null ? 1 : count + 1);
    }

    private static int getCount(Map<String, Integer> map, String key) {
        Integer count = map.get(key);
        return count == null ? 0 : count;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
