package com.example.hamhaevoca.model;

import java.io.Serializable;
import java.util.ArrayList;

public class QuizQuestion implements Serializable {
    private final long itemId;
    private final String direction;
    private final String prompt;
    private final ArrayList<String> choices;
    private final String correctAnswer;
    private final String term;
    private final String meaning;

    public QuizQuestion(long itemId, String direction, String prompt, ArrayList<String> choices,
                        String correctAnswer, String term, String meaning) {
        this.itemId = itemId;
        this.direction = direction;
        this.prompt = prompt;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.term = term;
        this.meaning = meaning;
    }

    public long getItemId() { return itemId; }
    public String getDirection() { return direction; }
    public String getPrompt() { return prompt; }
    public ArrayList<String> getChoices() { return choices; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getTerm() { return term; }
    public String getMeaning() { return meaning; }
}
