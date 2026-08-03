package com.example.hamhaevoca.model;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {
    private final List<ParsedRow> rows;
    private final int totalRows;
    private final int validRows;
    private final int invalidRows;
    private final int duplicateRows;

    public ParseResult(List<ParsedRow> rows, int totalRows, int validRows, int invalidRows, int duplicateRows) {
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

    public List<VocabItem> toVocabItems() {
        List<VocabItem> items = new ArrayList<VocabItem>();
        for (ParsedRow row : rows) {
            if (row.isValid() && !row.isDuplicate()) {
                items.add(new VocabItem(row.getTerm(), row.getMeaning()));
            }
        }
        return items;
    }
}
