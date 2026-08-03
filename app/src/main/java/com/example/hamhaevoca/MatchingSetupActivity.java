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

import java.util.List;

public class MatchingSetupActivity extends Activity {
    private long bookId;
    private EditText maxWordsInput;
    private SeekBar timeSeekBar;
    private TextView timeValue;
    private TextView availableCountView;
    private int totalWordCount;

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
        totalWordCount = words.size();
        availableCountView.setText("전체 " + totalWordCount
                + "개를 사용할 수 있습니다. 중복 단어와 뜻은 서로 다른 라운드에 배치됩니다.");

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

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + "초");
    }

    private int readMaximumWordCount() {
        String raw = maxWordsInput.getText().toString().trim();
        if (raw.isEmpty()) return totalWordCount;
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
        if (requested > totalWordCount) {
            maxWordsInput.setError("전체 단어 수인 " + totalWordCount + "개 이하로 입력하세요.");
            return -1;
        }
        return requested;
    }

    private void startMatching() {
        int maximumWords = readMaximumWordCount();
        if (maximumWords < 1) return;
        Intent intent = new Intent(this, MatchingActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("max_words", maximumWords);
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        startActivity(intent);
    }
}
