package com.example.hamhaevoca;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

public class QuizResultActivity extends LocalizedActivity {
    private long bookId;
    private long attemptId;
    private List<QuizAnswerData> answers;
    private final List<QuizAnswerData> wrongAnswers = new ArrayList<QuizAnswerData>();

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
            Toast.makeText(this, Lang.text(QuizResultActivity.this, R.string.ui_471d6bd6f90e), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        int rate = attempt.getTotalCount() == 0 ? 0
                : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
        TextView summary = findViewById(R.id.resultSummaryText);
        summary.setText(Lang.text(QuizResultActivity.this, R.string.ui_57ff445a7f6b) + attempt.getTotalCount() + Lang.text(QuizResultActivity.this, R.string.ui_77d4612bd543) + attempt.getCorrectCount()
                + Lang.text(QuizResultActivity.this, R.string.ui_a05539858e31) + attempt.getIncorrectCount() + Lang.text(QuizResultActivity.this, R.string.ui_6b0fa13084d8)
                + attempt.getTimedOutCount() + Lang.text(QuizResultActivity.this, R.string.ui_31caa7fb9e05) + rate + "%");

        wrongAnswers.clear();
        for (QuizAnswerData answer : answers) {
            if (!answer.isCorrect()) wrongAnswers.add(answer);
        }

        ListView listView = findViewById(R.id.wrongAnswerList);
        listView.setAdapter(new WrongAnswerAdapter());
        TextView empty = findViewById(R.id.noWrongText);
        empty.setVisibility(wrongAnswers.isEmpty() ? View.VISIBLE : View.GONE);

        Button review = findViewById(R.id.reviewWrongButton);
        review.setEnabled(!wrongAnswers.isEmpty());
        review.setAlpha(wrongAnswers.isEmpty() ? 0.45f : 1f);
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
        for (QuizAnswerData answer : answers) {
            if (!answer.isCorrect()) ids.add(answer.getItemId());
        }
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

    private final class WrongAnswerAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return wrongAnswers.size();
        }

        @Override
        public QuizAnswerData getItem(int position) {
            return wrongAnswers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getItemId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(QuizResultActivity.this)
                        .inflate(R.layout.row_quiz_wrong, parent, false);
                holder = new ViewHolder();
                holder.term = convertView.findViewById(R.id.wrongTerm);
                holder.meaning = convertView.findViewById(R.id.wrongMeaning);
                holder.question = convertView.findViewById(R.id.wrongQuestion);
                holder.selected = convertView.findViewById(R.id.wrongSelectedAnswer);
                holder.correct = convertView.findViewById(R.id.wrongCorrectAnswer);
                holder.badge = convertView.findViewById(R.id.wrongResultBadge);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            QuizAnswerData answer = getItem(position);
            holder.term.setText(answer.getTerm());
            holder.meaning.setText(answer.getMeaning());
            holder.question.setText(Lang.text(QuizResultActivity.this, R.string.ui_6ba889e1849a) + answer.getPrompt());
            String selectedAnswer = answer.getSelectedAnswer() == null
                    ? Lang.text(QuizResultActivity.this, R.string.ui_6f5e091d530c) : answer.getSelectedAnswer();
            holder.selected.setText(Lang.text(QuizResultActivity.this, R.string.ui_abd56a192ec7) + selectedAnswer);
            holder.correct.setText(Lang.text(QuizResultActivity.this, R.string.ui_4ca35d924fdf) + answer.getCorrectAnswer());
            holder.badge.setText(answer.isTimedOut() ? Lang.text(QuizResultActivity.this, R.string.ui_0ad9a1ce7a33) : Lang.text(QuizResultActivity.this, R.string.ui_152478d5e15e));
            return convertView;
        }
    }

    private static final class ViewHolder {
        private TextView term;
        private TextView meaning;
        private TextView question;
        private TextView selected;
        private TextView correct;
        private TextView badge;
    }
}
