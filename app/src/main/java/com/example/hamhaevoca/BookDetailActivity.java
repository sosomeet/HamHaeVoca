package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.QuizAttemptSummary;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookDetailActivity extends LocalizedActivity {
    private long bookId;
    private DatabaseHelper db;
    private TextView titleView;
    private TextView statsView;
    private TextView filterResultText;
    private EditText searchInput;
    private Button wrongFilterButton;
    private WordAdapter adapter;
    private final List<VocabItem> allWords = new ArrayList<VocabItem>();
    private final List<VocabItem> shownWords = new ArrayList<VocabItem>();
    private int minimumWrongCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        bookId = getIntent().getLongExtra("book_id", -1);
        db = DatabaseHelper.getInstance(this);

        ListView listView = findViewById(R.id.wordList);
        View header = getLayoutInflater().inflate(R.layout.header_book_detail, listView, false);
        listView.addHeaderView(header, null, false);

        titleView = header.findViewById(R.id.detailBookTitle);
        statsView = header.findViewById(R.id.detailBookStats);
        filterResultText = header.findViewById(R.id.filterResultText);
        searchInput = header.findViewById(R.id.searchInput);
        wrongFilterButton = header.findViewById(R.id.wrongFilterButton);

        adapter = new WordAdapter(this, shownWords);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object selected = parent.getItemAtPosition(position);
            if (selected instanceof VocabItem) showEditDialog((VocabItem) selected);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Object selected = parent.getItemAtPosition(position);
            if (selected instanceof VocabItem) {
                confirmDeleteWord((VocabItem) selected);
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterWords(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        header.findViewById(R.id.detailBackButton).setOnClickListener(v -> finish());
        header.findViewById(R.id.bookSettingsButton).setOnClickListener(v -> openBookSettings());
        header.findViewById(R.id.flashcardButton).setOnClickListener(v -> openLearning(FlashcardActivity.class));
        header.findViewById(R.id.quizButton).setOnClickListener(v -> openLearning(QuizSetupActivity.class));
        header.findViewById(R.id.matchingButton).setOnClickListener(v -> openLearning(MatchingSetupActivity.class));
        wrongFilterButton.setOnClickListener(v -> showWrongFilterDialog());
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
                    .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_cbab1c6fb7b1))
                    .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_07522fb549f9))
                    .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_468266d639b5), (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        titleView.setText(summary.getName());
        statsView.setText(Lang.text(BookDetailActivity.this, R.string.ui_57ff445a7f6b) + summary.getTotalCount() + Lang.text(BookDetailActivity.this, R.string.ui_4354af2c7ee7)
                + summary.getMemorizedCount() + Lang.text(BookDetailActivity.this, R.string.ui_d6688e7f48b7) + summary.getUnmemorizedCount());
        allWords.clear();
        allWords.addAll(db.getWords(bookId));
        filterWords();
    }

    private void filterWords() {
        if (searchInput == null || adapter == null) return;
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());
        shownWords.clear();
        for (VocabItem item : allWords) {
            boolean matchesText = query.isEmpty()
                    || item.getTerm().toLowerCase(Locale.getDefault()).contains(query)
                    || item.getMeaning().toLowerCase(Locale.getDefault()).contains(query);
            boolean matchesWrongCount = minimumWrongCount <= 0
                    || item.getIncorrectCount() >= minimumWrongCount;
            if (matchesText && matchesWrongCount) shownWords.add(item);
        }
        adapter.notifyDataSetChanged();
        updateFilterSummary();
    }

    private void updateFilterSummary() {
        if (minimumWrongCount <= 0) {
            wrongFilterButton.setText(Lang.text(BookDetailActivity.this, R.string.ui_bbe9bca07617));
        } else {
            wrongFilterButton.setText(Lang.text(BookDetailActivity.this, R.string.ui_d400e444f441) + minimumWrongCount + Lang.text(BookDetailActivity.this, R.string.ui_76107bdde6cc));
        }
        filterResultText.setText(Lang.text(BookDetailActivity.this, R.string.ui_916beb4b9acd) + shownWords.size() + Lang.text(BookDetailActivity.this, R.string.ui_8d9dbf3400c4) + allWords.size() + Lang.text(BookDetailActivity.this, R.string.ui_11600c9ada77));
    }

    private void showWrongFilterDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(Lang.text(BookDetailActivity.this, R.string.ui_4463d53188f6));
        input.setSingleLine(true);
        if (minimumWrongCount > 0) {
            input.setText(String.valueOf(minimumWrongCount));
            input.setSelection(input.length());
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_375a15fe250b))
                .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_bb01f1f06a97))
                .setView(input)
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_e01dd02f527f), null)
                .setNeutralButton(Lang.text(BookDetailActivity.this, R.string.ui_934dd25ec5f7), (d, w) -> {
                    minimumWrongCount = 0;
                    filterWords();
                })
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                minimumWrongCount = 0;
            } else {
                try {
                    minimumWrongCount = Math.max(0, Integer.parseInt(raw));
                } catch (NumberFormatException e) {
                    input.setError(Lang.text(BookDetailActivity.this, R.string.ui_381be7ceea62));
                    return;
                }
            }
            dialog.dismiss();
            filterWords();
        }));
        dialog.show();
    }

    private void openBookSettings() {
        Intent intent = new Intent(this, BookSettingsActivity.class);
        intent.putExtra("book_id", bookId);
        startActivity(intent);
    }

    private void showRecentAttemptsDialog() {
        List<QuizAttemptSummary> attempts = db.getRecentAttempts(bookId, 5);
        if (attempts.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_62e2814bc80d))
                    .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_c6e842e44e3a))
                    .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_468266d639b5), null)
                    .show();
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (QuizAttemptSummary attempt : attempts) {
            int rate = attempt.getTotalCount() == 0 ? 0
                    : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
            builder.append(attempt.getCompletedAt().replace('T', ' '))
                    .append(Lang.text(BookDetailActivity.this, R.string.ui_77d4612bd543)).append(attempt.getCorrectCount())
                    .append(" / ").append(attempt.getTotalCount())
                    .append(" · ").append(rate).append("%")
                    .append(Lang.text(BookDetailActivity.this, R.string.ui_1be068cb67b1)).append(attempt.getTimeLimit()).append(Lang.text(BookDetailActivity.this, R.string.ui_3845248e2861))
                    .append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_62e2814bc80d))
                .setMessage(builder.toString().trim())
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_468266d639b5), null)
                .show();
    }

    private void openLearning(Class<?> cls) {
        if (allWords.isEmpty()) {
            Toast.makeText(this, Lang.text(BookDetailActivity.this, R.string.ui_d23e4d78a601), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("book_id", bookId);
        startActivity(intent);
    }

    private void showMatchingLimitDialog() {
        if (allWords.size() < 2) {
            Toast.makeText(this, Lang.text(BookDetailActivity.this, R.string.ui_d975cbc5c7c4), Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint(Lang.text(BookDetailActivity.this, R.string.ui_69a8515dc2c9) + allWords.size() + Lang.text(BookDetailActivity.this, R.string.ui_11600c9ada77));
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_202eee223f82))
                .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_2f1bc3ae915e))
                .setView(input)
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_e89cc866baab), null)
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            int maxWords = 0;
            if (!raw.isEmpty()) {
                try {
                    maxWords = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    input.setError(Lang.text(BookDetailActivity.this, R.string.ui_8c5c02f2a819));
                    return;
                }
                if (maxWords < 2) {
                    input.setError(Lang.text(BookDetailActivity.this, R.string.ui_c6950f19c867));
                    return;
                }
                if (maxWords > allWords.size()) {
                    input.setError(Lang.text(BookDetailActivity.this, R.string.ui_486e5c811b12));
                    return;
                }
            }
            dialog.dismiss();
            Intent intent = new Intent(this, MatchingActivity.class);
            intent.putExtra("book_id", bookId);
            intent.putExtra("max_words", maxWords);
            startActivity(intent);
        }));
        dialog.show();
    }

    private void openBookUpdate() {
        Intent intent = new Intent(this, ImportActivity.class);
        intent.putExtra("update_book_id", bookId);
        startActivity(intent);
    }

    private void showEditDialog(final VocabItem existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_word, null);
        EditText termInput = view.findViewById(R.id.dialogTermInput);
        EditText meaningInput = view.findViewById(R.id.dialogMeaningInput);
        if (existing != null) {
            termInput.setText(existing.getTerm());
            meaningInput.setText(existing.getMeaning());
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? Lang.text(BookDetailActivity.this, R.string.ui_3d2957d26b5c) : Lang.text(BookDetailActivity.this, R.string.ui_114618fc79bd))
                .setView(view)
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_1f1712acff27), null)
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String term = termInput.getText().toString().trim();
            String meaning = meaningInput.getText().toString().trim();
            if (term.isEmpty()) {
                termInput.setError(Lang.text(BookDetailActivity.this, R.string.ui_65c711250651));
                return;
            }
            if (meaning.isEmpty()) {
                meaningInput.setError(Lang.text(BookDetailActivity.this, R.string.ui_7b977760f897));
                return;
            }
            long excluding = existing == null ? -1 : existing.getId();
            if (db.exactWordExists(bookId, term, meaning, excluding)) {
                termInput.setError(Lang.text(BookDetailActivity.this, R.string.ui_f5e4962b71b4));
                return;
            }
            if (existing == null) db.addWord(bookId, term, meaning);
            else db.updateWord(existing.getId(), bookId, term, meaning);
            dialog.dismiss();
            reload();
        }));
        dialog.show();
    }

    private void confirmDeleteWord(final VocabItem item) {
        new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_c4ad28822181))
                .setMessage(item.getTerm() + " — " + item.getMeaning() + Lang.text(BookDetailActivity.this, R.string.ui_0325254c09b4))
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_fc81e222b97c), (d, w) -> {
                    db.deleteWord(item.getId(), bookId);
                    reload();
                })
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_91d1e2b3617f))
                .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_44c99b6a55f0))
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_ff75b4ff2463), (d, w) -> {
                    db.resetMemorized(bookId);
                    Toast.makeText(this, Lang.text(BookDetailActivity.this, R.string.ui_4b994780f97d), Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .show();
    }

    private void renameBook() {
        BookSummary summary = db.getBookSummary(bookId);
        if (summary == null) return;
        final EditText input = new EditText(this);
        input.setText(summary.getName());
        input.setSelection(input.length());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_e5d232871e57))
                .setView(input)
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_1f1712acff27), null)
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(Lang.text(BookDetailActivity.this, R.string.ui_43d8a84eec67));
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
                .setTitle(Lang.text(BookDetailActivity.this, R.string.ui_33e270a7d3f6))
                .setMessage(Lang.text(BookDetailActivity.this, R.string.ui_01ff88cbb110))
                .setPositiveButton(Lang.text(BookDetailActivity.this, R.string.ui_fc81e222b97c), (d, w) -> {
                    db.deleteBook(bookId);
                    finish();
                })
                .setNegativeButton(Lang.text(BookDetailActivity.this, R.string.ui_19b2d19bc141), null)
                .show();
    }

    private static class WordAdapter extends ArrayAdapter<VocabItem> {
        private final LayoutInflater inflater;

        WordAdapter(Context context, List<VocabItem> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = inflater.inflate(R.layout.row_word, parent, false);
            VocabItem item = getItem(position);
            ((TextView) convertView.findViewById(R.id.wordTerm)).setText(item.getTerm());
            ((TextView) convertView.findViewById(R.id.wordMeaning)).setText(item.getMeaning());
            int totalAnswers = item.getCorrectCount() + item.getIncorrectCount();
            int wrongRate = totalAnswers == 0 ? 0
                    : Math.round(item.getIncorrectCount() * 100f / totalAnswers);
            TextView state = convertView.findViewById(R.id.wordState);
            state.setText((item.isMemorized() ? Lang.text(getContext(), R.string.ui_65d54abc78ca) : Lang.text(getContext(), R.string.ui_7e37bbef9b45))
                    + Lang.text(getContext(), R.string.ui_e5e13b993918) + totalAnswers + Lang.text(getContext(), R.string.ui_e006fd405e7a) + wrongRate + "%");
            TextView wrongCount = convertView.findViewById(R.id.wordWrongCount);
            wrongCount.setText(Lang.text(getContext(), R.string.ui_bbd4d442b4fd) + item.getIncorrectCount() + Lang.text(getContext(), R.string.ui_2fc05c02be34));
            return convertView;
        }
    }
}
