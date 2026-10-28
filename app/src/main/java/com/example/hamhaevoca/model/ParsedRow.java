package com.example.hamhaevoca.model;

public class ParsedRow {
    private final int rowNumber;
    private final String term;
    private final String meaning;
    private final String day;
    private final String example;
    private final String past;
    private final String pastParticiple;
    private final boolean valid;
    private final boolean duplicate;
    private final String error;

    public ParsedRow(int rowNumber, String term, String meaning, boolean valid,
                     boolean duplicate, String error) {
        this(rowNumber, term, meaning, "", valid, duplicate, error);
    }

    public ParsedRow(int rowNumber, String term, String meaning, String day, boolean valid,
                     boolean duplicate, String error) {
        this(rowNumber, term, meaning, day, "", valid, duplicate, error);
    }

    public ParsedRow(int rowNumber, String term, String meaning, String day, String example, boolean valid, boolean duplicate, String error) {
        this(rowNumber, term, meaning, day, example, "", "", valid, duplicate, error);
    }

    public ParsedRow(int rowNumber, String term, String meaning, String day, String example, String past, String pastParticiple, boolean valid, boolean duplicate, String error) {
        this.past = past == null ? "" : past;
        this.pastParticiple = pastParticiple == null ? "" : pastParticiple;
        this.rowNumber = rowNumber;
        this.term = term;
        this.meaning = meaning;
        this.day = day == null ? "" : day;
        this.example = example == null ? "" : example;
        this.valid = valid;
        this.duplicate = duplicate;
        this.error = error;
    }

    public int getRowNumber() { return rowNumber; }
    public String getTerm() { return term; }
    public String getMeaning() { return meaning; }
    public String getPast() { return past; }
    public String getPastParticiple() { return pastParticiple; }
    public String getExample() { return example; }
    public String getDay() { return day; }
    public boolean isValid() { return valid; }
    public boolean isDuplicate() { return duplicate; }
    public String getError() { return error; }
}
