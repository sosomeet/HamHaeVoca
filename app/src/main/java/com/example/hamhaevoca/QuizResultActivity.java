package com.example.hamhaevoca;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.QuizAttemptSummary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuizResultActivity extends Activity {
    private long bookId;
    private long attemptId;
    private List<QuizAnswerData> answers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        bookId = getIntent().getLongExtra("book_id", -1);
        attemptId = getIntent().getLongExtra("attempt_id", -1);
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        QuizAttemptSummary attempt = db.getQuizAttempt(attemptId);
        answers = db.getQuizAnswers(attemptId);
        if (attempt == null) {
            Toast.makeText(this, "시험 결과를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        int rate = attempt.getTotalCount() == 0 ? 0 : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
        TextView summary = findViewById(R.id.resultSummaryText);
        summary.setText("전체 " + attempt.getTotalCount() + "\n정답 " + attempt.getCorrectCount()
                + " · 오답 " + attempt.getIncorrectCount() + " · 시간 초과 " + attempt.getTimedOutCount()
                + "\n정답률 " + rate + "%");

        List<String> wrongRows = new ArrayList<String>();
        for (QuizAnswerData answer : answers) {
            if (!answer.isCorrect()) {
                String selected = answer.getSelectedAnswer() == null ? "선택 없음" : answer.getSelectedAnswer();
                wrongRows.add(answer.getTerm() + " — " + answer.getMeaning() + "\n문제: " + answer.getPrompt()
                        + "\n내 답: " + selected + " / 정답: " + answer.getCorrectAnswer());
            }
        }
        ListView listView = findViewById(R.id.wrongAnswerList);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, wrongRows));
        TextView empty = findViewById(R.id.noWrongText);
        empty.setVisibility(wrongRows.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        Button review = findViewById(R.id.reviewWrongButton);
        review.setEnabled(!wrongRows.isEmpty());
        review.setOnClickListener(v -> openWrongReview());
        findViewById(R.id.retryQuizButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizSetupActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.backToBookButton).setOnClickListener(v -> finish());
    }

    private void openWrongReview() {
        Set<Long> ids = new LinkedHashSet<Long>();
        for (QuizAnswerData answer : answers) if (!answer.isCorrect()) ids.add(answer.getItemId());
        if (ids.isEmpty()) return;
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (builder.length() > 0) builder.append(',');
            builder.append(id);
        }
        Intent intent = new Intent(this, FlashcardActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("wrong_ids", builder.toString());
        startActivity(intent);
    }
}
