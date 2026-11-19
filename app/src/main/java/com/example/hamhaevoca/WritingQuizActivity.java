package com.example.hamhaevoca;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.VocabItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WritingQuizActivity extends LocalizedActivity {
    private long bookId;
    private boolean meaningToTerm;
    private final ArrayList<VocabItem> questions = new ArrayList<>();
    private final ArrayList<QuizAnswerData> answers = new ArrayList<>();
    private int index = 0;
    private boolean answered = false;
    private long startedAt;
    private TextView progress, direction, prompt, feedback;
    private ProgressBar progressBar;
    private EditText input;
    private Button check, next;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_writing_quiz);
        bookId = getIntent().getLongExtra("book_id", -1);
        meaningToTerm = getIntent().getBooleanExtra("meaning_to_term", true);
        int minimum = Math.max(0, getIntent().getIntExtra("min_incorrect", 0));
        List<VocabItem> all = DatabaseHelper.getInstance(this).getWords(bookId);
        for (VocabItem word : all) {
            if (word.getIncorrectCount() >= minimum && !word.getTerm().trim().isEmpty() && !word.getMeaning().trim().isEmpty()) questions.add(word);
        }
        Collections.shuffle(questions);
        int maximum = getIntent().getIntExtra("max_words", 0);
        if (maximum > 0 && maximum < questions.size()) questions.subList(maximum, questions.size()).clear();
        if (questions.isEmpty()) { Toast.makeText(this, R.string.writing_no_words, Toast.LENGTH_SHORT).show(); finish(); return; }
        progress = findViewById(R.id.writingProgress);
        direction = findViewById(R.id.writingDirection);
        prompt = findViewById(R.id.writingPrompt);
        progressBar = findViewById(R.id.writingProgressBar);
        input = findViewById(R.id.writingAnswer);
        feedback = findViewById(R.id.writingFeedback);
        check = findViewById(R.id.writingCheck);
        next = findViewById(R.id.writingNext);
        progressBar.setMax(questions.size());
        check.setOnClickListener(v -> grade());
        next.setOnClickListener(v -> { index++; showQuestion(); });
        input.setOnEditorActionListener((v, actionId, event) -> { if (!answered) { grade(); return true; } return false; });
        findViewById(R.id.writingBack).setOnClickListener(v -> confirmExit());
        if (state != null && state.getInt("answered_count", 0) > 0) {
            // Avoid silently losing an in-progress attempt if Android recreates the Activity.
            // The simplest safe policy is to restart the attempt after process recreation.
        }
        showQuestion();
    }
    private void showQuestion() {
        if (index >= questions.size()) { finishQuiz(); return; }
        answered = false;
        VocabItem word = questions.get(index);
        progress.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress(index + 1);
        direction.setText(meaningToTerm ? R.string.writing_direction_meaning : R.string.writing_direction_term);
        prompt.setText(meaningToTerm ? word.getMeaning() : word.getTerm());
        input.setText("");
        input.setEnabled(true);
        input.setInputType(meaningToTerm ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(true);
        input.setVisibility(View.VISIBLE);
        feedback.setText("");
        feedback.setVisibility(View.GONE);
        check.setVisibility(View.VISIBLE);
        next.setVisibility(View.GONE);
        startedAt = System.currentTimeMillis();
    }
    static boolean isCorrect(String typed, String expected, boolean english) {
        String left = typed.trim().replaceAll("\\s+", " ");
        String right = expected.trim().replaceAll("\\s+", " ");
        return english ? left.equalsIgnoreCase(right) : left.equals(right);
    }
    private void grade() {
        if (answered) return;
        VocabItem word = questions.get(index);
        String submitted = input.getText().toString().trim();
        if (submitted.isEmpty()) { input.setError(getString(R.string.writing_enter_answer)); return; }
        answered = true;
        String expected = meaningToTerm ? word.getTerm() : word.getMeaning();
        boolean correct = isCorrect(submitted, expected, meaningToTerm);
        answers.add(new QuizAnswerData(word.getId(), meaningToTerm ? "meaning-to-term" : "term-to-meaning",
                word.getTerm(), word.getMeaning(), meaningToTerm ? word.getMeaning() : word.getTerm(),
                submitted, expected, correct, false, System.currentTimeMillis() - startedAt));
        feedback.setText(correct ? getString(R.string.writing_correct) : getString(R.string.writing_incorrect, expected));
        feedback.setVisibility(View.VISIBLE);
        input.setEnabled(false);
        check.setVisibility(View.GONE);
        next.setVisibility(View.VISIBLE);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
    }
    private void finishQuiz() {
        long attemptId = DatabaseHelper.getInstance(this).saveQuizAttempt(bookId,
                meaningToTerm ? "writing-meaning-to-term" : "writing-term-to-meaning", 0, answers);
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("attempt_id", attemptId);
        intent.putExtra("writing_exam", true);
        startActivity(intent);
        finish();
    }
    private void confirmExit() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.ui_5cabff816073)
            .setMessage(R.string.ui_321a5d8a81b7)
            .setPositiveButton(R.string.ui_b37b7b42a11a, (d,w) -> finish())
            .setNegativeButton(R.string.ui_995224fb92dd, null).show();
    }
    @Override public void onBackPressed() { confirmExit(); }
}
