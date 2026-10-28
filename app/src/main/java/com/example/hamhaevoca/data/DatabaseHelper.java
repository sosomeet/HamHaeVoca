package com.example.hamhaevoca.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.MergeResult;
import com.example.hamhaevoca.model.QuizAttemptSummary;
import com.example.hamhaevoca.model.VocabItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "hamhaevoca.db";
    private static final int DB_VERSION = 4;

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
        db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE)");
        db.execSQL("CREATE TABLE books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "source_type TEXT NOT NULL," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL)");

        db.execSQL("CREATE TABLE words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "term TEXT NOT NULL," +
                "meaning TEXT NOT NULL," +
                "example TEXT NOT NULL DEFAULT ''," +
                "past TEXT NOT NULL DEFAULT ''," +
                "past_participle TEXT NOT NULL DEFAULT ''," +
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
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE)");
            db.execSQL("ALTER TABLE books ADD COLUMN folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE words ADD COLUMN example TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE words ADD COLUMN past TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE words ADD COLUMN past_participle TEXT NOT NULL DEFAULT ''");
        }
    }

    public boolean isVerbBook(long bookId) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT 1 FROM words WHERE book_id = ? AND past != '' AND past_participle != '' LIMIT 1", new String[]{String.valueOf(bookId)});
        try { return cursor.moveToFirst(); } finally { cursor.close(); }
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

            int order = 1;
            for (VocabItem item : items) {
                ContentValues values = new ContentValues();
                values.put("book_id", bookId);
                values.put("term", item.getTerm());
                values.put("meaning", item.getMeaning());
                values.put("example", item.getExample());
                values.put("past", item.getPast());
                values.put("past_participle", item.getPastParticiple());
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

    /**
     * Existing study history is preserved while imported words are merged into a book.
     *
     * Matching order:
     * 1) normalized term + meaning exact match
     * 2) a unique normalized term match (allows a meaning correction)
     * 3) otherwise insert as a new word
     *
     * Existing words that are not included in the imported file are kept and appended after
     * the imported rows. This prevents an accidental import from deleting study history.
     */
    public MergeResult mergeBookWords(long bookId, String sourceType, List<VocabItem> importedItems) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<VocabItem> existingItems = readWords(db, bookId);
            Map<String, List<VocabItem>> exactMatches = new HashMap<String, List<VocabItem>>();
            Map<String, List<VocabItem>> termMatches = new HashMap<String, List<VocabItem>>();
            for (VocabItem item : existingItems) {
                addToMap(exactMatches, wordKey(item.getTerm(), item.getMeaning()), item);
                addToMap(termMatches, normalize(item.getTerm()), item);
            }

            Map<String, Integer> importedTermCounts = new HashMap<String, Integer>();
            for (VocabItem item : importedItems) {
                String key = normalize(item.getTerm());
                Integer count = importedTermCounts.get(key);
                importedTermCounts.put(key, count == null ? 1 : count + 1);
            }

            Set<Long> usedIds = new HashSet<Long>();
            String timestamp = now();
            int added = 0;
            int updated = 0;
            int retained = 0;
            int order = 0;

            for (VocabItem imported : importedItems) {
                VocabItem match = firstUnused(exactMatches.get(
                        wordKey(imported.getTerm(), imported.getMeaning())), usedIds);

                String normalizedTerm = normalize(imported.getTerm());
                if (match == null && importedTermCounts.get(normalizedTerm) != null
                        && importedTermCounts.get(normalizedTerm) == 1) {
                    List<VocabItem> sameTerm = unusedItems(termMatches.get(normalizedTerm), usedIds);
                    if (sameTerm.size() == 1) match = sameTerm.get(0);
                }

                if (match == null) {
                    ContentValues values = new ContentValues();
                    values.put("book_id", bookId);
                    values.put("term", imported.getTerm());
                    values.put("meaning", imported.getMeaning());
                values.put("example", imported.getExample());
                values.put("past", imported.getPast());
                values.put("past_participle", imported.getPastParticiple());
                    values.put("sort_order", order++);
                    values.put("memorized", 0);
                    values.put("correct_count", 0);
                    values.put("incorrect_count", 0);
                    values.put("created_at", timestamp);
                    values.put("updated_at", timestamp);
                    db.insertOrThrow("words", null, values);
                    added++;
                    continue;
                }

                usedIds.add(match.getId());
                boolean contentChanged = !match.getTerm().equals(imported.getTerm())
                        || !match.getMeaning().equals(imported.getMeaning())
                        || !match.getExample().equals(imported.getExample())
                        || !match.getPast().equals(imported.getPast())
                        || !match.getPastParticiple().equals(imported.getPastParticiple());
                ContentValues values = new ContentValues();
                values.put("term", imported.getTerm());
                values.put("meaning", imported.getMeaning());
                values.put("example", imported.getExample());
                values.put("past", imported.getPast());
                values.put("past_participle", imported.getPastParticiple());
                values.put("sort_order", order++);
                if (contentChanged) values.put("updated_at", timestamp);
                db.update("words", values, "id = ? AND book_id = ?",
                        new String[]{String.valueOf(match.getId()), String.valueOf(bookId)});
                if (contentChanged) updated++;
                else retained++;
            }

            // Keep words omitted from the new file. Their study counts stay intact.
            for (VocabItem existing : existingItems) {
                if (usedIds.contains(existing.getId())) continue;
                ContentValues values = new ContentValues();
                values.put("sort_order", order++);
                db.update("words", values, "id = ? AND book_id = ?",
                        new String[]{String.valueOf(existing.getId()), String.valueOf(bookId)});
            }

            ContentValues bookValues = new ContentValues();
            bookValues.put("source_type", sourceType);
            bookValues.put("updated_at", timestamp);
            int affected = db.update("books", bookValues, "id = ?",
                    new String[]{String.valueOf(bookId)});
            if (affected != 1) throw new IllegalArgumentException("갱신할 단어장을 찾을 수 없습니다.");

            db.setTransactionSuccessful();
            return new MergeResult(added, updated, retained, existingItems.size() - usedIds.size());
        } finally {
            db.endTransaction();
        }
    }

    private List<VocabItem> readWords(SQLiteDatabase db, long bookId) {
        List<VocabItem> result = new ArrayList<VocabItem>();
        Cursor cursor = db.query("words",
                new String[]{"id", "book_id", "term", "meaning", "sort_order",
                        "memorized", "correct_count", "incorrect_count", "example", "past", "past_participle"},
                "book_id = ?", new String[]{String.valueOf(bookId)}, null, null,
                "sort_order ASC");
        try {
            while (cursor.moveToNext()) result.add(readWord(cursor));
        } finally {
            cursor.close();
        }
        return result;
    }

    private void addToMap(Map<String, List<VocabItem>> map, String key, VocabItem item) {
        List<VocabItem> values = map.get(key);
        if (values == null) {
            values = new ArrayList<VocabItem>();
            map.put(key, values);
        }
        values.add(item);
    }

    private VocabItem firstUnused(List<VocabItem> items, Set<Long> usedIds) {
        if (items == null) return null;
        for (VocabItem item : items) {
            if (!usedIds.contains(item.getId())) return item;
        }
        return null;
    }

    private List<VocabItem> unusedItems(List<VocabItem> items, Set<Long> usedIds) {
        List<VocabItem> result = new ArrayList<VocabItem>();
        if (items == null) return result;
        for (VocabItem item : items) {
            if (!usedIds.contains(item.getId())) result.add(item);
        }
        return result;
    }

    private String wordKey(String term, String meaning) {
        return normalize(term) + "\u0001" + normalize(meaning);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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
                new String[]{"id", "book_id", "term", "meaning", "sort_order", "memorized", "correct_count", "incorrect_count", "example", "past", "past_participle"},
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
                "SELECT id, book_id, term, meaning, sort_order, memorized, correct_count, incorrect_count, example, past, past_participle " +
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
        VocabItem item = new VocabItem(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                cursor.getString(3), cursor.getInt(4), cursor.getInt(5) == 1,
                cursor.getInt(6), cursor.getInt(7));
        item.setExample(cursor.getString(8));
        int pastIndex = cursor.getColumnIndex("past");
        int participleIndex = cursor.getColumnIndex("past_participle");
        if (pastIndex >= 0) item.setPast(cursor.getString(pastIndex));
        if (participleIndex >= 0) item.setPastParticiple(cursor.getString(participleIndex));
        return item;
    }

    public long addWord(long bookId, String term, String meaning) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM words WHERE book_id = ?",
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

    /**
     * 낱말카드에서 "못 외움"으로 판정한 횟수를 오답 횟수에 반영한다.
     */
    public void incrementIncorrectCount(long wordId, long bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE words SET incorrect_count = incorrect_count + 1, updated_at = ? "
                        + "WHERE id = ? AND book_id = ?",
                new Object[]{now(), wordId, bookId});
        touchBook(db, bookId);
    }

    /**
     * 낱말카드의 이전 단어 기능으로 직전 "못 외움" 판정을 취소할 때 사용한다.
     * 오답 횟수는 0 아래로 내려가지 않는다.
     */
    public void decrementIncorrectCount(long wordId, long bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE words SET incorrect_count = MAX(incorrect_count - 1, 0), updated_at = ? "
                        + "WHERE id = ? AND book_id = ?",
                new Object[]{now(), wordId, bookId});
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

    public static final class Folder {
        public final long id;
        public final String name;
        public Folder(long id, String name) { this.id = id; this.name = name; }
    }

    public List<Folder> getFolders() {
        List<Folder> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("folders", new String[]{"id", "name"},
                null, null, null, null, "name COLLATE NOCASE ASC")) {
            while (c.moveToNext()) result.add(new Folder(c.getLong(0), c.getString(1)));
        }
        return result;
    }

    public long createFolder(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("폴더 이름을 입력하세요.");
        ContentValues v = new ContentValues(); v.put("name", clean);
        return getWritableDatabase().insertOrThrow("folders", null, v);
    }

    public void renameFolder(long id, String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("폴더 이름을 입력하세요.");
        ContentValues v = new ContentValues(); v.put("name", clean);
        getWritableDatabase().update("folders", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteFolder(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues(); v.putNull("folder_id");
            db.update("books", v, "folder_id=?", new String[]{String.valueOf(id)});
            db.delete("folders", "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void setBookFolder(long bookId, Long folderId) {
        ContentValues v = new ContentValues();
        if (folderId == null) v.putNull("folder_id"); else v.put("folder_id", folderId);
        getWritableDatabase().update("books", v, "id=?", new String[]{String.valueOf(bookId)});
    }

    public Long getBookFolderId(long bookId) {
        try (Cursor c = getReadableDatabase().query("books", new String[]{"folder_id"},
                "id=?", new String[]{String.valueOf(bookId)}, null, null, null)) {
            return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : null;
        }
    }

    public String getBookFolderName(long bookId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT f.name FROM books b LEFT JOIN folders f ON b.folder_id=f.id WHERE b.id=?",
                new String[]{String.valueOf(bookId)})) {
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "미분류";
        }
    }
}
