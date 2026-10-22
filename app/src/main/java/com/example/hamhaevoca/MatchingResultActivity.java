package com.example.hamhaevoca;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MatchingResultActivity extends LocalizedActivity {
    private long bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_result);

        bookId = getIntent().getLongExtra("book_id", -1);
        int totalCount = getIntent().getIntExtra("total_count", 0);
        int completedCount = getIntent().getIntExtra("completed_count", 0);
        int correctCount = getIntent().getIntExtra("correct_count", 0);
        int wrongCount = getIntent().getIntExtra("wrong_count", 0);
        int failedRounds = getIntent().getIntExtra("failed_rounds", 0);

        int accuracy = correctCount + wrongCount == 0 ? 100
                : Math.round(correctCount * 100f / (correctCount + wrongCount));
        TextView summary = findViewById(R.id.matchingResultSummary);
        summary.setText(Lang.text(MatchingResultActivity.this, R.string.ui_57ff445a7f6b) + totalCount
                + Lang.text(MatchingResultActivity.this, R.string.ui_a9b64b9b84e3) + completedCount
                + Lang.text(MatchingResultActivity.this, R.string.ui_fc96b0313d61) + wrongCount
                + Lang.text(MatchingResultActivity.this, R.string.ui_a781b3cc0b20) + failedRounds
                + Lang.text(MatchingResultActivity.this, R.string.ui_adcded323362) + accuracy + "%");

        findViewById(R.id.retryMatchingButton).setOnClickListener(v -> finish());
        findViewById(R.id.matchingBackToBookButton).setOnClickListener(v -> openBook());
    }

    private void openBook() {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book_id", bookId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        openBook();
    }
}
