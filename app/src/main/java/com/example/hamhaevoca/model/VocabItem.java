package com.example.hamhaevoca.model;

import java.io.Serializable;

public class VocabItem implements Serializable {
    private long id;
    private long bookId;
    private String term;
    private String meaning;
    private String example = "";
    private String past = "";
    private String pastParticiple = "";
    private int sortOrder;
    private boolean memorized;
    private int correctCount;
    private int incorrectCount;

    public VocabItem() {
    }

    public VocabItem(long id, long bookId, String term, String meaning, int sortOrder,
                     boolean memorized, int correctCount, int incorrectCount) {
        this.id = id;
        this.bookId = bookId;
        this.term = term;
        this.meaning = meaning;
        this.sortOrder = sortOrder;
        this.memorized = memorized;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
    }

    public VocabItem(String term, String meaning) {
        this(0, 0, term, meaning, 0, false, 0, 0);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getExample() { return example == null ? "" : example; }
    public void setExample(String example) { this.example = example == null ? "" : example; }
    public String getPast() { return past == null ? "" : past; }
    public void setPast(String value) { past = value == null ? "" : value; }
    public String getPastParticiple() { return pastParticiple == null ? "" : pastParticiple; }
    public void setPastParticiple(String value) { pastParticiple = value == null ? "" : value; }
    public boolean isVerb() { return !getPast().isEmpty() && !getPastParticiple().isEmpty(); }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isMemorized() { return memorized; }
    public void setMemorized(boolean memorized) { this.memorized = memorized; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }
}
