package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizQuestion;
import com.example.hamhaevoca.model.VocabItem;
import com.example.hamhaevoca.util.QuizEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuizSetupActivity extends Activity {
    private long bookId;
    private RadioGroup modeGroup;
    private SeekBar timeSeekBar;
    private TextView timeValue;
    private TextView availableCountView;
    private EditText maxWordsInput;
    private List<VocabItem> words;
    private int availableQuestionCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_setup);
        bookId = getIntent().getLongExtra("book_id", -1);
        modeGroup = findViewById(R.id.quizModeGroup);
        timeSeekBar = findViewById(R.id.quizTimeSeekBar);
        timeValue = findViewById(R.id.quizTimeValue);
        availableCountView = findViewById(R.id.quizAvailableCount);
        maxWordsInput = findViewById(R.id.quizMaxWordsInput);
        words = DatabaseHelper.getInstance(this).getWords(bookId);

        timeSeekBar.setMax(15);
        timeSeekBar.setProgress(0);
        updateTimeLabel();
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateTimeLabel(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> updateAvailableCount());
        findViewById(R.id.startQuizButton).setOnClickListener(v -> startQuiz());
        updateAvailableCount();
    }

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + "초");
    }

    private String selectedMode() {
        int checked = modeGroup.getCheckedRadioButtonId();
        if (checked == R.id.modeMeaningToTerm) return QuizEngine.MEANING_TO_TERM;
        if (checked == R.id.modeMixed) return QuizEngine.MIXED;
        return QuizEngine.TERM_TO_MEANING;
    }

    private void updateAvailableCount() {
        ArrayList<QuizQuestion> questions = QuizEngine.createQuestions(words, selectedMode(), new Random(0));
        availableQuestionCount = questions.size();
        if (availableQuestionCount == 0) {
            availableCountView.setText("현재 설정으로 출제 가능한 문제가 없습니다.");
        } else {
            if (availableQuestionCount == words.size()) {
            availableCountView.setText("전체 " + words.size()
                    + "개 모두 출제 가능 · 중복 항목은 힌트로 구분됩니다.");
        } else {
            availableCountView.setText("전체 " + words.size() + "개 중 "
                    + availableQuestionCount + "개 출제 가능");
        }
        }
    }

    private int readMaximumQuestionCount() {
        String raw = maxWordsInput.getText().toString().trim();
        if (raw.isEmpty()) return availableQuestionCount;
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
        if (requested > availableQuestionCount) {
            maxWordsInput.setError("출제 가능한 " + availableQuestionCount + "개 이하로 입력하세요.");
            return -1;
        }
        return requested;
    }

    private void startQuiz() {
        ArrayList<QuizQuestion> questions = QuizEngine.createQuestions(words, selectedMode(), new Random());
        availableQuestionCount = questions.size();
        if (questions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("시험을 시작할 수 없음")
                    .setMessage("문제마다 정답을 포함한 5개의 서로 다른 보기가 필요합니다. "
                            + "현재 단어장에서는 보기를 만들 수 없습니다.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        int maximumQuestions = readMaximumQuestionCount();
        if (maximumQuestions < 1) return;

        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("mode", selectedMode());
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        intent.putExtra("max_words", maximumQuestions);
        startActivity(intent);
    }
}
