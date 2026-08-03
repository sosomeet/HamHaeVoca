package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_setup);
        bookId = getIntent().getLongExtra("book_id", -1);
        modeGroup = findViewById(R.id.quizModeGroup);
        timeSeekBar = findViewById(R.id.quizTimeSeekBar);
        timeValue = findViewById(R.id.quizTimeValue);

        timeSeekBar.setMax(15);
        timeSeekBar.setProgress(5);
        updateTimeLabel();
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateTimeLabel(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        findViewById(R.id.startQuizButton).setOnClickListener(v -> startQuiz());
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

    private void startQuiz() {
        List<VocabItem> words = DatabaseHelper.getInstance(this).getWords(bookId);
        ArrayList<QuizQuestion> questions = QuizEngine.createQuestions(words, selectedMode(), new Random());
        if (questions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("시험을 시작할 수 없음")
                    .setMessage("5개의 서로 다른 보기를 만들 수 있어야 합니다. 단어 또는 뜻의 중복을 확인하고 최소 5개 이상의 고유 단어를 준비하세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("mode", selectedMode());
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        startActivity(intent);
    }
}
