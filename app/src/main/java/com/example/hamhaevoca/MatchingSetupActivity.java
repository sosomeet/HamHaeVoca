package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;

import java.util.List;

public class MatchingSetupActivity extends Activity {
    private long bookId;
    private EditText maxWordsInput;
    private SeekBar timeSeekBar;
    private TextView timeValue;
    private TextView availableCountView;
    private int totalWordCount;
    private List<VocabItem> allWords;
    private EditText minIncorrectInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_setup);

        bookId = getIntent().getLongExtra("book_id", -1);
        maxWordsInput = findViewById(R.id.matchingMaxWordsInput);
        minIncorrectInput = findViewById(R.id.matchingMinIncorrectInput);
        timeSeekBar = findViewById(R.id.matchingTimeSeekBar);
        timeValue = findViewById(R.id.matchingTimeValue);
        availableCountView = findViewById(R.id.matchingAvailableCount);

        allWords = DatabaseHelper.getInstance(this).getWords(bookId);
        totalWordCount = allWords.size();
        minIncorrectInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAvailableCount();
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        updateAvailableCount();

        timeSeekBar.setMax(15);
        timeSeekBar.setProgress(0);
        updateTimeLabel();
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeLabel();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        findViewById(R.id.matchingSetupBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.startMatchingButton).setOnClickListener(v -> startMatching());

        if (totalWordCount < 1) {
            new AlertDialog.Builder(this)
                    .setTitle("맞추기를 시작할 수 없음")
                    .setMessage("단어가 1개 이상 필요합니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private int readMinimumIncorrect() {
        String text = minIncorrectInput.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try { return Integer.parseInt(text); }
        catch (NumberFormatException e) { return -1; }
    }

    private int eligibleCount() {
        int threshold = readMinimumIncorrect();
        if (threshold < 0) return 0;
        int count = 0;
        for (VocabItem word : allWords) {
            if (word.getIncorrectCount() >= threshold) count++;
        }
        return count;
    }

    private void updateAvailableCount() {
        int count = eligibleCount();
        availableCountView.setText("출제 가능: " + count + "개");
    }

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + "초");
    }

    private int readMaximumWordCount() {
        String raw = maxWordsInput.getText().toString().trim();
        int available = eligibleCount();
        if (raw.isEmpty()) return available;
        final int requested;
        try {
            requested = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            maxWordsInput.setError("숫자를 입력하세요.");
            return -1;
        }
        if (requested < 1) {
            maxWordsInput.setError("1개 이상 입력하세요.");
            return -1;
        }
        if (requested > available) {
            maxWordsInput.setError("출제 가능한 " + available + "개 이하로 입력하세요.");
            return -1;
        }
        return requested;
    }

    private void startMatching() {
        int minimum = readMinimumIncorrect();
        if (minimum < 0) {
            minIncorrectInput.setError("0 이상의 정수를 입력하세요.");
            return;
        }
        if (eligibleCount() < 1) {
            new AlertDialog.Builder(this).setTitle("맞추기를 시작할 수 없음")
                    .setMessage("조건에 맞는 단어가 없습니다.")
                    .setPositiveButton("확인", null).show();
            return;
        }
        int maximumWords = readMaximumWordCount();
        if (maximumWords < 1) return;
        Intent intent = new Intent(this, MatchingActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("max_words", maximumWords);
        intent.putExtra("min_incorrect", minimum);
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        startActivity(intent);
    }
}
