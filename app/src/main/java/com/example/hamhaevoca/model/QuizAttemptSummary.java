package com.example.hamhaevoca.model;

public class QuizAttemptSummary {
    private final long id;
    private final String mode;
    private final int timeLimit;
    private final int totalCount;
    private final int correctCount;
    private final int incorrectCount;
    private final int timedOutCount;
    private final String completedAt;

    public QuizAttemptSummary(long id, String mode, int timeLimit, int totalCount,
                              int correctCount, int incorrectCount, int timedOutCount,
                              String completedAt) {
        this.id = id;
        this.mode = mode;
        this.timeLimit = timeLimit;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
        this.timedOutCount = timedOutCount;
        this.completedAt = completedAt;
    }

    public long getId() { return id; }
    public String getMode() { return mode; }
    public int getTimeLimit() { return timeLimit; }
    public int getTotalCount() { return totalCount; }
    public int getCorrectCount() { return correctCount; }
    public int getIncorrectCount() { return incorrectCount; }
    public int getTimedOutCount() { return timedOutCount; }
    public String getCompletedAt() { return completedAt; }
}
