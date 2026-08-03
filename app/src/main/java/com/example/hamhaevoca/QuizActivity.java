package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.QuizQuestion;
import com.example.hamhaevoca.model.VocabItem;
import com.example.hamhaevoca.util.QuizEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuizActivity extends Activity {
    private long bookId;
    private String mode;
    private int timeLimit;
    private ArrayList<QuizQuestion> questions;
    private final ArrayList<QuizAnswerData> answers = new ArrayList<QuizAnswerData>();
    private final Button[] choiceButtons = new Button[5];
    private TextView progressText;
    private TextView timerText;
    private TextView directionText;
    private TextView promptText;
    private ProgressBar progressBar;
    private int index = 0;
    private boolean answered = false;
    private CountDownTimer timer;
    private long questionStartedAt;
    private long remainingMillis;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        bookId = getIntent().getLongExtra("book_id", -1);
        mode = getIntent().getStringExtra("mode");
        timeLimit = getIntent().getIntExtra("time_limit", 5);
        int maximumWords = getIntent().getIntExtra("max_words", 0);
        List<VocabItem> words = DatabaseHelper.getInstance(this).getWords(bookId);
        questions = QuizEngine.createQuestions(words, mode, new Random());
        if (maximumWords > 0 && questions.size() > maximumWords) {
            questions = new ArrayList<QuizQuestion>(questions.subList(0, maximumWords));
        }

        progressText = findViewById(R.id.quizProgressText);
        timerText = findViewById(R.id.quizTimerText);
        directionText = findViewById(R.id.quizDirectionText);
        promptText = findViewById(R.id.quizPromptText);
        progressBar = findViewById(R.id.quizProgressBar);
        choiceButtons[0] = findViewById(R.id.choice1);
        choiceButtons[1] = findViewById(R.id.choice2);
        choiceButtons[2] = findViewById(R.id.choice3);
        choiceButtons[3] = findViewById(R.id.choice4);
        choiceButtons[4] = findViewById(R.id.choice5);
        for (int i = 0; i < choiceButtons.length; i++) {
            final int choiceIndex = i;
            choiceButtons[i].setOnClickListener(v -> selectAnswer(choiceIndex));
        }
        findViewById(R.id.quizBackButton).setOnClickListener(v -> confirmExit());

        if (questions == null || questions.isEmpty()) {
            finish();
            return;
        }
        progressBar.setMax(questions.size());
        showQuestion();
    }

    private void showQuestion() {
        if (index >= questions.size()) {
            finishQuiz();
            return;
        }
        answered = false;
        QuizQuestion question = questions.get(index);
        progressText.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress(index + 1);
        directionText.setText("term-to-meaning".equals(question.getDirection()) ? "영단어를 보고 뜻을 고르세요" : "뜻을 보고 영단어를 고르세요");
        promptText.setText(question.getPrompt());
        for (int i = 0; i < 5; i++) {
            choiceButtons[i].setText((i + 1) + ". " + question.getChoices().get(i));
            choiceButtons[i].setEnabled(true);
            choiceButtons[i].setBackgroundResource(R.drawable.bg_choice_default);
            choiceButtons[i].setTextColor(getResources().getColor(R.color.text_primary));
        }
        questionStartedAt = System.currentTimeMillis();
        remainingMillis = timeLimit * 1000L;
        startTimer(remainingMillis);
    }

    private void startTimer(long duration) {
        cancelTimer();
        timer = new CountDownTimer(duration, 100) {
            @Override public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                timerText.setText(seconds + "초");
            }
            @Override public void onFinish() {
                remainingMillis = 0;
                timerText.setText("0초");
                handleAnswer(null, true);
            }
        }.start();
    }

    private void selectAnswer(int choiceIndex) {
        if (answered) return;
        handleAnswer(questions.get(index).getChoices().get(choiceIndex), false);
    }

    private void handleAnswer(String selected, boolean timedOut) {
        if (answered) return;
        answered = true;
        cancelTimer();
        QuizQuestion question = questions.get(index);
        boolean correct = !timedOut && question.getCorrectAnswer().equals(selected);
        long responseTime = System.currentTimeMillis() - questionStartedAt;
        answers.add(new QuizAnswerData(question.getItemId(), question.getDirection(), question.getTerm(), question.getMeaning(),
                question.getPrompt(), selected, question.getCorrectAnswer(), correct, timedOut, responseTime));

        for (Button button : choiceButtons) button.setEnabled(false);
        for (int i = 0; i < 5; i++) {
            String value = question.getChoices().get(i);
            if (value.equals(question.getCorrectAnswer())) {
                choiceButtons[i].setBackgroundResource(R.drawable.bg_choice_correct);
                choiceButtons[i].setTextColor(getResources().getColor(R.color.text_primary));
            } else if (selected != null && value.equals(selected)) {
                choiceButtons[i].setBackgroundResource(R.drawable.bg_choice_wrong);
                choiceButtons[i].setTextColor(getResources().getColor(R.color.text_primary));
            }
        }
        handler.postDelayed(() -> {
            index++;
            showQuestion();
        }, 800);
    }

    private void finishQuiz() {
        cancelTimer();
        long attemptId = DatabaseHelper.getInstance(this).saveQuizAttempt(bookId, mode, timeLimit, answers);
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("attempt_id", attemptId);
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("시험 종료")
                .setMessage("현재 시험 기록은 저장되지 않습니다. 나갈까요?")
                .setPositiveButton("나가기", (d, w) -> finish())
                .setNegativeButton("계속", null)
                .show();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
