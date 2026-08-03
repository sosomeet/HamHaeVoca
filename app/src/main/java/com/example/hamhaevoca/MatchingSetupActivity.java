package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MatchingSetupActivity extends Activity {
    private long bookId;
    private EditText maxWordsInput;
    private SeekBar timeSeekBar;
    private TextView timeValue;
    private TextView availableCountView;
    private int eligibleCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_setup);

        bookId = getIntent().getLongExtra("book_id", -1);
        maxWordsInput = findViewById(R.id.matchingMaxWordsInput);
        timeSeekBar = findViewById(R.id.matchingTimeSeekBar);
        timeValue = findViewById(R.id.matchingTimeValue);
        availableCountView = findViewById(R.id.matchingAvailableCount);

        List<VocabItem> words = DatabaseHelper.getInstance(this).getWords(bookId);
        eligibleCount = countEligibleWords(words);
        availableCountView.setText("맞추기 가능 " + eligibleCount + "개 · 비우면 모두 사용");

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

        if (eligibleCount < 2) {
            new AlertDialog.Builder(this)
                    .setTitle("맞추기를 시작할 수 없음")
                    .setMessage("서로 구분 가능한 단어와 뜻이 최소 2쌍 필요합니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private int countEligibleWords(List<VocabItem> words) {
        Set<String> terms = new HashSet<String>();
        Set<String> meanings = new HashSet<String>();
        int count = 0;
        for (VocabItem item : words) {
            String termKey = item.getTerm().trim().toLowerCase(Locale.getDefault());
            String meaningKey = item.getMeaning().trim().toLowerCase(Locale.getDefault());
            if (terms.contains(termKey) || meanings.contains(meaningKey)) continue;
            terms.add(termKey);
            meanings.add(meaningKey);
            count++;
        }
        return count;
    }

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + "초");
    }

    private int readMaximumWordCount() {
        String raw = maxWordsInput.getText().toString().trim();
        if (raw.isEmpty()) return eligibleCount;
        final int requested;
        try {
            requested = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            maxWordsInput.setError("숫자를 입력하세요.");
            return -1;
        }
        if (requested < 2) {
            maxWordsInput.setError("2개 이상 입력하세요.");
            return -1;
        }
        if (requested > eligibleCount) {
            maxWordsInput.setError("맞추기 가능한 " + eligibleCount + "개 이하로 입력하세요.");
            return -1;
        }
        return requested;
    }

    private void startMatching() {
        int maximumWords = readMaximumWordCount();
        if (maximumWords < 2) return;
        Intent intent = new Intent(this, MatchingActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("max_words", maximumWords);
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        startActivity(intent);
    }
}
