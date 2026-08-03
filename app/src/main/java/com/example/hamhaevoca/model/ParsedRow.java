package com.example.hamhaevoca.model;

public class ParsedRow {
    private final int rowNumber;
    private final String term;
    private final String meaning;
    private final boolean valid;
    private final boolean duplicate;
    private final String error;

    public ParsedRow(int rowNumber, String term, String meaning, boolean valid, boolean duplicate, String error) {
        this.rowNumber = rowNumber;
        this.term = term;
        this.meaning = meaning;
        this.valid = valid;
        this.duplicate = duplicate;
        this.error = error;
    }

    public int getRowNumber() { return rowNumber; }
    public String getTerm() { return term; }
    public String getMeaning() { return meaning; }
    public boolean isValid() { return valid; }
    public boolean isDuplicate() { return duplicate; }
    public String getError() { return error; }
}
