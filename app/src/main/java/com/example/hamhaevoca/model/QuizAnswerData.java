package com.example.hamhaevoca.model;

import java.io.Serializable;

public class QuizAnswerData implements Serializable {
    private final long itemId;
    private final String direction;
    private final String term;
    private final String meaning;
    private final String prompt;
    private final String selectedAnswer;
    private final String correctAnswer;
    private final boolean correct;
    private final boolean timedOut;
    private final long responseTimeMs;

    public QuizAnswerData(long itemId, String direction, String term, String meaning,
                          String prompt, String selectedAnswer, String correctAnswer,
                          boolean correct, boolean timedOut, long responseTimeMs) {
        this.itemId = itemId;
        this.direction = direction;
        this.term = term;
        this.meaning = meaning;
        this.prompt = prompt;
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.timedOut = timedOut;
        this.responseTimeMs = responseTimeMs;
    }

    public long getItemId() { return itemId; }
    public String getDirection() { return direction; }
    public String getTerm() { return term; }
    public String getMeaning() { return meaning; }
    public String getPrompt() { return prompt; }
    public String getSelectedAnswer() { return selectedAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public boolean isCorrect() { return correct; }
    public boolean isTimedOut() { return timedOut; }
    public long getResponseTimeMs() { return responseTimeMs; }
}
