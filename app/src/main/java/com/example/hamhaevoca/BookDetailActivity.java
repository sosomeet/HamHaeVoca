package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.QuizAttemptSummary;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookDetailActivity extends Activity {
    private long bookId;
    private DatabaseHelper db;
    private TextView titleView;
    private TextView statsView;
    private TextView recentView;
    private EditText searchInput;
    private WordAdapter adapter;
    private final List<VocabItem> allWords = new ArrayList<VocabItem>();
    private final List<VocabItem> shownWords = new ArrayList<VocabItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        bookId = getIntent().getLongExtra("book_id", -1);
        db = DatabaseHelper.getInstance(this);

        titleView = findViewById(R.id.detailBookTitle);
        statsView = findViewById(R.id.detailBookStats);
        recentView = findViewById(R.id.recentQuizText);
        searchInput = findViewById(R.id.searchInput);
        ListView listView = findViewById(R.id.wordList);
        adapter = new WordAdapter(this, shownWords);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> showEditDialog(shownWords.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDeleteWord(shownWords.get(position));
            return true;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterWords(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.addWordButton).setOnClickListener(v -> showEditDialog(null));
        findViewById(R.id.flashcardButton).setOnClickListener(v -> openLearning(FlashcardActivity.class));
        findViewById(R.id.quizButton).setOnClickListener(v -> openLearning(QuizSetupActivity.class));
        findViewById(R.id.matchingButton).setOnClickListener(v -> openLearning(MatchingActivity.class));
        findViewById(R.id.resetMemorizedButton).setOnClickListener(v -> confirmReset());
        findViewById(R.id.deleteBookButton).setOnClickListener(v -> confirmDeleteBook());
        findViewById(R.id.renameBookButton).setOnClickListener(v -> renameBook());
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
        statsView.setText("전체 " + summary.getTotalCount() + " · 외움 " + summary.getMemorizedCount() + " · 미암기 " + summary.getUnmemorizedCount());
        allWords.clear();
        allWords.addAll(db.getWords(bookId));
        filterWords();
        showRecentAttempts();
    }

    private void showRecentAttempts() {
        List<QuizAttemptSummary> attempts = db.getRecentAttempts(bookId, 5);
        if (attempts.isEmpty()) {
            recentView.setText("최근 시험 기록 없음");
            return;
        }
        StringBuilder builder = new StringBuilder("최근 시험\n");
        for (QuizAttemptSummary attempt : attempts) {
            int rate = attempt.getTotalCount() == 0 ? 0 : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
            builder.append(attempt.getCompletedAt().replace('T', ' '))
                    .append(" · ").append(attempt.getCorrectCount()).append('/').append(attempt.getTotalCount())
                    .append(" (").append(rate).append("%)\n");
        }
        recentView.setText(builder.toString().trim());
    }

    private void filterWords() {
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());
        shownWords.clear();
        for (VocabItem item : allWords) {
            if (query.isEmpty() || item.getTerm().toLowerCase(Locale.getDefault()).contains(query)
                    || item.getMeaning().toLowerCase(Locale.getDefault()).contains(query)) {
                shownWords.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openLearning(Class<?> cls) {
        if (allWords.isEmpty()) {
            Toast.makeText(this, "단어를 먼저 추가하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("book_id", bookId);
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
                .setTitle(existing == null ? "단어 추가" : "단어 수정")
                .setView(view)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
            long excluding = existing == null ? -1 : existing.getId();
            if (db.exactWordExists(bookId, term, meaning, excluding)) {
                termInput.setError("동일한 단어와 뜻이 이미 있습니다.");
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
                .setTitle("단어 삭제")
                .setMessage(item.getTerm() + " — " + item.getMeaning() + "\n\n삭제할까요?")
                .setPositiveButton("삭제", (d, w) -> {
                    db.deleteWord(item.getId(), bookId);
                    reload();
                })
                .setNegativeButton("취소", null)
                .show();
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("단어장 이름 변경")
                .setView(input)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
                    finish();
                })
                .setNegativeButton("취소", null)
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
            TextView state = convertView.findViewById(R.id.wordState);
            state.setText((item.isMemorized() ? "외움" : "미암기") + " · 정답 " + item.getCorrectCount() + " · 오답 " + item.getIncorrectCount());
            return convertView;
        }
    }
}
