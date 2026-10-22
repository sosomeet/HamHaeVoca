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

public class BookSettingsActivity extends LocalizedActivity {
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
                    .setTitle(Lang.text(BookSettingsActivity.this, R.string.ui_cbab1c6fb7b1))
                    .setMessage(Lang.text(BookSettingsActivity.this, R.string.ui_07522fb549f9))
                    .setPositiveButton(Lang.text(BookSettingsActivity.this, R.string.ui_468266d639b5), (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        titleView.setText(summary.getName());
        statsView.setText(Lang.text(BookSettingsActivity.this, R.string.ui_57ff445a7f6b) + summary.getTotalCount() + Lang.text(BookSettingsActivity.this, R.string.ui_4354af2c7ee7)
                + summary.getMemorizedCount() + Lang.text(BookSettingsActivity.this, R.string.ui_d6688e7f48b7) + summary.getUnmemorizedCount());
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
                    + Lang.text(BookSettingsActivity.this, R.string.ui_77d4612bd543) + attempt.getCorrectCount() + " / " + attempt.getTotalCount()
                    + " · " + rate + Lang.text(BookSettingsActivity.this, R.string.ui_d6fd819f4961) + attempt.getTimeLimit() + Lang.text(BookSettingsActivity.this, R.string.ui_3845248e2861));
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
                .setTitle(Lang.text(BookSettingsActivity.this, R.string.ui_3d2957d26b5c))
                .setView(view)
                .setPositiveButton(Lang.text(BookSettingsActivity.this, R.string.ui_1f1712acff27), null)
                .setNegativeButton(Lang.text(BookSettingsActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String term = termInput.getText().toString().trim();
                    String meaning = meaningInput.getText().toString().trim();
                    if (term.isEmpty()) {
                        termInput.setError(Lang.text(BookSettingsActivity.this, R.string.ui_65c711250651));
                        return;
                    }
                    if (meaning.isEmpty()) {
                        meaningInput.setError(Lang.text(BookSettingsActivity.this, R.string.ui_7b977760f897));
                        return;
                    }
                    if (db.exactWordExists(bookId, term, meaning, -1)) {
                        termInput.setError(Lang.text(BookSettingsActivity.this, R.string.ui_f5e4962b71b4));
                        return;
                    }
                    db.addWord(bookId, term, meaning);
                    dialog.dismiss();
                    Toast.makeText(this, Lang.text(BookSettingsActivity.this, R.string.ui_cc6d3bff5ff5), Toast.LENGTH_SHORT).show();
                    reload();
                }));
        dialog.show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookSettingsActivity.this, R.string.ui_91d1e2b3617f))
                .setMessage(Lang.text(BookSettingsActivity.this, R.string.ui_44c99b6a55f0))
                .setPositiveButton(Lang.text(BookSettingsActivity.this, R.string.ui_ff75b4ff2463), (d, w) -> {
                    db.resetMemorized(bookId);
                    Toast.makeText(this, Lang.text(BookSettingsActivity.this, R.string.ui_4b994780f97d), Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton(Lang.text(BookSettingsActivity.this, R.string.ui_19b2d19bc141), null)
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
                .setTitle(Lang.text(BookSettingsActivity.this, R.string.ui_e5d232871e57))
                .setView(input)
                .setPositiveButton(Lang.text(BookSettingsActivity.this, R.string.ui_1f1712acff27), null)
                .setNegativeButton(Lang.text(BookSettingsActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError(Lang.text(BookSettingsActivity.this, R.string.ui_43d8a84eec67));
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
                .setTitle(Lang.text(BookSettingsActivity.this, R.string.ui_33e270a7d3f6))
                .setMessage(Lang.text(BookSettingsActivity.this, R.string.ui_01ff88cbb110))
                .setPositiveButton(Lang.text(BookSettingsActivity.this, R.string.ui_fc81e222b97c), (d, w) -> {
                    db.deleteBook(bookId);
                    Toast.makeText(this, Lang.text(BookSettingsActivity.this, R.string.ui_24dbfbfa1dc9), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(Lang.text(BookSettingsActivity.this, R.string.ui_19b2d19bc141), null)
                .show();
    }
}
