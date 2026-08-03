package com.example.hamhaevoca.parser;

import com.example.hamhaevoca.model.ParseResult;
import com.example.hamhaevoca.model.ParsedRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DelimitedTextParser {
    private DelimitedTextParser() {}

    public static ParseResult parse(String input, String delimiterOption) {
        if (input == null) input = "";
        input = stripBom(input);
        char delimiter = delimiterFor(input, delimiterOption);
        List<List<String>> table = parseRows(input, delimiter);
        return normalize(table);
    }

    private static String stripBom(String value) {
        if (value.startsWith("\uFEFF")) return value.substring(1);
        return value;
    }

    private static char delimiterFor(String input, String option) {
        if ("탭".equals(option)) return '\t';
        if ("쉼표".equals(option)) return ',';
        if ("세미콜론".equals(option)) return ';';
        if ("파이프".equals(option)) return '|';
        return detectDelimiter(input);
    }

    public static char detectDelimiter(String input) {
        char[] candidates = new char[]{'\t', ',', ';', '|'};
        int bestScore = -1;
        char best = '\t';
        String[] lines = input.split("\\r?\\n", 12);
        for (char candidate : candidates) {
            int score = 0;
            int consistentColumns = -1;
            for (String line : lines) {
                if (line.trim().length() == 0) continue;
                List<String> cells = parseSingleLine(line, candidate);
                if (cells.size() >= 2) {
                    if (consistentColumns == -1) consistentColumns = cells.size();
                    if (cells.size() == consistentColumns) score += 3;
                    else score += 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static List<List<String>> parseRows(String input, char delimiter) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> currentRow = new ArrayList<String>();
        StringBuilder currentCell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < input.length() && input.charAt(i + 1) == '"') {
                    currentCell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == delimiter && !quoted) {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<String>();
            } else {
                currentCell.append(c);
            }
        }
        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentCell.toString());
            rows.add(currentRow);
        }
        return rows;
    }

    private static List<String> parseSingleLine(String line, char delimiter) {
        List<List<String>> rows = parseRows(line, delimiter);
        if (rows.isEmpty()) return new ArrayList<String>();
        return rows.get(0);
    }

    public static ParseResult normalize(List<List<String>> table) {
        List<ParsedRow> rows = new ArrayList<ParsedRow>();
        Set<String> seen = new HashSet<String>();
        int valid = 0;
        int invalid = 0;
        int duplicate = 0;
        int total = 0;
        boolean firstDataRowSkipped = false;

        for (int i = 0; i < table.size(); i++) {
            List<String> cells = table.get(i);
            boolean allEmpty = true;
            for (String cell : cells) {
                if (cell != null && cell.trim().length() > 0) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) continue;
            if (!firstDataRowSkipped) {
                firstDataRowSkipped = true;
                continue;
            }
            total++;

            String term = cells.size() > 0 ? normalizeText(cells.get(0)) : "";
            String meaning = cells.size() > 1 ? normalizeText(cells.get(1)) : "";
            String day = cells.size() > 2 ? normalizeDay(cells.get(2)) : "";

            if (term.length() == 0 || meaning.length() == 0) {
                invalid++;
                rows.add(new ParsedRow(i + 1, term, meaning, day, false, false, "단어 또는 뜻이 비어 있습니다."));
                continue;
            }

            String key = term.toLowerCase(Locale.US) + "\u0000" + meaning + "\u0000" + day;
            if (seen.contains(key)) {
                duplicate++;
                rows.add(new ParsedRow(i + 1, term, meaning, day, true, true, "중복 항목입니다."));
                continue;
            }
            seen.add(key);
            valid++;
            rows.add(new ParsedRow(i + 1, term, meaning, day, true, false, ""));
        }
        return new ParseResult(rows, total, valid, invalid, duplicate);
    }

    private static String normalizeText(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeDay(String value) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) return "";
        normalized = normalized.replaceAll("(?i)^day\\s*", "").trim();
        if (normalized.matches("\\d+\\.0+")) {
            normalized = normalized.substring(0, normalized.indexOf('.'));
        }
        return normalized;
    }

    private static boolean looksLikeHeader(String term, String meaning) {
        String left = term.toLowerCase(Locale.US);
        String right = meaning.toLowerCase(Locale.US);
        boolean termHeader = left.equals("word") || left.equals("vocabulary") || left.equals("term") ||
                left.equals("english") || left.equals("단어") || left.equals("영단어") || left.equals("영어");
        boolean meaningHeader = right.equals("meaning") || right.equals("definition") || right.equals("translation") ||
                right.equals("korean") || right.equals("뜻") || right.equals("의미") || right.equals("해석") || right.equals("한글");
        return termHeader && meaningHeader;
    }
}
