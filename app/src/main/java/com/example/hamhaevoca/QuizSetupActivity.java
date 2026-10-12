package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizQuestion;
import com.example.hamhaevoca.model.VocabItem;
import com.example.hamhaevoca.util.QuizEngine;
import com.example.hamhaevoca.util.PosTagParser;

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
    private EditText minIncorrectInput;
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
        minIncorrectInput = findViewById(R.id.quizMinIncorrectInput);
        minIncorrectInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateAvailableCount(); }
            @Override public void afterTextChanged(Editable e) {}
        });
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
        if (checked == R.id.modePartOfSpeech) return QuizEngine.PART_OF_SPEECH;
        return QuizEngine.TERM_TO_MEANING;
    }

    private int minimumIncorrectCount() {
        String raw = minIncorrectInput.getText().toString().trim();
        if (raw.isEmpty()) return 0;
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { return -1; }
    }

    private List<VocabItem> eligibleWords() {
        List<VocabItem> eligible = new ArrayList<>();
        int minimum = minimumIncorrectCount();
        if (minimum < 0) return eligible;
        for (VocabItem word : words) {
            if (word.getIncorrectCount() >= minimum) eligible.add(word);
        }
        return eligible;
    }

    private ArrayList<QuizQuestion> eligibleQuestions(String mode, Random random) {
        ArrayList<QuizQuestion> all = QuizEngine.createQuestions(words, mode, random);
        java.util.HashSet<Long> ids = new java.util.HashSet<>();
        for (VocabItem word : eligibleWords()) ids.add(word.getId());
        ArrayList<QuizQuestion> eligible = new ArrayList<>();
        for (QuizQuestion question : all) {
            if (ids.contains(question.getItemId())) eligible.add(question);
        }
        return eligible;
    }

    private void updateAvailableCount() {
        if (minimumIncorrectCount() < 0) {
            availableQuestionCount = 0;
            availableCountView.setText("0 이상의 숫자를 입력하세요.");
            return;
        }
        String mode = selectedMode();
        if (QuizEngine.PART_OF_SPEECH.equals(mode)) {
            availableQuestionCount = PosTagParser.filterTaggedItems(eligibleWords()).size();
        } else {
            availableQuestionCount = eligibleQuestions(mode, new Random(0)).size();
        }
        availableCountView.setText("출제 가능: " + availableQuestionCount + "개");
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
        if (minimumIncorrectCount() < 0) {
            minIncorrectInput.setError("0 이상의 정수를 입력하세요.");
            return;
        }
        String mode = selectedMode();
        if (QuizEngine.PART_OF_SPEECH.equals(mode)) {
            availableQuestionCount = PosTagParser.filterTaggedItems(eligibleWords()).size();
            if (availableQuestionCount == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("품사 시험을 시작할 수 없음")
                        .setMessage("뜻 앞에 (n), (v), (adj) 같은 지원 품사 태그가 필요합니다.")
                        .setPositiveButton("확인", null)
                        .show();
                return;
            }
            int maximumQuestions = readMaximumQuestionCount();
            if (maximumQuestions < 1) return;
            Intent intent = new Intent(this, PosQuizActivity.class);
            intent.putExtra("book_id", bookId);
            intent.putExtra("mode", mode);
            intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
            intent.putExtra("max_words", maximumQuestions);
            intent.putExtra("min_incorrect", minimumIncorrectCount());
            startActivity(intent);
            return;
        }

        ArrayList<QuizQuestion> questions = eligibleQuestions(mode, new Random());
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
        intent.putExtra("mode", mode);
        intent.putExtra("time_limit", timeSeekBar.getProgress() + 5);
        intent.putExtra("max_words", maximumQuestions);
        intent.putExtra("min_incorrect", minimumIncorrectCount());
        startActivity(intent);
    }
}
