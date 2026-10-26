package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioButton;
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

public class QuizSetupActivity extends LocalizedActivity {
    private long bookId;
    private RadioButton[] modeButtons;
    private int selectedModeId = R.id.modeTermToMeaning;
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
        modeButtons = new RadioButton[]{
                findViewById(R.id.modeTermToMeaning),
                findViewById(R.id.modeMeaningToTerm),
                findViewById(R.id.modeMixed),
                findViewById(R.id.modePartOfSpeech)
        };
        for (RadioButton button : modeButtons) {
            button.setOnClickListener(v -> {
                selectedModeId = v.getId();
                for (RadioButton item : modeButtons) item.setChecked(item.getId() == selectedModeId);
                updateAvailableCount();
            });
        }
        modeButtons[0].setChecked(true);
        findViewById(R.id.quizSetupBackButton).setOnClickListener(v -> finish());
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

        findViewById(R.id.startQuizButton).setOnClickListener(v -> startQuiz());
        updateAvailableCount();
    }

    private void updateTimeLabel() {
        timeValue.setText((timeSeekBar.getProgress() + 5) + Lang.text(QuizSetupActivity.this, R.string.ui_3845248e2861));
    }

    private String selectedMode() {
        int checked = selectedModeId;
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
            availableCountView.setText(Lang.text(QuizSetupActivity.this, R.string.ui_381be7ceea62));
            return;
        }
        String mode = selectedMode();
        if (QuizEngine.PART_OF_SPEECH.equals(mode)) {
            availableQuestionCount = PosTagParser.filterTaggedItems(eligibleWords()).size();
        } else {
            availableQuestionCount = eligibleQuestions(mode, new Random(0)).size();
        }
        availableCountView.setText(Lang.text(QuizSetupActivity.this, R.string.ui_ccfae27393f5) + availableQuestionCount + Lang.text(QuizSetupActivity.this, R.string.ui_11600c9ada77));
    }

    private int readMaximumQuestionCount() {
        String raw = maxWordsInput.getText().toString().trim();
        if (raw.isEmpty()) return availableQuestionCount;
        final int requested;
        try {
            requested = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            maxWordsInput.setError(Lang.text(QuizSetupActivity.this, R.string.ui_8c5c02f2a819));
            return -1;
        }
        if (requested < 1) {
            maxWordsInput.setError(Lang.text(QuizSetupActivity.this, R.string.ui_9248737339ea));
            return -1;
        }
        if (requested > availableQuestionCount) {
            maxWordsInput.setError(Lang.text(QuizSetupActivity.this, R.string.ui_3fcc4d7b89a7) + availableQuestionCount + Lang.text(QuizSetupActivity.this, R.string.ui_7f64b5c1e784));
            return -1;
        }
        return requested;
    }

    private void startQuiz() {
        if (minimumIncorrectCount() < 0) {
            minIncorrectInput.setError(Lang.text(QuizSetupActivity.this, R.string.ui_98f4ac4a1828));
            return;
        }
        String mode = selectedMode();
        if (QuizEngine.PART_OF_SPEECH.equals(mode)) {
            availableQuestionCount = PosTagParser.filterTaggedItems(eligibleWords()).size();
            if (availableQuestionCount == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(Lang.text(QuizSetupActivity.this, R.string.ui_c9cc22f3f11b))
                        .setMessage(Lang.text(QuizSetupActivity.this, R.string.ui_725462b061e6))
                        .setPositiveButton(Lang.text(QuizSetupActivity.this, R.string.ui_468266d639b5), null)
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
                    .setTitle(Lang.text(QuizSetupActivity.this, R.string.ui_f227db66881f))
                    .setMessage(Lang.text(QuizSetupActivity.this, R.string.ui_bc66d19591a8)
                            + Lang.text(QuizSetupActivity.this, R.string.ui_4b8a9b0116a0))
                    .setPositiveButton(Lang.text(QuizSetupActivity.this, R.string.ui_468266d639b5), null)
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
