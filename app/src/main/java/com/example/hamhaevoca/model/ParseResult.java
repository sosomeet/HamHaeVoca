package com.example.hamhaevoca.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ParseResult {
    private final List<ParsedRow> rows;
    private final int totalRows;
    private final int validRows;
    private final int invalidRows;
    private final int duplicateRows;

    public ParseResult(List<ParsedRow> rows, int totalRows, int validRows,
                       int invalidRows, int duplicateRows) {
        this.rows = rows;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.duplicateRows = duplicateRows;
    }

    public List<ParsedRow> getRows() { return rows; }
    public int getTotalRows() { return totalRows; }
    public int getValidRows() { return validRows; }
    public int getInvalidRows() { return invalidRows; }
    public int getDuplicateRows() { return duplicateRows; }

    public List<String> getAvailableDays() {
        Set<String> unique = new LinkedHashSet<String>();
        for (ParsedRow row : rows) {
            if (row.isValid() && !row.isDuplicate() && !row.getDay().isEmpty()) {
                unique.add(row.getDay());
            }
        }
        List<String> result = new ArrayList<String>(unique);
        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                Integer leftNumber = parseInteger(left);
                Integer rightNumber = parseInteger(right);
                if (leftNumber != null && rightNumber != null) {
                    return leftNumber.compareTo(rightNumber);
                }
                if (leftNumber != null) return -1;
                if (rightNumber != null) return 1;
                return left.compareToIgnoreCase(right);
            }
        });
        return result;
    }

    public int countValidRowsForDays(Set<String> selectedDays) {
        Set<String> seen = new LinkedHashSet<String>();
        for (ParsedRow row : rows) {
            if (shouldInclude(row, selectedDays)) {
                seen.add(wordKey(row));
            }
        }
        return seen.size();
    }

    public List<VocabItem> toVocabItems() {
        return toVocabItems(null);
    }

    public List<VocabItem> toVocabItems(Set<String> selectedDays) {
        List<VocabItem> items = new ArrayList<VocabItem>();
        Set<String> seen = new LinkedHashSet<String>();
        for (ParsedRow row : rows) {
            if (shouldInclude(row, selectedDays) && seen.add(wordKey(row))) {
                items.add(new VocabItem(row.getTerm(), row.getMeaning()));
            }
        }
        return items;
    }

    private boolean shouldInclude(ParsedRow row, Set<String> selectedDays) {
        if (!row.isValid() || row.isDuplicate()) return false;
        if (selectedDays == null || selectedDays.isEmpty()) return true;
        return !row.getDay().isEmpty() && selectedDays.contains(row.getDay());
    }


    private String wordKey(ParsedRow row) {
        return row.getTerm().trim().toLowerCase() + "\u0000" + row.getMeaning().trim();
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
