package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.view.inputmethod.InputMethodManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private DatabaseHelper db;
    private BookAdapter adapter;
    private final List<BookSummary> books = new ArrayList<BookSummary>();
    private TextView emptyView;
    private TextView bookSectionTitle;
    private EditText searchInput;
    private View searchContainer;
    private final List<BookSummary> allBooks = new ArrayList<>();
    private String searchQuery = "";
    private Long activeFolderId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DatabaseHelper.getInstance(this);

        ListView listView = findViewById(R.id.bookList);
        emptyView = findViewById(R.id.emptyView);
        adapter = new BookAdapter(this, books, db);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openBook(books.get(position).getId()));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showBookMenu(books.get(position));
            return true;
        });

        searchInput = findViewById(R.id.bookSearchInput);
        searchContainer = findViewById(R.id.bookSearchContainer);
        ImageButton searchToggle = findViewById(R.id.bookSearchButton);
        searchToggle.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.VISIBLE) {
                searchInput.setText("");
                searchInput.clearFocus();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                searchContainer.setVisibility(View.GONE);
            } else {
                searchContainer.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
                searchInput.post(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT));
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        bookSectionTitle = findViewById(R.id.bookSectionTitle);
        findViewById(R.id.folderButton).setOnClickListener(v -> selectFolderFilter());
        findViewById(R.id.importButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ImportActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        allBooks.clear();
        allBooks.addAll(db.getBookSummaries());
        updateFolderTitle();
        applyFilters();
    }

    private void applyFilters() {
        books.clear();
        for (BookSummary book : allBooks) {
            if (!book.getName().toLowerCase(Locale.ROOT).contains(searchQuery)) continue;
            Long folderId = db.getBookFolderId(book.getId());
            if (activeFolderId != null) {
                if (activeFolderId == -1L && folderId != null) continue;
                if (activeFolderId != -1L && !activeFolderId.equals(folderId)) continue;
            }
            books.add(book);
        }
        adapter.notifyDataSetChanged();
        emptyView.setText(allBooks.isEmpty() ? "아직 저장된 단어장이 없어요." : "검색 조건에 맞는 단어장이 없습니다.");
        emptyView.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateFolderTitle() {
        if (activeFolderId == null) {
            bookSectionTitle.setText("내 단어장");
        } else if (activeFolderId == -1L) {
            bookSectionTitle.setText("미분류 단어장");
        } else {
            String folderName = null;
            for (DatabaseHelper.Folder folder : db.getFolders()) {
                if (folder.id == activeFolderId) {
                    folderName = folder.name;
                    break;
                }
            }
            if (folderName == null) {
                activeFolderId = null;
                bookSectionTitle.setText("내 단어장");
            } else {
                bookSectionTitle.setText(folderName);
            }
        }
    }

    private void selectFolderFilter() {
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] labels = new String[folders.size() + 3];
        labels[0] = "전체 단어장";
        labels[1] = "미분류";
        for (int i = 0; i < folders.size(); i++) labels[i + 2] = folders.get(i).name;
        labels[labels.length - 1] = "폴더 관리";
        new AlertDialog.Builder(this).setTitle("폴더")
                .setItems(labels, (d, index) -> {
                    if (index == labels.length - 1) {
                        manageFolders();
                        return;
                    }
                    activeFolderId = index == 0 ? null : index == 1 ? -1L : folders.get(index - 2).id;
                    updateFolderTitle();
                    applyFilters();
                }).show();
    }

    private void manageFolders() {
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] items = new String[folders.size() + 1];
        items[0] = "+ 새 폴더";
        for (int i = 0; i < folders.size(); i++) items[i + 1] = folders.get(i).name;
        new AlertDialog.Builder(this).setTitle("폴더 관리").setItems(items, (d, index) -> {
            if (index == 0) { editFolder(null); return; }
            DatabaseHelper.Folder folder = folders.get(index - 1);
            new AlertDialog.Builder(this).setTitle(folder.name)
                    .setItems(new String[]{"이름 변경", "삭제"}, (dialog, action) -> {
                        if (action == 0) editFolder(folder);
                        else confirmDeleteFolder(folder);
                    }).show();
        }).show();
    }

    private void editFolder(DatabaseHelper.Folder folder) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        if (folder != null) { input.setText(folder.name); input.setSelection(input.length()); }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(folder == null ? "새 폴더" : "폴더 이름 변경")
                .setView(input).setPositiveButton("저장", null)
                .setNegativeButton("취소", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) { input.setError("폴더 이름을 입력하세요."); return; }
                    try {
                        if (folder == null) db.createFolder(name);
                        else db.renameFolder(folder.id, name);
                        dialog.dismiss();
                        reload();
                        updateFolderTitle();
                    } catch (SQLiteConstraintException e) { input.setError("이미 존재하는 폴더입니다."); }
                }));
        dialog.show();
    }

    private void confirmDeleteFolder(DatabaseHelper.Folder folder) {
        new AlertDialog.Builder(this).setTitle("폴더 삭제")
                .setMessage("폴더를 삭제해도 단어장과 학습 기록은 유지됩니다. 단어장은 미분류로 이동합니다.")
                .setPositiveButton("삭제", (d, w) -> {
                    db.deleteFolder(folder.id);
                    if (activeFolderId != null && activeFolderId == folder.id) {
                        activeFolderId = null;
                        updateFolderTitle();
                    }
                    reload();
                }).setNegativeButton("취소", null).show();
    }

    private void changeBookFolder(BookSummary book) {
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] items = new String[folders.size() + 1]; items[0] = "미분류";
        for (int i = 0; i < folders.size(); i++) items[i + 1] = folders.get(i).name;
        new AlertDialog.Builder(this).setTitle(book.getName() + " · 폴더 변경")
                .setItems(items, (d, index) -> {
                    db.setBookFolder(book.getId(), index == 0 ? null : folders.get(index - 1).id);
                    reload();
                }).show();
    }

    private void openBook(long bookId) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book_id", bookId);
        startActivity(intent);
    }

    private void showBookMenu(final BookSummary book) {
        final String[] actions = new String[]{"단어장 열기", "이름 변경", "폴더 변경", "삭제"};
        new AlertDialog.Builder(this)
                .setTitle(book.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openBook(book.getId());
                    else if (which == 1) renameBook(book);
                    else if (which == 2) changeBookFolder(book);
                    else confirmDelete(book);
                })
                .show();
    }

    private void renameBook(final BookSummary book) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(book.getName());
        input.setSelection(input.length());
        int p = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(p, p / 2, p, p / 2);
        final AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle("단어장 이름 변경")
                .setView(input)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        alert.setOnShowListener(dialog ->
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("이름을 입력하세요.");
                        return;
                    }
                    db.renameBook(book.getId(), name);
                    alert.dismiss();
                    reload();
                }));
        alert.show();
    }

    private void confirmDelete(final BookSummary book) {
        new AlertDialog.Builder(this)
                .setTitle("단어장 삭제")
                .setMessage("'" + book.getName() + "' 단어장과 학습 기록을 모두 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.deleteBook(book.getId());
                    Toast.makeText(MainActivity.this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private static class BookAdapter extends ArrayAdapter<BookSummary> {
        private final LayoutInflater inflater;

        BookAdapter(Context context, List<BookSummary> data, DatabaseHelper db) {
            super(context, 0, data);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_book, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.bookName);
                holder.stats = convertView.findViewById(R.id.bookStats);
                holder.date = convertView.findViewById(R.id.bookDate);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            BookSummary item = getItem(position);
            holder.name.setText(item.getName());
            holder.stats.setText("전체 " + item.getTotalCount() + " · 외움 " + item.getMemorizedCount() + " · 미암기 " + item.getUnmemorizedCount());
            holder.date.setText("수정: " + item.getUpdatedAt().replace('T', ' '));
            return convertView;
        }

        private static class ViewHolder {
            TextView name;
            TextView stats;
            TextView date;
        }
    }
}
