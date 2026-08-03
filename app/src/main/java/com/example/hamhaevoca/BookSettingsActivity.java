package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.QuizAttemptSummary;
import com.example.hamhaevoca.util.Ui;

import java.util.List;

public class BookSettingsActivity extends Activity {
    private long bookId;
    private DatabaseHelper db;
    private TextView titleView;
    private TextView statsView;
    private TextView recentEmptyView;
    private LinearLayout recentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_settings);

        bookId = getIntent().getLongExtra("book_id", -1);
        db = DatabaseHelper.getInstance(this);
        titleView = findViewById(R.id.settingsBookTitle);
        statsView = findViewById(R.id.settingsBookStats);
        recentEmptyView = findViewById(R.id.recentQuizEmptyText);
        recentContainer = findViewById(R.id.recentQuizContainer);

        findViewById(R.id.bookSettingsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.updateBookButton).setOnClickListener(v -> openBookUpdate());
        findViewById(R.id.addWordButton).setOnClickListener(v -> showAddWordDialog());
        findViewById(R.id.resetMemorizedButton).setOnClickListener(v -> confirmReset());
        findViewById(R.id.renameBookButton).setOnClickListener(v -> renameBook());
        findViewById(R.id.deleteBookButton).setOnClickListener(v -> confirmDeleteBook());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        BookSummary summary = db.getBookSummary(bookId);
        if (summary == null) {
            new AlertDialog.Builder(this)
                    .setTitle("단어장을 찾을 수 없음")
                    .setMessage("삭제되었거나 존재하지 않는 단어장입니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        titleView.setText(summary.getName());
        statsView.setText("전체 " + summary.getTotalCount() + " · 외움 "
                + summary.getMemorizedCount() + " · 미암기 " + summary.getUnmemorizedCount());
        renderRecentAttempts();
    }

    private void renderRecentAttempts() {
        recentContainer.removeAllViews();
        List<QuizAttemptSummary> attempts = db.getRecentAttempts(bookId, 5);
        recentEmptyView.setVisibility(attempts.isEmpty() ? View.VISIBLE : View.GONE);
        for (QuizAttemptSummary attempt : attempts) {
            int rate = attempt.getTotalCount() == 0 ? 0
                    : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
            TextView card = new TextView(this);
            card.setText(attempt.getCompletedAt().replace('T', ' ')
                    + "\n정답 " + attempt.getCorrectCount() + " / " + attempt.getTotalCount()
                    + " · " + rate + "% · 제한 " + attempt.getTimeLimit() + "초");
            card.setTextColor(getResources().getColor(R.color.text_primary));
            card.setTextSize(15f);
            card.setLineSpacing(0f, 1.2f);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14),
                    Ui.dp(this, 16), Ui.dp(this, 14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, Ui.dp(this, 10));
            card.setLayoutParams(params);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, QuizResultActivity.class);
                intent.putExtra("book_id", bookId);
                intent.putExtra("attempt_id", attempt.getId());
                startActivity(intent);
            });
            recentContainer.addView(card);
        }
    }

    private void openBookUpdate() {
        Intent intent = new Intent(this, ImportActivity.class);
        intent.putExtra("update_book_id", bookId);
        startActivity(intent);
    }

    private void showAddWordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_word, null);
        EditText termInput = view.findViewById(R.id.dialogTermInput);
        EditText meaningInput = view.findViewById(R.id.dialogMeaningInput);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("단어 추가")
                .setView(view)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String term = termInput.getText().toString().trim();
                    String meaning = meaningInput.getText().toString().trim();
                    if (term.isEmpty()) {
                        termInput.setError("영단어를 입력하세요.");
                        return;
                    }
                    if (meaning.isEmpty()) {
                        meaningInput.setError("뜻을 입력하세요.");
                        return;
                    }
                    if (db.exactWordExists(bookId, term, meaning, -1)) {
                        termInput.setError("동일한 단어와 뜻이 이미 있습니다.");
                        return;
                    }
                    db.addWord(bookId, term, meaning);
                    dialog.dismiss();
                    Toast.makeText(this, "단어를 추가했습니다.", Toast.LENGTH_SHORT).show();
                    reload();
                }));
        dialog.show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("암기 상태 초기화")
                .setMessage("외움 표시를 모두 초기화할까요? 시험 정답·오답 횟수는 유지됩니다.")
                .setPositiveButton("초기화", (d, w) -> {
                    db.resetMemorized(bookId);
                    Toast.makeText(this, "초기화했습니다.", Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void renameBook() {
        BookSummary summary = db.getBookSummary(bookId);
        if (summary == null) return;
        final EditText input = new EditText(this);
        input.setText(summary.getName());
        input.setSelection(input.length());
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("단어장 이름 변경")
                .setView(input)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("이름을 입력하세요.");
                        return;
                    }
                    db.renameBook(bookId, name);
                    dialog.dismiss();
                    reload();
                }));
        dialog.show();
    }

    private void confirmDeleteBook() {
        new AlertDialog.Builder(this)
                .setTitle("단어장 삭제")
                .setMessage("단어와 시험 기록을 모두 삭제합니다. 계속할까요?")
                .setPositiveButton("삭제", (d, w) -> {
                    db.deleteBook(bookId);
                    Toast.makeText(this, "단어장을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
