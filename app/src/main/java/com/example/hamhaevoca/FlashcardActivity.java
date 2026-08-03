package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FlashcardActivity extends Activity {
    private static final String PREFS_NAME = "flashcard_settings";
    private static final String PREF_ORDER = "order";
    private static final String PREF_MEANING_FRONT = "meaning_front";
    private static final String PREF_BATCH_SIZE = "batch_size";
    private static final int DEFAULT_BATCH_SIZE = 30;

    private static final String SESSION_PREFS = "flashcard_sessions";
    private static final String FIELD_SOURCE_IDS = "source_ids";
    private static final String FIELD_BATCH_START = "batch_start";
    private static final String FIELD_BATCH_SIZE = "session_batch_size";
    private static final String FIELD_ORDER = "session_order";
    private static final String FIELD_ROUND_IDS = "round_ids";
    private static final String FIELD_NOT_LEARNED_IDS = "not_learned_ids";
    private static final String FIELD_HISTORY_IDS = "history_ids";
    private static final String FIELD_HISTORY_LEARNED = "history_learned";
    private static final String FIELD_HISTORY_PREVIOUS = "history_previous";
    private static final String FIELD_INDEX = "index";
    private static final String FIELD_LEARNED_COUNT = "learned_count";
    private static final String FIELD_NOT_LEARNED_COUNT = "not_learned_count";
    private static final String FIELD_ROUND_NUMBER = "round_number";

    private long bookId;
    private DatabaseHelper db;
    private TextView batchInfoView;
    private TextView roundInfoView;
    private TextView wordInfoView;
    private TextView notLearnedCountView;
    private TextView learnedCountView;
    private TextView cardView;
    private View swipeBackground;
    private Button previousButton;

    private int selectedOrder = 0;
    private boolean meaningOnFront = false;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private final List<VocabItem> rawSourceWords = new ArrayList<VocabItem>();
    private final List<VocabItem> sourceWords = new ArrayList<VocabItem>();
    private final List<VocabItem> batchWords = new ArrayList<VocabItem>();
    private final List<VocabItem> roundWords = new ArrayList<VocabItem>();
    private final List<VocabItem> notLearned = new ArrayList<VocabItem>();
    private final List<Decision> history = new ArrayList<Decision>();

    private int batchStartIndex = 0;
    private int index = 0;
    private int learnedCount = 0;
    private int notLearnedCount = 0;
    private int roundNumber = 1;
    private boolean showingFront = true;
    private boolean locked = false;
    private float downX;
    private float downY;
    private String sessionPrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);
        bookId = getIntent().getLongExtra("book_id", -1);
        db = DatabaseHelper.getInstance(this);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedOrder = preferences.getInt(PREF_ORDER, 0);
        if (selectedOrder < 0 || selectedOrder > 2) selectedOrder = 0;
        meaningOnFront = preferences.getBoolean(PREF_MEANING_FRONT, false);
        batchSize = Math.max(1, preferences.getInt(PREF_BATCH_SIZE, DEFAULT_BATCH_SIZE));

        String wrongIds = getIntent().getStringExtra("wrong_ids");
        sessionPrefix = "session_" + bookId + "_" + (wrongIds == null ? 0 : wrongIds.hashCode()) + "_";

        batchInfoView = findViewById(R.id.flashBatchInfo);
        roundInfoView = findViewById(R.id.flashRoundInfo);
        wordInfoView = findViewById(R.id.flashWordInfo);
        notLearnedCountView = findViewById(R.id.flashNotLearnedCount);
        learnedCountView = findViewById(R.id.flashLearnedCount);
        cardView = findViewById(R.id.flashCard);
        swipeBackground = findViewById(R.id.flashSwipeBackground);
        previousButton = findViewById(R.id.flashPreviousButton);

        findViewById(R.id.flashBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.flashSettingsButton).setOnClickListener(v -> showSettingsDialog());
        cardView.setOnTouchListener((v, event) -> handleCardTouch(event));
        previousButton.setOnClickListener(v -> goToPreviousWord());
        findViewById(R.id.notLearnedButton).setOnClickListener(v -> judge(false));
        findViewById(R.id.learnedButton).setOnClickListener(v -> judge(true));

        loadWords(wrongIds);
    }

    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_flashcard_settings, null);
        Spinner orderSpinner = view.findViewById(R.id.dialogFlashOrderSpinner);
        Switch frontSwitch = view.findViewById(R.id.dialogFrontSideSwitch);
        EditText batchInput = view.findViewById(R.id.dialogFlashBatchSizeInput);

        String[] orders = new String[]{"단어장 순서", "무작위", "알파벳순"};
        orderSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, orders));
        orderSpinner.setSelection(selectedOrder);
        frontSwitch.setChecked(meaningOnFront);
        batchInput.setText(String.valueOf(batchSize));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("낱말카드 설정")
                .setView(view)
                .setPositiveButton("적용", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int newOrder = orderSpinner.getSelectedItemPosition();
                    boolean newMeaningOnFront = frontSwitch.isChecked();
                    int newBatchSize;
                    try {
                        newBatchSize = Integer.parseInt(batchInput.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        batchInput.setError("숫자를 입력하세요.");
                        return;
                    }
                    if (newBatchSize < 1) {
                        batchInput.setError("1개 이상 입력하세요.");
                        return;
                    }

                    boolean sessionSettingChanged = newOrder != selectedOrder
                            || newBatchSize != batchSize;
                    if (sessionSettingChanged) {
                        new AlertDialog.Builder(this)
                                .setTitle("학습 묶음 다시 시작")
                                .setMessage("학습 순서나 묶음 크기를 바꾸면 현재 낱말카드 진행 위치가 처음으로 돌아갑니다.")
                                .setPositiveButton("변경", (confirmDialog, which) -> {
                                    applySettings(newOrder, newMeaningOnFront, newBatchSize, true);
                                    dialog.dismiss();
                                })
                                .setNegativeButton("취소", null)
                                .show();
                    } else {
                        applySettings(newOrder, newMeaningOnFront, newBatchSize, false);
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private void applySettings(int newOrder, boolean newMeaningOnFront,
                               int newBatchSize, boolean restartSession) {
        selectedOrder = newOrder;
        meaningOnFront = newMeaningOnFront;
        batchSize = newBatchSize;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(PREF_ORDER, selectedOrder)
                .putBoolean(PREF_MEANING_FRONT, meaningOnFront)
                .putInt(PREF_BATCH_SIZE, batchSize)
                .apply();
        showingFront = true;
        if (restartSession) {
            clearSavedSession();
            startNewSession();
        } else {
            renderCard();
        }
    }

    private void loadWords(String wrongIds) {
        List<VocabItem> all = db.getWords(bookId);
        if (wrongIds != null && !wrongIds.trim().isEmpty()) {
            Set<Long> ids = new HashSet<Long>();
            for (String value : wrongIds.split(",")) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (Exception ignored) {
                    // 오래된 결과 화면에서 잘못된 ID가 넘어오면 제외한다.
                }
            }
            for (VocabItem item : all) {
                if (ids.contains(item.getId())) rawSourceWords.add(item);
            }
        } else {
            rawSourceWords.addAll(all);
        }

        if (rawSourceWords.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("학습할 단어 없음")
                    .setMessage("학습할 단어가 없습니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        if (!restoreSession()) startNewSession();
    }

    private void startNewSession() {
        sourceWords.clear();
        sourceWords.addAll(rawSourceWords);
        applyOrder(sourceWords);
        batchStartIndex = 0;
        startBatch();
    }

    private void startBatch() {
        if (batchStartIndex >= sourceWords.size()) {
            showAllCompleted();
            return;
        }
        batchWords.clear();
        int end = Math.min(batchStartIndex + batchSize, sourceWords.size());
        batchWords.addAll(sourceWords.subList(batchStartIndex, end));
        roundNumber = 1;
        setRound(batchWords);
    }

    private void setRound(List<VocabItem> words) {
        roundWords.clear();
        roundWords.addAll(words);
        if (selectedOrder == 1) {
            Collections.shuffle(roundWords);
        } else if (selectedOrder == 2) {
            Collections.sort(roundWords, new Comparator<VocabItem>() {
                @Override
                public int compare(VocabItem a, VocabItem b) {
                    return safe(a.getTerm()).toLowerCase(Locale.getDefault())
                            .compareTo(safe(b.getTerm()).toLowerCase(Locale.getDefault()));
                }
            });
        }
        notLearned.clear();
        history.clear();
        index = 0;
        learnedCount = 0;
        notLearnedCount = 0;
        showingFront = true;
        locked = false;
        resetSwipeVisuals();
        saveSession();
        renderCard();
    }

    private void applyOrder(List<VocabItem> words) {
        if (selectedOrder == 1) {
            Collections.shuffle(words);
        } else if (selectedOrder == 2) {
            Collections.sort(words, new Comparator<VocabItem>() {
                @Override
                public int compare(VocabItem a, VocabItem b) {
                    return safe(a.getTerm()).toLowerCase(Locale.getDefault())
                            .compareTo(safe(b.getTerm()).toLowerCase(Locale.getDefault()));
                }
            });
        } else {
            Collections.sort(words, new Comparator<VocabItem>() {
                @Override
                public int compare(VocabItem a, VocabItem b) {
                    return Integer.compare(a.getSortOrder(), b.getSortOrder());
                }
            });
        }
    }

    private boolean handleCardTouch(MotionEvent event) {
        if (locked || roundWords.isEmpty() || index >= roundWords.size()) return true;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            resetSwipeFeedback();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = event.getRawX() - downX;
            cardView.setTranslationX(dx);
            float rotation = Math.max(-11f, Math.min(11f, dx / 32f));
            cardView.setRotation(rotation);
            showSwipeFeedback(dx);
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            float dx = event.getRawX() - downX;
            float dy = event.getRawY() - downY;
            if (Math.abs(dx) > 120) {
                animateJudge(dx > 0);
            } else {
                cardView.animate().translationX(0f).rotation(0f)
                        .setDuration(140)
                        .withEndAction(this::resetSwipeFeedback)
                        .start();
                if (Math.abs(dx) < 20 && Math.abs(dy) < 20) flipCard();
            }
            return true;
        }
        return false;
    }

    private void showSwipeFeedback(float dx) {
        if (dx == 0f) {
            resetSwipeFeedback();
            return;
        }
        swipeBackground.setBackgroundResource(dx < 0
                ? R.drawable.bg_swipe_wrong : R.drawable.bg_swipe_learned);
        float width = Math.max(1f, cardView.getWidth());
        float alpha = Math.min(0.9f, Math.abs(dx) / (width * 0.42f));
        swipeBackground.setAlpha(alpha);
    }

    private void resetSwipeFeedback() {
        swipeBackground.setAlpha(0f);
    }

    private void resetSwipeVisuals() {
        cardView.animate().cancel();
        cardView.setTranslationX(0f);
        cardView.setRotation(0f);
        cardView.setAlpha(1f);
        resetSwipeFeedback();
    }

    private void animateJudge(final boolean learned) {
        if (locked || index >= roundWords.size()) return;
        locked = true;
        swipeBackground.setBackgroundResource(learned
                ? R.drawable.bg_swipe_learned : R.drawable.bg_swipe_wrong);
        swipeBackground.setAlpha(0.75f);
        float width = Math.max(cardView.getWidth(),
                getResources().getDisplayMetrics().widthPixels);
        float target = learned ? width * 1.1f : -width * 1.1f;
        cardView.animate().translationX(target).rotation(learned ? 12f : -12f)
                .alpha(0f).setDuration(180).withEndAction(() -> {
                    processJudge(learned);
                    resetSwipeVisuals();
                }).start();
    }

    private void judge(boolean learned) {
        if (locked || roundWords.isEmpty() || index >= roundWords.size()) return;
        animateJudge(learned);
    }

    private void processJudge(boolean learned) {
        if (index >= roundWords.size()) return;
        VocabItem item = roundWords.get(index);
        history.add(new Decision(item.getId(), learned, item.isMemorized()));
        db.setMemorized(item.getId(), bookId, learned);
        item.setMemorized(learned);
        if (learned) {
            learnedCount++;
        } else {
            notLearnedCount++;
            notLearned.add(item);
        }
        index++;
        showingFront = true;
        locked = false;
        saveSession();
        if (index >= roundWords.size()) {
            finishRound();
        } else {
            renderCard();
        }
    }

    private void goToPreviousWord() {
        if (locked || history.isEmpty()) {
            Toast.makeText(this, "이전 단어가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Decision decision = history.remove(history.size() - 1);
        index = Math.max(0, index - 1);
        VocabItem item = findById(roundWords, decision.itemId);
        if (item != null) {
            db.setMemorized(item.getId(), bookId, decision.previousMemorized);
            item.setMemorized(decision.previousMemorized);
        }
        if (decision.learned) {
            learnedCount = Math.max(0, learnedCount - 1);
        } else {
            notLearnedCount = Math.max(0, notLearnedCount - 1);
            removeLastById(notLearned, decision.itemId);
        }
        showingFront = true;
        locked = false;
        resetSwipeVisuals();
        saveSession();
        renderCard();
    }

    private void finishRound() {
        if (!notLearned.isEmpty()) {
            final int remaining = notLearned.size();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("회차 완료")
                    .setMessage("아직 못 외운 단어 " + remaining
                            + "개로 다음 회차를 시작합니다.")
                    .setPositiveButton("계속", (d, w) -> {
                        roundNumber++;
                        setRound(new ArrayList<VocabItem>(notLearned));
                    })
                    .setNeutralButton("이전 단어", (d, w) -> goToPreviousWord())
                    .setCancelable(false)
                    .create();
            dialog.show();
        } else {
            showBatchCompletion();
        }
    }

    private void showBatchCompletion() {
        final int completedEnd = batchStartIndex + batchWords.size();
        if (completedEnd >= sourceWords.size()) {
            showAllCompleted();
            return;
        }
        int nextCount = Math.min(batchSize, sourceWords.size() - completedEnd);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("이번 묶음 완료")
                .setMessage("전체 " + sourceWords.size() + "개 중 " + completedEnd
                        + "개까지 학습했습니다.\n다음 " + nextCount + "개를 이어서 학습할 수 있습니다.")
                .setPositiveButton("다음 " + nextCount + "개", (d, w) -> {
                    batchStartIndex = completedEnd;
                    startBatch();
                })
                .setNegativeButton("나중에 계속", (d, w) -> {
                    batchStartIndex = completedEnd;
                    roundWords.clear();
                    batchWords.clear();
                    history.clear();
                    notLearned.clear();
                    index = 0;
                    saveSession();
                    finish();
                })
                .setNeutralButton("이전 단어", (d, w) -> goToPreviousWord())
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void showAllCompleted() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("학습 완료")
                .setMessage("전체 " + sourceWords.size() + "개 단어 학습을 완료했습니다.")
                .setPositiveButton("처음부터 다시", (d, w) -> {
                    clearSavedSession();
                    startNewSession();
                })
                .setNegativeButton("단어장으로", (d, w) -> {
                    clearSavedSession();
                    finish();
                })
                .setNeutralButton("이전 단어", (d, w) -> goToPreviousWord())
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void flipCard() {
        if (index >= roundWords.size()) return;
        showingFront = !showingFront;
        cardView.animate().alpha(0f).setDuration(80).withEndAction(() -> {
            renderCard();
            cardView.animate().alpha(1f).setDuration(80).start();
        }).start();
    }

    private void renderCard() {
        previousButton.setEnabled(!history.isEmpty());
        previousButton.setAlpha(history.isEmpty() ? 0.45f : 1f);
        if (roundWords.isEmpty() || index >= roundWords.size()) return;
        VocabItem item = roundWords.get(index);
        boolean showMeaning = showingFront ? meaningOnFront : !meaningOnFront;
        cardView.setText(showMeaning ? item.getMeaning() : item.getTerm());

        int totalBatches = (int) Math.ceil(sourceWords.size() / (double) batchSize);
        int currentBatch = Math.min(totalBatches, batchStartIndex / batchSize + 1);
        batchInfoView.setText("묶음 " + currentBatch + " / " + totalBatches);
        roundInfoView.setText("회차 " + roundNumber);
        wordInfoView.setText((index + 1) + " / " + roundWords.size());
        notLearnedCountView.setText("못 외움 " + notLearnedCount);
        learnedCountView.setText("외움 " + learnedCount);
    }

    private boolean restoreSession() {
        SharedPreferences session = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE);
        String sourceIdsRaw = session.getString(key(FIELD_SOURCE_IDS), "");
        if (sourceIdsRaw == null || sourceIdsRaw.isEmpty()) return false;
        if (session.getInt(key(FIELD_ORDER), -1) != selectedOrder) return false;
        if (session.getInt(key(FIELD_BATCH_SIZE), -1) != batchSize) return false;

        Map<Long, VocabItem> currentById = toMap(rawSourceWords);
        List<Long> persistedSourceIds = parseIds(sourceIdsRaw);
        sourceWords.clear();
        Set<Long> added = new HashSet<Long>();
        for (Long id : persistedSourceIds) {
            VocabItem item = currentById.get(id);
            if (item != null) {
                sourceWords.add(item);
                added.add(id);
            }
        }
        List<VocabItem> newItems = new ArrayList<VocabItem>();
        for (VocabItem item : rawSourceWords) {
            if (!added.contains(item.getId())) newItems.add(item);
        }
        applyOrder(newItems);
        sourceWords.addAll(newItems);
        if (sourceWords.isEmpty()) return false;

        batchStartIndex = Math.max(0, Math.min(
                session.getInt(key(FIELD_BATCH_START), 0), sourceWords.size()));
        roundNumber = Math.max(1, session.getInt(key(FIELD_ROUND_NUMBER), 1));
        learnedCount = Math.max(0, session.getInt(key(FIELD_LEARNED_COUNT), 0));
        notLearnedCount = Math.max(0, session.getInt(key(FIELD_NOT_LEARNED_COUNT), 0));

        batchWords.clear();
        if (batchStartIndex < sourceWords.size()) {
            int end = Math.min(batchStartIndex + batchSize, sourceWords.size());
            batchWords.addAll(sourceWords.subList(batchStartIndex, end));
        }

        List<Long> roundIds = parseIds(session.getString(key(FIELD_ROUND_IDS), ""));
        if (roundIds.isEmpty()) {
            startBatch();
            return true;
        }
        roundWords.clear();
        for (Long id : roundIds) {
            VocabItem item = currentById.get(id);
            if (item == null) {
                startBatch();
                return true;
            }
            roundWords.add(item);
        }

        index = Math.max(0, Math.min(session.getInt(key(FIELD_INDEX), 0), roundWords.size()));
        notLearned.clear();
        for (Long id : parseIds(session.getString(key(FIELD_NOT_LEARNED_IDS), ""))) {
            VocabItem item = currentById.get(id);
            if (item != null) notLearned.add(item);
        }
        restoreHistory(session, currentById);
        if (history.size() > index) {
            while (history.size() > index) history.remove(history.size() - 1);
        }
        showingFront = true;
        locked = false;
        resetSwipeVisuals();
        saveSession();
        if (index >= roundWords.size()) finishRound();
        else renderCard();
        return true;
    }

    private void restoreHistory(SharedPreferences session, Map<Long, VocabItem> currentById) {
        history.clear();
        List<Long> ids = parseIds(session.getString(key(FIELD_HISTORY_IDS), ""));
        List<Boolean> learnedValues = parseBooleans(
                session.getString(key(FIELD_HISTORY_LEARNED), ""));
        List<Boolean> previousValues = parseBooleans(
                session.getString(key(FIELD_HISTORY_PREVIOUS), ""));
        int size = Math.min(ids.size(), Math.min(learnedValues.size(), previousValues.size()));
        for (int i = 0; i < size; i++) {
            if (currentById.containsKey(ids.get(i))) {
                history.add(new Decision(ids.get(i), learnedValues.get(i), previousValues.get(i)));
            }
        }
    }

    private void saveSession() {
        if (sourceWords.isEmpty()) return;
        SharedPreferences.Editor editor = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).edit();
        editor.putString(key(FIELD_SOURCE_IDS), joinIds(sourceWords));
        editor.putInt(key(FIELD_BATCH_START), batchStartIndex);
        editor.putInt(key(FIELD_BATCH_SIZE), batchSize);
        editor.putInt(key(FIELD_ORDER), selectedOrder);
        editor.putString(key(FIELD_ROUND_IDS), joinIds(roundWords));
        editor.putString(key(FIELD_NOT_LEARNED_IDS), joinIds(notLearned));
        editor.putInt(key(FIELD_INDEX), index);
        editor.putInt(key(FIELD_LEARNED_COUNT), learnedCount);
        editor.putInt(key(FIELD_NOT_LEARNED_COUNT), notLearnedCount);
        editor.putInt(key(FIELD_ROUND_NUMBER), roundNumber);

        StringBuilder historyIds = new StringBuilder();
        StringBuilder historyLearned = new StringBuilder();
        StringBuilder historyPrevious = new StringBuilder();
        for (Decision decision : history) {
            append(historyIds, String.valueOf(decision.itemId));
            append(historyLearned, decision.learned ? "1" : "0");
            append(historyPrevious, decision.previousMemorized ? "1" : "0");
        }
        editor.putString(key(FIELD_HISTORY_IDS), historyIds.toString());
        editor.putString(key(FIELD_HISTORY_LEARNED), historyLearned.toString());
        editor.putString(key(FIELD_HISTORY_PREVIOUS), historyPrevious.toString());
        editor.apply();
    }

    private void clearSavedSession() {
        SharedPreferences.Editor editor = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).edit();
        String[] fields = new String[]{FIELD_SOURCE_IDS, FIELD_BATCH_START, FIELD_BATCH_SIZE,
                FIELD_ORDER, FIELD_ROUND_IDS, FIELD_NOT_LEARNED_IDS, FIELD_HISTORY_IDS,
                FIELD_HISTORY_LEARNED, FIELD_HISTORY_PREVIOUS, FIELD_INDEX,
                FIELD_LEARNED_COUNT, FIELD_NOT_LEARNED_COUNT, FIELD_ROUND_NUMBER};
        for (String field : fields) editor.remove(key(field));
        editor.apply();
    }

    private String key(String field) {
        return sessionPrefix + field;
    }

    private static String joinIds(List<VocabItem> items) {
        StringBuilder builder = new StringBuilder();
        for (VocabItem item : items) append(builder, String.valueOf(item.getId()));
        return builder.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (builder.length() > 0) builder.append(',');
        builder.append(value);
    }

    private static List<Long> parseIds(String raw) {
        ArrayList<Long> result = new ArrayList<Long>();
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String value : raw.split(",")) {
            try {
                result.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // 손상된 저장 값만 건너뛴다.
            }
        }
        return result;
    }

    private static List<Boolean> parseBooleans(String raw) {
        ArrayList<Boolean> result = new ArrayList<Boolean>();
        if (raw == null || raw.trim().isEmpty()) return result;
        for (String value : raw.split(",")) result.add("1".equals(value.trim()));
        return result;
    }

    private static Map<Long, VocabItem> toMap(List<VocabItem> items) {
        Map<Long, VocabItem> result = new HashMap<Long, VocabItem>();
        for (VocabItem item : items) result.put(item.getId(), item);
        return result;
    }

    private static VocabItem findById(List<VocabItem> items, long id) {
        for (VocabItem item : items) {
            if (item.getId() == id) return item;
        }
        return null;
    }

    private static void removeLastById(List<VocabItem> items, long id) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).getId() == id) {
                items.remove(i);
                return;
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void onPause() {
        saveSession();
        super.onPause();
    }

    private static final class Decision {
        private final long itemId;
        private final boolean learned;
        private final boolean previousMemorized;

        private Decision(long itemId, boolean learned, boolean previousMemorized) {
            this.itemId = itemId;
            this.learned = learned;
            this.previousMemorized = previousMemorized;
        }
    }
}
