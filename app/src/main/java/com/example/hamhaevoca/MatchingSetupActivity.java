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

public class MatchingSetupActivity extends LocalizedActivity {
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
                    .setTitle(Lang.text(MatchingSetupActivity.this, R.string.ui_405bdedda3ab))
                    .setMessage(Lang.text(MatchingSetupActivity.this, R.string.ui_913abb2ab55e))
                    .setPositiveButton(Lang.text(MatchingSetupActivity.this, R.string.ui_468266d639b5), (d, w) -> finish())
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
        availableCountView.setText(Lang.text(MatchingSetupActivity.this, R.string.ui_ccfae27393f5) + count + Lang.text(MatchingSetupActivity.this, R.string.ui_11600c9ada77));
    }

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + Lang.text(MatchingSetupActivity.this, R.string.ui_3845248e2861));
    }

    private int readMaximumWordCount() {
        String raw = maxWordsInput.getText().toString().trim();
        int available = eligibleCount();
        if (raw.isEmpty()) return available;
        final int requested;
        try {
            requested = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            maxWordsInput.setError(Lang.text(MatchingSetupActivity.this, R.string.ui_8c5c02f2a819));
            return -1;
        }
        if (requested < 1) {
            maxWordsInput.setError(Lang.text(MatchingSetupActivity.this, R.string.ui_9248737339ea));
            return -1;
        }
        if (requested > available) {
            maxWordsInput.setError(Lang.text(MatchingSetupActivity.this, R.string.ui_3fcc4d7b89a7) + available + Lang.text(MatchingSetupActivity.this, R.string.ui_7f64b5c1e784));
            return -1;
        }
        return requested;
    }

    private void startMatching() {
        int minimum = readMinimumIncorrect();
        if (minimum < 0) {
            minIncorrectInput.setError(Lang.text(MatchingSetupActivity.this, R.string.ui_98f4ac4a1828));
            return;
        }
        if (eligibleCount() < 1) {
            new AlertDialog.Builder(this).setTitle(Lang.text(MatchingSetupActivity.this, R.string.ui_405bdedda3ab))
                    .setMessage(Lang.text(MatchingSetupActivity.this, R.string.ui_df84c6cdff40))
                    .setPositiveButton(Lang.text(MatchingSetupActivity.this, R.string.ui_468266d639b5), null).show();
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
