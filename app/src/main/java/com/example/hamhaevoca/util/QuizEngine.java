package com.example.hamhaevoca.util;

import com.example.hamhaevoca.model.QuizQuestion;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 모든 단어를 누락 없이 객관식 문제로 변환한다.
 *
 * 같은 영단어나 뜻이 여러 번 등록된 경우에도 해당 항목을 제외하지 않는다.
 * 동일 질문에 정답이 여러 개일 수 있으면 정답의 최소 구분 힌트를 질문에 붙이고,
 * 같은 질문에 속하는 다른 정답은 오답 보기에서 제거해 정답이 하나만 남도록 한다.
 */
public final class QuizEngine {
    public static final String TERM_TO_MEANING = "term-to-meaning";
    public static final String MEANING_TO_TERM = "meaning-to-term";
    public static final String MIXED = "mixed";
    public static final String PART_OF_SPEECH = "part-of-speech";

    private static final int CHOICE_COUNT = 5;

    private QuizEngine() { }

    public static ArrayList<QuizQuestion> createQuestions(List<VocabItem> source,
                                                           String mode,
                                                           Random random) {
        ArrayList<QuizQuestion> result = new ArrayList<QuizQuestion>();
        if (source == null || source.isEmpty()) return result;
        if (random == null) random = new Random();

        // Verb books: choose present/past/participle individually, then build the
        // answer pool from the selected inflection, preserving original word IDs.
        ArrayList<VocabItem> projected = new ArrayList<VocabItem>();
        Map<Long, Integer> chosenStage = new HashMap<Long, Integer>();
        for (VocabItem original : source) {
            VocabItem copy = new VocabItem(original.getId(), original.getBookId(),
                    original.getTerm(), original.getMeaning(), original.getSortOrder(),
                    original.isMemorized(), original.getCorrectCount(), original.getIncorrectCount());
            int stage = original.isVerb() ? random.nextInt(3) : 0;
            if (stage == 1) copy.setTerm(original.getPast());
            else if (stage == 2) copy.setTerm(original.getPastParticiple());
            chosenStage.put(original.getId(), stage);
            projected.add(copy);
        }
        source = projected;
        Map<String, Integer> termCounts = countValues(source, true);
        Map<String, Integer> meaningCounts = countValues(source, false);
        ArrayList<VocabItem> shuffled = new ArrayList<VocabItem>(source);
        Collections.shuffle(shuffled, random);

        for (VocabItem item : shuffled) {
            ArrayList<String> directions = directionOrder(
                    item, mode, termCounts, meaningCounts, random);
            QuizQuestion question = null;
            for (String direction : directions) {
                question = buildQuestion(item, source, direction, random);
                if (question != null) break;
            }
            if (question != null) {
                int stage = chosenStage.get(item.getId());
                if (stage > 0) {
                    String label = stage == 1 ? "[2 · past] " : "[3 · past participle] ";
                    question = new QuizQuestion(question.getItemId(), question.getDirection(),
                            label + question.getPrompt(), question.getChoices(),
                            question.getCorrectAnswer(), question.getTerm(), question.getMeaning());
                }
                result.add(question);
            }
        }
        return result;
    }

    private static ArrayList<String> directionOrder(VocabItem item,
                                                        String mode,
                                                        Map<String, Integer> termCounts,
                                                        Map<String, Integer> meaningCounts,
                                                        Random random) {
        ArrayList<String> directions = new ArrayList<String>(2);
        boolean termUnique = countOf(termCounts, normalized(item.getTerm())) == 1;
        boolean meaningUnique = countOf(meaningCounts, normalized(item.getMeaning())) == 1;

        if (MIXED.equals(mode)) {
            if (termUnique && meaningUnique) {
                if (random.nextBoolean()) {
                    directions.add(TERM_TO_MEANING);
                    directions.add(MEANING_TO_TERM);
                } else {
                    directions.add(MEANING_TO_TERM);
                    directions.add(TERM_TO_MEANING);
                }
            } else if (termUnique) {
                directions.add(TERM_TO_MEANING);
                directions.add(MEANING_TO_TERM);
            } else if (meaningUnique) {
                directions.add(MEANING_TO_TERM);
                directions.add(TERM_TO_MEANING);
            } else {
                if (random.nextBoolean()) {
                    directions.add(TERM_TO_MEANING);
                    directions.add(MEANING_TO_TERM);
                } else {
                    directions.add(MEANING_TO_TERM);
                    directions.add(TERM_TO_MEANING);
                }
            }
            return directions;
        }

        String requested = MEANING_TO_TERM.equals(mode)
                ? MEANING_TO_TERM : TERM_TO_MEANING;
        String opposite = TERM_TO_MEANING.equals(requested)
                ? MEANING_TO_TERM : TERM_TO_MEANING;
        boolean requestedUnique = TERM_TO_MEANING.equals(requested)
                ? termUnique : meaningUnique;
        boolean oppositeUnique = TERM_TO_MEANING.equals(opposite)
                ? termUnique : meaningUnique;

        // 선택한 방향이 모호하고 반대 방향은 명확한 항목만 자동으로 뒤집는다.
        if (!requestedUnique && oppositeUnique) {
            directions.add(opposite);
            directions.add(requested);
        } else {
            directions.add(requested);
            directions.add(opposite);
        }
        return directions;
    }

    private static QuizQuestion buildQuestion(VocabItem item,
                                              List<VocabItem> source,
                                              String direction,
                                              Random random) {
        boolean termToMeaning = TERM_TO_MEANING.equals(direction);
        String rawPrompt = safe(termToMeaning ? item.getTerm() : item.getMeaning());
        String correct = safe(termToMeaning ? item.getMeaning() : item.getTerm());
        if (rawPrompt.isEmpty() || correct.isEmpty()) return null;

        String promptKey = normalized(rawPrompt);
        String correctKey = normalized(correct);

        // 같은 질문에 연결된 모든 정답을 수집한다. 이 값들은 오답 보기에 넣지 않는다.
        LinkedHashMap<String, String> validAnswersForPrompt = new LinkedHashMap<String, String>();
        for (VocabItem candidate : source) {
            String candidatePrompt = safe(termToMeaning
                    ? candidate.getTerm() : candidate.getMeaning());
            if (!normalized(candidatePrompt).equals(promptKey)) continue;
            String candidateAnswer = safe(termToMeaning
                    ? candidate.getMeaning() : candidate.getTerm());
            if (!candidateAnswer.isEmpty()) {
                validAnswersForPrompt.put(normalized(candidateAnswer), candidateAnswer);
            }
        }

        // 표시 문자열 기준으로 중복되지 않는 오답 후보를 만든다.
        LinkedHashMap<String, String> distractors = new LinkedHashMap<String, String>();
        for (VocabItem candidate : source) {
            if (candidate.getId() == item.getId()) continue;

            String candidateAnswer = safe(termToMeaning
                    ? candidate.getMeaning() : candidate.getTerm());
            String answerKey = normalized(candidateAnswer);
            if (answerKey.isEmpty() || answerKey.equals(correctKey)) continue;
            if (validAnswersForPrompt.containsKey(answerKey)) continue;
            if (!distractors.containsKey(answerKey)) {
                distractors.put(answerKey, candidateAnswer);
            }
        }

        ArrayList<String> pool = new ArrayList<String>(distractors.values());
        if (pool.size() < CHOICE_COUNT - 1) return null;
        Collections.shuffle(pool, random);

        ArrayList<String> choices = new ArrayList<String>(CHOICE_COUNT);
        choices.add(correct);
        for (int i = 0; i < pool.size() && choices.size() < CHOICE_COUNT; i++) {
            choices.add(pool.get(i));
        }
        Collections.shuffle(choices, random);

        String displayPrompt = rawPrompt;
        if (validAnswersForPrompt.size() > 1) {
            String hint = shortestUniquePrefix(correct, validAnswersForPrompt);
            String label = localizedQuizHint(termToMeaning);
            displayPrompt = rawPrompt + "\n(" + label + ": " + hint + ")";
        }

        return new QuizQuestion(item.getId(), direction, displayPrompt, choices, correct,
                safe(item.getTerm()), safe(item.getMeaning()));
    }

    /**
     * 같은 질문에 연결된 다른 정답과 구분되는 가장 짧은 접두사를 반환한다.
     * 같은 답이 완전히 중복된 경우에는 전체 답 대신 "동일 항목"을 표시한다.
     */
    private static String localizedQuizHint(boolean meaningHint) {
        String code = java.util.Locale.getDefault().getLanguage();
        switch (code) {
            case "en": return meaningHint ? "Meaning hint" : "English word hint";
            case "fr": return meaningHint ? "Indice de sens" : "Indice du mot anglais";
            case "es": return meaningHint ? "Pista del significado" : "Pista de la palabra inglesa";
            case "vi": return meaningHint ? "Gợi ý nghĩa" : "Gợi ý từ tiếng Anh";
            case "ja": return meaningHint ? "意味のヒント" : "英単語のヒント";
            case "zh": return meaningHint ? "释义提示" : "英语单词提示";
            default: return meaningHint ? "뜻 힌트" : "영단어 힌트";
        }
    }

    private static String localizedDuplicate() {
        switch (java.util.Locale.getDefault().getLanguage()) {
            case "en": return "Same entry";
            case "fr": return "Même entrée";
            case "es": return "Misma entrada";
            case "vi": return "Mục giống nhau";
            case "ja": return "同じ項目";
            case "zh": return "相同条目";
            default: return "동일 항목";
        }
    }

    private static String shortestUniquePrefix(String correct,
                                               Map<String, String> validAnswersForPrompt) {
        String value = safe(correct);
        String correctKey = normalized(value);
        int sameCount = 0;
        for (String key : validAnswersForPrompt.keySet()) {
            if (key.equals(correctKey)) sameCount++;
        }
        if (sameCount > 1) return localizedDuplicate();

        int codePointCount = value.codePointCount(0, value.length());
        for (int length = 1; length <= codePointCount; length++) {
            String prefix = prefixByCodePoints(value, length);
            String prefixKey = normalized(prefix);
            boolean unique = true;
            for (Map.Entry<String, String> entry : validAnswersForPrompt.entrySet()) {
                if (entry.getKey().equals(correctKey)) continue;
                if (normalized(entry.getValue()).startsWith(prefixKey)) {
                    unique = false;
                    break;
                }
            }
            if (unique) return length < codePointCount ? prefix + "…" : prefix;
        }
        return value;
    }

    private static String prefixByCodePoints(String value, int count) {
        int end = value.offsetByCodePoints(0, Math.min(count,
                value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }


    private static Map<String, Integer> countValues(List<VocabItem> source, boolean terms) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (VocabItem item : source) {
            String key = normalized(terms ? item.getTerm() : item.getMeaning());
            Integer count = counts.get(key);
            counts.put(key, count == null ? 1 : count + 1);
        }
        return counts;
    }

    private static int countOf(Map<String, Integer> counts, String key) {
        Integer count = counts.get(key);
        return count == null ? 0 : count;
    }
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalized(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }
}
