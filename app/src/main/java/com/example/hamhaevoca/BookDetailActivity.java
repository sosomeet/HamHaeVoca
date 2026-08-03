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

public class BookDetailActivity extends Activity {
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
            wrongFilterButton.setText("오답 필터: 전체");
        } else {
            wrongFilterButton.setText("오답 필터: " + minimumWrongCount + "회 이상");
        }
        filterResultText.setText("표시 중 " + shownWords.size() + "개 / 전체 " + allWords.size() + "개");
    }

    private void showWrongFilterDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("0은 전체 표시");
        input.setSingleLine(true);
        if (minimumWrongCount > 0) {
            input.setText(String.valueOf(minimumWrongCount));
            input.setSelection(input.length());
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("오답 횟수 필터")
                .setMessage("입력한 횟수 이상 틀린 단어만 표시합니다.")
                .setView(input)
                .setPositiveButton("적용", null)
                .setNeutralButton("전체", (d, w) -> {
                    minimumWrongCount = 0;
                    filterWords();
                })
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                minimumWrongCount = 0;
            } else {
                try {
                    minimumWrongCount = Math.max(0, Integer.parseInt(raw));
                } catch (NumberFormatException e) {
                    input.setError("0 이상의 숫자를 입력하세요.");
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
                    .setTitle("최근 시험 기록")
                    .setMessage("아직 완료한 시험이 없습니다.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (QuizAttemptSummary attempt : attempts) {
            int rate = attempt.getTotalCount() == 0 ? 0
                    : Math.round(attempt.getCorrectCount() * 100f / attempt.getTotalCount());
            builder.append(attempt.getCompletedAt().replace('T', ' '))
                    .append("\n정답 ").append(attempt.getCorrectCount())
                    .append(" / ").append(attempt.getTotalCount())
                    .append(" · ").append(rate).append("%")
                    .append(" · 제한 ").append(attempt.getTimeLimit()).append("초")
                    .append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("최근 시험 기록")
                .setMessage(builder.toString().trim())
                .setPositiveButton("확인", null)
                .show();
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

    private void showMatchingLimitDialog() {
        if (allWords.size() < 2) {
            Toast.makeText(this, "맞추기에는 최소 2개의 단어가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint("비우면 전체 · 최대 " + allWords.size() + "개");
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("맞추기 단어 수")
                .setMessage("이번 맞추기에 사용할 최대 단어 개수를 입력하세요.")
                .setView(input)
                .setPositiveButton("시작", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            int maxWords = 0;
            if (!raw.isEmpty()) {
                try {
                    maxWords = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    input.setError("숫자를 입력하세요.");
                    return;
                }
                if (maxWords < 2) {
                    input.setError("최소 2개 이상 입력하세요.");
                    return;
                }
                if (maxWords > allWords.size()) {
                    input.setError("전체 단어 수 이하로 입력하세요.");
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
            int totalAnswers = item.getCorrectCount() + item.getIncorrectCount();
            int wrongRate = totalAnswers == 0 ? 0
                    : Math.round(item.getIncorrectCount() * 100f / totalAnswers);
            TextView state = convertView.findViewById(R.id.wordState);
            state.setText((item.isMemorized() ? "외움" : "미암기")
                    + " · 시험 " + totalAnswers + "회 · 오답률 " + wrongRate + "%");
            TextView wrongCount = convertView.findViewById(R.id.wordWrongCount);
            wrongCount.setText("오답 " + item.getIncorrectCount() + "회");
            return convertView;
        }
    }
}
