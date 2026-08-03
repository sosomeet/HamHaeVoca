package com.example.hamhaevoca.util;

import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 모든 단어를 누락 없이 카드 맞추기 라운드에 배치한다.
 * 같은 라운드 안에서는 동일한 영단어나 동일한 뜻이 겹치지 않는다.
 */
public final class MatchingRoundPlanner {
    private MatchingRoundPlanner() {}

    public static ArrayList<ArrayList<VocabItem>> createRounds(List<VocabItem> source,
                                                               int maximumWords,
                                                               int roundSize,
                                                               Random random) {
        ArrayList<ArrayList<VocabItem>> rounds = new ArrayList<ArrayList<VocabItem>>();
        if (source == null || source.isEmpty()) return rounds;
        if (roundSize < 1) roundSize = 1;
        if (random == null) random = new Random();

        ArrayList<VocabItem> selected = new ArrayList<VocabItem>(source);
        Collections.shuffle(selected, random);
        if (maximumWords > 0 && selected.size() > maximumWords) {
            selected = new ArrayList<VocabItem>(selected.subList(0, maximumWords));
        }

        final Map<String, Integer> termFrequency = countTermFrequency(selected);
        final Map<String, Integer> meaningFrequency = countMeaningFrequency(selected);

        // 충돌이 많은 항목부터 배치하면 뒤에 한 항목짜리 라운드가 몰리는 현상을 줄일 수 있다.
        Collections.sort(selected, new Comparator<VocabItem>() {
            @Override
            public int compare(VocabItem first, VocabItem second) {
                int firstScore = frequencyScore(first, termFrequency, meaningFrequency);
                int secondScore = frequencyScore(second, termFrequency, meaningFrequency);
                return Integer.compare(secondScore, firstScore);
            }
        });

        ArrayList<Set<String>> roundTerms = new ArrayList<Set<String>>();
        ArrayList<Set<String>> roundMeanings = new ArrayList<Set<String>>();

        for (VocabItem item : selected) {
            String termKey = normalize(item.getTerm());
            String meaningKey = normalize(item.getMeaning());
            boolean placed = false;

            for (int i = 0; i < rounds.size(); i++) {
                ArrayList<VocabItem> round = rounds.get(i);
                if (round.size() >= roundSize) continue;
                if (roundTerms.get(i).contains(termKey)) continue;
                if (roundMeanings.get(i).contains(meaningKey)) continue;

                round.add(item);
                roundTerms.get(i).add(termKey);
                roundMeanings.get(i).add(meaningKey);
                placed = true;
                break;
            }

            if (!placed) {
                ArrayList<VocabItem> round = new ArrayList<VocabItem>();
                round.add(item);
                rounds.add(round);

                Set<String> terms = new HashSet<String>();
                terms.add(termKey);
                roundTerms.add(terms);

                Set<String> meanings = new HashSet<String>();
                meanings.add(meaningKey);
                roundMeanings.add(meanings);
            }
        }

        // 같은 조건에서도 매번 라운드와 표시 순서가 달라지도록 섞는다.
        for (ArrayList<VocabItem> round : rounds) Collections.shuffle(round, random);
        Collections.shuffle(rounds, random);
        return rounds;
    }

    private static int frequencyScore(VocabItem item,
                                      Map<String, Integer> termFrequency,
                                      Map<String, Integer> meaningFrequency) {
        Integer term = termFrequency.get(normalize(item.getTerm()));
        Integer meaning = meaningFrequency.get(normalize(item.getMeaning()));
        return (term == null ? 0 : term) + (meaning == null ? 0 : meaning);
    }

    private static Map<String, Integer> countTermFrequency(List<VocabItem> items) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (VocabItem item : items) add(result, normalize(item.getTerm()));
        return result;
    }

    private static Map<String, Integer> countMeaningFrequency(List<VocabItem> items) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (VocabItem item : items) add(result, normalize(item.getMeaning()));
        return result;
    }

    private static void add(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        map.put(key, value == null ? 1 : value + 1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
