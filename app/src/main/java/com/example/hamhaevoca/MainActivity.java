package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private DatabaseHelper db;
    private BookAdapter adapter;
    private final List<BookSummary> books = new ArrayList<BookSummary>();
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DatabaseHelper.getInstance(this);

        ListView listView = findViewById(R.id.bookList);
        emptyView = findViewById(R.id.emptyView);
        adapter = new BookAdapter(this, books);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openBook(books.get(position).getId()));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showBookMenu(books.get(position));
            return true;
        });

        Button importButton = findViewById(R.id.importButton);
        importButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ImportActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        books.clear();
        books.addAll(db.getBookSummaries());
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openBook(long bookId) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book_id", bookId);
        startActivity(intent);
    }

    private void showBookMenu(final BookSummary book) {
        final String[] actions = new String[]{"단어장 열기", "이름 변경", "삭제"};
        new AlertDialog.Builder(this)
                .setTitle(book.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openBook(book.getId());
                    else if (which == 1) renameBook(book);
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

        BookAdapter(Context context, List<BookSummary> data) {
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
