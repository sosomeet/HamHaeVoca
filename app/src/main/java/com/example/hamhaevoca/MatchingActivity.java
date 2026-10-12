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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;
import com.example.hamhaevoca.util.MatchingRoundPlanner;
import com.example.hamhaevoca.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MatchingActivity extends Activity {
    private long bookId;
    private int maximumWords;
    private int timeLimitSeconds = 5;
    private long roundDurationMillis = 5000L;
    private TextView roundText;
    private TextView matchingStats;
    private TextView matchingTimer;
    private TextView matchingFeedback;
    private LinearLayout leftColumn;
    private LinearLayout rightColumn;

    private final ArrayList<ArrayList<VocabItem>> plannedRounds =
            new ArrayList<ArrayList<VocabItem>>();
    private final ArrayList<VocabItem> remainingRoundItems =
            new ArrayList<VocabItem>();
    private int roundIndex = -1;
    private int totalSelectedCount = 0;
    private int completedCount = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int failedRounds = 0;
    private Button selectedLeft;
    private Button selectedRight;
    private boolean inputLocked = false;
    private CountDownTimer timer;
    private long remainingMillis = 5000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);
        bookId = getIntent().getLongExtra("book_id", -1);
        maximumWords = getIntent().getIntExtra("max_words", 0);
        timeLimitSeconds = Math.max(5, Math.min(20,
                getIntent().getIntExtra("time_limit", 5)));
        roundDurationMillis = timeLimitSeconds * 1000L;
        remainingMillis = roundDurationMillis;
        roundText = findViewById(R.id.matchingRoundText);
        matchingStats = findViewById(R.id.matchingStats);
        matchingTimer = findViewById(R.id.matchingTimer);
        matchingFeedback = findViewById(R.id.matchingFeedback);
        leftColumn = findViewById(R.id.leftMatchingColumn);
        rightColumn = findViewById(R.id.rightMatchingColumn);
        prepareItems();
    }

    private void prepareItems() {
        List<VocabItem> all = DatabaseHelper.getInstance(this).getWords(bookId);
        int minIncorrect = Math.max(0, getIntent().getIntExtra("min_incorrect", 0));
        List<VocabItem> eligible = new ArrayList<>();
        for (VocabItem word : all) {
            if (word.getIncorrectCount() >= minIncorrect) eligible.add(word);
        }
        plannedRounds.clear();
        plannedRounds.addAll(MatchingRoundPlanner.createRounds(
                eligible, maximumWords, 5, new Random()));

        for (ArrayList<VocabItem> round : plannedRounds) {
            totalSelectedCount += round.size();
        }

        if (totalSelectedCount < 1) {
            new AlertDialog.Builder(this)
                    .setTitle("게임을 시작할 수 없음")
                    .setMessage("학습할 단어가 없습니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        startNextRound();
    }

    private void startNextRound() {
        cancelTimer();
        roundIndex++;
        if (roundIndex >= plannedRounds.size()) {
            showCompletion();
            return;
        }
        remainingRoundItems.clear();
        remainingRoundItems.addAll(plannedRounds.get(roundIndex));
        renderRound();
    }

    private void renderRound() {
        inputLocked = false;
        selectedLeft = null;
        selectedRight = null;
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        matchingFeedback.setText("");

        List<VocabItem> left = new ArrayList<VocabItem>(remainingRoundItems);
        List<VocabItem> right = new ArrayList<VocabItem>(remainingRoundItems);
        Collections.shuffle(left);
        Collections.shuffle(right);
        for (VocabItem item : left) leftColumn.addView(createItemButton(item, true));
        for (VocabItem item : right) rightColumn.addView(createItemButton(item, false));
        updateHeader();
        startTimer(roundDurationMillis);
    }

    private Button createItemButton(VocabItem item, boolean left) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(left ? item.getTerm() : item.getMeaning());
        button.setTag(item.getId());
        button.setTextSize(16f);
        button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
        button.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        button.setTextColor(getResources().getColor(R.color.text_primary));
        button.setBackgroundResource(R.drawable.bg_choice_default);
        button.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        button.setStateListAnimator(null);
        button.setMinHeight(Ui.dp(this, 58));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, Ui.dp(this, 8));
        button.setLayoutParams(params);
        button.setOnClickListener(v -> selectButton((Button) v, left));
        return button;
    }

    private void selectButton(Button button, boolean left) {
        if (inputLocked || !button.isEnabled()) return;
        if (left) {
            if (selectedLeft != null) resetButtonStyle(selectedLeft);
            selectedLeft = button;
        } else {
            if (selectedRight != null) resetButtonStyle(selectedRight);
            selectedRight = button;
        }
        button.setBackgroundResource(R.drawable.bg_choice_selected);
        if (selectedLeft != null && selectedRight != null) evaluatePair();
    }

    private void evaluatePair() {
        inputLocked = true;
        long leftId = (Long) selectedLeft.getTag();
        long rightId = (Long) selectedRight.getTag();
        if (leftId == rightId) {
            correctCount++;
            selectedLeft.setBackgroundResource(R.drawable.bg_choice_correct);
            selectedRight.setBackgroundResource(R.drawable.bg_choice_correct);
            startTimer(roundDurationMillis);
            final Button left = selectedLeft;
            final Button right = selectedRight;
            final long matchedId = leftId;
            handler.postDelayed(() -> {
                leftColumn.removeView(left);
                rightColumn.removeView(right);
                removeRemainingItem(matchedId);
                selectedLeft = null;
                selectedRight = null;
                inputLocked = false;
                completedCount++;
                updateHeader();
                if (remainingRoundItems.isEmpty()) finishRound();
            }, 250);
        } else {
            wrongCount++;
            matchingFeedback.setText("남은 시간 1초 차감");
            selectedLeft.setBackgroundResource(R.drawable.bg_choice_wrong);
            selectedRight.setBackgroundResource(R.drawable.bg_choice_wrong);
            long reduced = Math.max(0, remainingMillis - 1000);
            startTimer(reduced);
            handler.postDelayed(() -> {
                resetButtonStyle(selectedLeft);
                resetButtonStyle(selectedRight);
                selectedLeft = null;
                selectedRight = null;
                inputLocked = false;
                matchingFeedback.setText("");
            }, 500);
        }
    }

    private void removeRemainingItem(long id) {
        for (int i = 0; i < remainingRoundItems.size(); i++) {
            if (remainingRoundItems.get(i).getId() == id) {
                remainingRoundItems.remove(i);
                return;
            }
        }
    }

    private void resetButtonStyle(Button button) {
        if (button != null) button.setBackgroundResource(R.drawable.bg_choice_default);
    }

    private void finishRound() {
        cancelTimer();
        if (roundIndex + 1 >= plannedRounds.size()) {
            showCompletion();
        } else {
            matchingFeedback.setText("라운드 완료");
            handler.postDelayed(this::startNextRound, 400);
        }
    }

    private void startTimer(long duration) {
        cancelTimer();
        remainingMillis = duration;
        if (duration <= 0) {
            onTimeOut();
            return;
        }
        timer = new CountDownTimer(duration, 100) {
            @Override public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                matchingTimer.setText((int) Math.ceil(millisUntilFinished / 1000.0) + "초");
            }

            @Override public void onFinish() {
                remainingMillis = 0;
                matchingTimer.setText("0초");
                onTimeOut();
            }
        }.start();
    }

    private void onTimeOut() {
        if (isFinishing() || remainingRoundItems.isEmpty()) return;
        inputLocked = true;
        failedRounds++;
        new AlertDialog.Builder(this)
                .setTitle("시간 초과")
                .setMessage("현재 남은 " + remainingRoundItems.size()
                        + "쌍을 다시 섞어서 시작합니다.")
                .setPositiveButton("다시 시작", (d, w) -> renderRound())
                .setNegativeButton("단어장으로", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showCompletion() {
        cancelTimer();
        Intent intent = new Intent(this, MatchingResultActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("max_words", maximumWords);
        intent.putExtra("time_limit", timeLimitSeconds);
        intent.putExtra("total_count", totalSelectedCount);
        intent.putExtra("completed_count", completedCount);
        intent.putExtra("correct_count", correctCount);
        intent.putExtra("wrong_count", wrongCount);
        intent.putExtra("failed_rounds", failedRounds);
        startActivity(intent);
        finish();
    }

    private void restartGame() {
        plannedRounds.clear();
        remainingRoundItems.clear();
        roundIndex = -1;
        totalSelectedCount = 0;
        completedCount = 0;
        correctCount = 0;
        wrongCount = 0;
        failedRounds = 0;
        prepareItems();
    }

    private void updateHeader() {
        roundText.setText("라운드 " + (roundIndex + 1) + " / " + plannedRounds.size());
        matchingStats.setText("완료 " + completedCount + " / " + totalSelectedCount
                + " · 정답 " + correctCount + " · 오답 " + wrongCount);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
