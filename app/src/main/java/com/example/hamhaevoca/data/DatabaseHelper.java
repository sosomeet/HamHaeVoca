package com.example.hamhaevoca.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.QuizAttemptSummary;
import com.example.hamhaevoca.model.VocabItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "hamhaevoca.db";
    private static final int DB_VERSION = 1;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "source_type TEXT NOT NULL," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL)");

        db.execSQL("CREATE TABLE words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "term TEXT NOT NULL," +
                "meaning TEXT NOT NULL," +
                "sort_order INTEGER NOT NULL," +
                "memorized INTEGER NOT NULL DEFAULT 0," +
                "correct_count INTEGER NOT NULL DEFAULT 0," +
                "incorrect_count INTEGER NOT NULL DEFAULT 0," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_words_book ON words(book_id)");
        db.execSQL("CREATE INDEX idx_words_book_order ON words(book_id, sort_order)");

        db.execSQL("CREATE TABLE quiz_attempts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "mode TEXT NOT NULL," +
                "time_limit INTEGER NOT NULL," +
                "total_count INTEGER NOT NULL," +
                "correct_count INTEGER NOT NULL," +
                "incorrect_count INTEGER NOT NULL," +
                "timed_out_count INTEGER NOT NULL," +
                "completed_at TEXT NOT NULL," +
                "FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE quiz_answers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "attempt_id INTEGER NOT NULL," +
                "item_id INTEGER," +
                "direction TEXT NOT NULL," +
                "term_snapshot TEXT NOT NULL," +
                "meaning_snapshot TEXT NOT NULL," +
                "prompt TEXT NOT NULL," +
                "selected_answer TEXT," +
                "correct_answer TEXT NOT NULL," +
                "is_correct INTEGER NOT NULL," +
                "timed_out INTEGER NOT NULL," +
                "response_time_ms INTEGER NOT NULL," +
                "FOREIGN KEY(attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_answers_attempt ON quiz_answers(attempt_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1: no migrations yet.
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    public long createBookWithWords(String name, String sourceType, List<VocabItem> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String timestamp = now();
            ContentValues bookValues = new ContentValues();
            bookValues.put("name", name);
            bookValues.put("source_type", sourceType);
            bookValues.put("created_at", timestamp);
            bookValues.put("updated_at", timestamp);
            long bookId = db.insertOrThrow("books", null, bookValues);

            int order = 0;
            for (VocabItem item : items) {
                ContentValues values = new ContentValues();
                values.put("book_id", bookId);
                values.put("term", item.getTerm());
                values.put("meaning", item.getMeaning());
                values.put("sort_order", order++);
                values.put("memorized", 0);
                values.put("correct_count", 0);
                values.put("incorrect_count", 0);
                values.put("created_at", timestamp);
                values.put("updated_at", timestamp);
                db.insertOrThrow("words", null, values);
            }
            db.setTransactionSuccessful();
            return bookId;
        } finally {
            db.endTransaction();
        }
    }

    public List<BookSummary> getBookSummaries() {
        List<BookSummary> result = new ArrayList<BookSummary>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT b.id, b.name, b.updated_at, COUNT(w.id) total_count, " +
                        "COALESCE(SUM(CASE WHEN w.memorized = 1 THEN 1 ELSE 0 END), 0) memorized_count " +
                        "FROM books b LEFT JOIN words w ON b.id = w.book_id " +
                        "GROUP BY b.id, b.name, b.updated_at ORDER BY b.updated_at DESC", null);
        try {
            while (cursor.moveToNext()) {
                result.add(new BookSummary(
                        cursor.getLong(0), cursor.getString(1), cursor.getInt(3),
                        cursor.getInt(4), cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public BookSummary getBookSummary(long bookId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT b.id, b.name, b.updated_at, COUNT(w.id), " +
                        "COALESCE(SUM(CASE WHEN w.memorized = 1 THEN 1 ELSE 0 END), 0) " +
                        "FROM books b LEFT JOIN words w ON b.id = w.book_id " +
                        "WHERE b.id = ? GROUP BY b.id, b.name, b.updated_at",
                new String[]{String.valueOf(bookId)});
        try {
            if (!cursor.moveToFirst()) return null;
            return new BookSummary(cursor.getLong(0), cursor.getString(1), cursor.getInt(3),
                    cursor.getInt(4), cursor.getString(2));
        } finally {
            cursor.close();
        }
    }

    public List<VocabItem> getWords(long bookId) {
        List<VocabItem> result = new ArrayList<VocabItem>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("words",
                new String[]{"id", "book_id", "term", "meaning", "sort_order", "memorized", "correct_count", "incorrect_count"},
                "book_id = ?", new String[]{String.valueOf(bookId)}, null, null, "sort_order ASC");
        try {
            while (cursor.moveToNext()) {
                result.add(readWord(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public List<VocabItem> getWordsByIds(long bookId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<VocabItem>();
        StringBuilder placeholders = new StringBuilder();
        String[] args = new String[ids.size() + 1];
        args[0] = String.valueOf(bookId);
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
            args[i + 1] = String.valueOf(ids.get(i));
        }
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, book_id, term, meaning, sort_order, memorized, correct_count, incorrect_count " +
                        "FROM words WHERE book_id = ? AND id IN (" + placeholders + ") ORDER BY sort_order", args);
        List<VocabItem> result = new ArrayList<VocabItem>();
        try {
            while (cursor.moveToNext()) result.add(readWord(cursor));
        } finally {
            cursor.close();
        }
        return result;
    }

    private VocabItem readWord(Cursor cursor) {
        return new VocabItem(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                cursor.getString(3), cursor.getInt(4), cursor.getInt(5) == 1,
                cursor.getInt(6), cursor.getInt(7));
    }

    public long addWord(long bookId, String term, String meaning) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM words WHERE book_id = ?",
                new String[]{String.valueOf(bookId)});
        int order = 0;
        try {
            if (cursor.moveToFirst()) order = cursor.getInt(0);
        } finally {
            cursor.close();
        }
        String timestamp = now();
        ContentValues values = new ContentValues();
        values.put("book_id", bookId);
        values.put("term", term);
        values.put("meaning", meaning);
        values.put("sort_order", order);
        values.put("memorized", 0);
        values.put("correct_count", 0);
        values.put("incorrect_count", 0);
        values.put("created_at", timestamp);
        values.put("updated_at", timestamp);
        long id = db.insertOrThrow("words", null, values);
        touchBook(db, bookId);
        return id;
    }

    public void updateWord(long wordId, long bookId, String term, String meaning) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("term", term);
        values.put("meaning", meaning);
        values.put("updated_at", now());
        db.update("words", values, "id = ? AND book_id = ?",
                new String[]{String.valueOf(wordId), String.valueOf(bookId)});
        touchBook(db, bookId);
    }

    public void deleteWord(long wordId, long bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("words", "id = ? AND book_id = ?",
                new String[]{String.valueOf(wordId), String.valueOf(bookId)});
        touchBook(db, bookId);
    }

    public boolean exactWordExists(long bookId, String term, String meaning, long excludingWordId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM words WHERE book_id = ? AND lower(trim(term)) = lower(trim(?)) " +
                        "AND trim(meaning) = trim(?) AND id != ? LIMIT 1",
                new String[]{String.valueOf(bookId), term, meaning, String.valueOf(excludingWordId)});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public void renameBook(long bookId, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("updated_at", now());
        getWritableDatabase().update("books", values, "id = ?", new String[]{String.valueOf(bookId)});
    }

    public void deleteBook(long bookId) {
        getWritableDatabase().delete("books", "id = ?", new String[]{String.valueOf(bookId)});
    }

    public void setMemorized(long wordId, long bookId, boolean memorized) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("memorized", memorized ? 1 : 0);
        values.put("updated_at", now());
        db.update("words", values, "id = ? AND book_id = ?",
                new String[]{String.valueOf(wordId), String.valueOf(bookId)});
        touchBook(db, bookId);
    }

    public void resetMemorized(long bookId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("memorized", 0);
        values.put("updated_at", now());
        db.update("words", values, "book_id = ?", new String[]{String.valueOf(bookId)});
        touchBook(db, bookId);
    }

    private void touchBook(SQLiteDatabase db, long bookId) {
        ContentValues values = new ContentValues();
        values.put("updated_at", now());
        db.update("books", values, "id = ?", new String[]{String.valueOf(bookId)});
    }

    public long saveQuizAttempt(long bookId, String mode, int timeLimit, List<QuizAnswerData> answers) {
        int correct = 0;
        int timedOut = 0;
        for (QuizAnswerData answer : answers) {
            if (answer.isCorrect()) correct++;
            if (answer.isTimedOut()) timedOut++;
        }
        int incorrect = answers.size() - correct;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues attempt = new ContentValues();
            attempt.put("book_id", bookId);
            attempt.put("mode", mode);
            attempt.put("time_limit", timeLimit);
            attempt.put("total_count", answers.size());
            attempt.put("correct_count", correct);
            attempt.put("incorrect_count", incorrect);
            attempt.put("timed_out_count", timedOut);
            attempt.put("completed_at", now());
            long attemptId = db.insertOrThrow("quiz_attempts", null, attempt);

            for (QuizAnswerData answer : answers) {
                ContentValues row = new ContentValues();
                row.put("attempt_id", attemptId);
                row.put("item_id", answer.getItemId());
                row.put("direction", answer.getDirection());
                row.put("term_snapshot", answer.getTerm());
                row.put("meaning_snapshot", answer.getMeaning());
                row.put("prompt", answer.getPrompt());
                if (answer.getSelectedAnswer() == null) row.putNull("selected_answer");
                else row.put("selected_answer", answer.getSelectedAnswer());
                row.put("correct_answer", answer.getCorrectAnswer());
                row.put("is_correct", answer.isCorrect() ? 1 : 0);
                row.put("timed_out", answer.isTimedOut() ? 1 : 0);
                row.put("response_time_ms", answer.getResponseTimeMs());
                db.insertOrThrow("quiz_answers", null, row);

                ContentValues counts = new ContentValues();
                String column = answer.isCorrect() ? "correct_count" : "incorrect_count";
                db.execSQL("UPDATE words SET " + column + " = " + column + " + 1, updated_at = ? WHERE id = ? AND book_id = ?",
                        new Object[]{now(), answer.getItemId(), bookId});
            }
            touchBook(db, bookId);
            db.setTransactionSuccessful();
            return attemptId;
        } finally {
            db.endTransaction();
        }
    }

    public List<QuizAnswerData> getQuizAnswers(long attemptId) {
        List<QuizAnswerData> result = new ArrayList<QuizAnswerData>();
        Cursor cursor = getReadableDatabase().query("quiz_answers",
                new String[]{"item_id", "direction", "term_snapshot", "meaning_snapshot", "prompt",
                        "selected_answer", "correct_answer", "is_correct", "timed_out", "response_time_ms"},
                "attempt_id = ?", new String[]{String.valueOf(attemptId)}, null, null, "id ASC");
        try {
            while (cursor.moveToNext()) {
                result.add(new QuizAnswerData(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.isNull(5) ? null : cursor.getString(5),
                        cursor.getString(6), cursor.getInt(7) == 1, cursor.getInt(8) == 1, cursor.getLong(9)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public QuizAttemptSummary getQuizAttempt(long attemptId) {
        Cursor cursor = getReadableDatabase().query("quiz_attempts",
                new String[]{"id", "mode", "time_limit", "total_count", "correct_count", "incorrect_count", "timed_out_count", "completed_at"},
                "id = ?", new String[]{String.valueOf(attemptId)}, null, null, null);
        try {
            if (!cursor.moveToFirst()) return null;
            return new QuizAttemptSummary(cursor.getLong(0), cursor.getString(1), cursor.getInt(2),
                    cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getString(7));
        } finally {
            cursor.close();
        }
    }

    public List<QuizAttemptSummary> getRecentAttempts(long bookId, int limit) {
        List<QuizAttemptSummary> result = new ArrayList<QuizAttemptSummary>();
        Cursor cursor = getReadableDatabase().query("quiz_attempts",
                new String[]{"id", "mode", "time_limit", "total_count", "correct_count", "incorrect_count", "timed_out_count", "completed_at"},
                "book_id = ?", new String[]{String.valueOf(bookId)}, null, null,
                "completed_at DESC", String.valueOf(limit));
        try {
            while (cursor.moveToNext()) {
                result.add(new QuizAttemptSummary(cursor.getLong(0), cursor.getString(1), cursor.getInt(2),
                        cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getString(7)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }
}
