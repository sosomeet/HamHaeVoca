package com.example.hamhaevoca;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;
import java.util.List;

public class WritingSetupActivity extends LocalizedActivity {
    private long bookId;
    private List<VocabItem> words;
    private EditText minWrong, maxQuestions;
    private TextView available;
    private Button start;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_writing_setup);
        bookId = getIntent().getLongExtra("book_id", -1);
        words = DatabaseHelper.getInstance(this).getWords(bookId);
        minWrong = findViewById(R.id.writingMinWrong);
        maxQuestions = findViewById(R.id.writingMaxQuestions);
        available = findViewById(R.id.writingAvailable);
        start = findViewById(R.id.writingStart);
        findViewById(R.id.writingSetupBack).setOnClickListener(v -> finish());
        minWrong.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updateCount(); }
            public void afterTextChanged(Editable e) {}
        });
        start.setOnClickListener(v -> begin());
        updateCount();
    }

    private int number(EditText field, int fallback) {
        String s = field.getText().toString().trim();
        if (s.isEmpty()) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }
    private int countEligible(int minimum) {
        int count = 0;
        for (VocabItem word : words) if (word.getIncorrectCount() >= minimum
                && !word.getTerm().trim().isEmpty() && !word.getMeaning().trim().isEmpty()) count++;
        return count;
    }
    private void updateCount() {
        int minimum = number(minWrong, 0);
        int count = minimum < 0 ? 0 : countEligible(minimum);
        available.setText(getString(R.string.writing_available, count));
        start.setEnabled(count > 0);
        start.setAlpha(count > 0 ? 1f : .5f);
    }
    private void begin() {
        int minimum = number(minWrong, 0);
        if (minimum < 0) { minWrong.setError(getString(R.string.writing_min_wrong)); return; }
        int count = countEligible(minimum);
        if (count == 0) { Toast.makeText(this, R.string.writing_no_words, Toast.LENGTH_SHORT).show(); return; }
        int maximum = number(maxQuestions, count);
        if (maximum < 1 || maximum > count) { maxQuestions.setError(getString(R.string.writing_available, count)); return; }
        Intent intent = new Intent(this, WritingQuizActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("min_incorrect", minimum);
        intent.putExtra("max_words", maximum);
        intent.putExtra("meaning_to_term", ((RadioGroup)findViewById(R.id.writingModeGroup)).getCheckedRadioButtonId() != R.id.writingModeTerm);
        startActivity(intent);
    }
}
