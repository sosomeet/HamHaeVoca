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

public class FlashcardActivity extends LocalizedActivity {
    private static final String PREFS_NAME = "flashcard_settings";
    private static final String PREF_ORDER = "order";
    private static final String PREF_MEANING_FRONT = "meaning_front";
    private static final String PREF_BATCH_SIZE = "batch_size";
    private static final int DEFAULT_BATCH_SIZE = 30;
    private static final String PREF_MAIN_FONT_SIZE = "main_font_size";
    private static final String PREF_EXAMPLE_FONT_SIZE = "example_font_size";

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
    private View cardMotionView;
    private TextView exampleTextView;
    private View exampleSection;
    private int mainFontSize = 32;
    private int exampleFontSize = 18;
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
    private int verbFlipStage = 0;
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
        mainFontSize = preferences.getInt(PREF_MAIN_FONT_SIZE, 32);
        exampleFontSize = preferences.getInt(PREF_EXAMPLE_FONT_SIZE, 18);

        String wrongIds = getIntent().getStringExtra("wrong_ids");
        sessionPrefix = "session_" + bookId + "_" + (wrongIds == null ? 0 : wrongIds.hashCode()) + "_";

        batchInfoView = findViewById(R.id.flashBatchInfo);
        roundInfoView = findViewById(R.id.flashRoundInfo);
        wordInfoView = findViewById(R.id.flashWordInfo);
        notLearnedCountView = findViewById(R.id.flashNotLearnedCount);
        learnedCountView = findViewById(R.id.flashLearnedCount);
        cardView = findViewById(R.id.flashCard);
        cardMotionView = findViewById(R.id.flashCardMotion);
        exampleTextView = findViewById(R.id.flashExampleText);
        exampleSection = findViewById(R.id.flashExampleSection);
        swipeBackground = findViewById(R.id.flashSwipeBackground);
        previousButton = findViewById(R.id.flashPreviousButton);

        findViewById(R.id.flashBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.flashSettingsButton).setOnClickListener(v -> showSettingsDialog());
        cardMotionView.setOnTouchListener((v, event) -> handleCardTouch(event));
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
        EditText mainFontInput = view.findViewById(R.id.dialogFlashMainFontSize);
        EditText exampleFontInput = view.findViewById(R.id.dialogFlashExampleFontSize);
        mainFontInput.setText(String.valueOf(mainFontSize));
        exampleFontInput.setText(String.valueOf(exampleFontSize));

        String[] orders = new String[]{Lang.text(FlashcardActivity.this, R.string.ui_da17dca8e034), Lang.text(FlashcardActivity.this, R.string.ui_f44aa6624b0d), Lang.text(FlashcardActivity.this, R.string.ui_39a8b5d2af6e)};
        orderSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, orders));
        orderSpinner.setSelection(selectedOrder);
        frontSwitch.setChecked(meaningOnFront);
        batchInput.setText(String.valueOf(batchSize));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_749b645b4d98))
                .setView(view)
                .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_e01dd02f527f), null)
                .setNegativeButton(Lang.text(FlashcardActivity.this, R.string.ui_19b2d19bc141), null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int newOrder = orderSpinner.getSelectedItemPosition();
                    boolean newMeaningOnFront = frontSwitch.isChecked();
                    int newBatchSize;
                    try {
                        newBatchSize = Integer.parseInt(batchInput.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        batchInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_8c5c02f2a819));
                        return;
                    }
                    if (newBatchSize < 1) {
                        batchInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_9248737339ea));
                        return;
                    }

                    int newMainFontSize;
                    int newExampleFontSize;
                    try {
                        newMainFontSize = Integer.parseInt(mainFontInput.getText().toString().trim());
                        newExampleFontSize = Integer.parseInt(exampleFontInput.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        mainFontInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_8c5c02f2a819));
                        exampleFontInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_8c5c02f2a819));
                        return;
                    }
                    if (newMainFontSize < 12 || newMainFontSize > 60 || newExampleFontSize < 10 || newExampleFontSize > 36) {
                        mainFontInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_869267ae4def));
                        exampleFontInput.setError(Lang.text(FlashcardActivity.this, R.string.ui_8d06be4fabb9));
                        return;
                    }
                    boolean sessionSettingChanged = newOrder != selectedOrder
                            || newBatchSize != batchSize;
                    if (sessionSettingChanged) {
                        new AlertDialog.Builder(this)
                                .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_29a0275b7057))
                                .setMessage(Lang.text(FlashcardActivity.this, R.string.ui_99268c984d35))
                                .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_ce0109bedc59), (confirmDialog, which) -> {
                                    applyFontSizes(newMainFontSize, newExampleFontSize);
                                    applySettings(newOrder, newMeaningOnFront, newBatchSize, true);
                                    dialog.dismiss();
                                })
                                .setNegativeButton(Lang.text(FlashcardActivity.this, R.string.ui_19b2d19bc141), null)
                                .show();
                    } else {
                        applyFontSizes(newMainFontSize, newExampleFontSize);
                        applySettings(newOrder, newMeaningOnFront, newBatchSize, false);
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private void applyFontSizes(int mainSize, int exampleSize) {
        mainFontSize = mainSize;
        exampleFontSize = exampleSize;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(PREF_MAIN_FONT_SIZE, mainSize)
                .putInt(PREF_EXAMPLE_FONT_SIZE, exampleSize)
                .apply();
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
        verbFlipStage = 0;
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
                    .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_3c88a2907378))
                    .setMessage(Lang.text(FlashcardActivity.this, R.string.ui_68ed4a25b62e))
                    .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_468266d639b5), (d, w) -> finish())
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
        verbFlipStage = 0;
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
            cardMotionView.setTranslationX(dx);
            float rotation = Math.max(-11f, Math.min(11f, dx / 32f));
            cardMotionView.setRotation(rotation);
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
                cardMotionView.animate().translationX(0f).rotation(0f)
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
        float width = Math.max(1f, cardMotionView.getWidth());
        float alpha = Math.min(0.9f, Math.abs(dx) / (width * 0.42f));
        swipeBackground.setAlpha(alpha);
    }

    private void resetSwipeFeedback() {
        swipeBackground.setAlpha(0f);
    }

    private void resetSwipeVisuals() {
        cardMotionView.animate().cancel();
        cardMotionView.setTranslationX(0f);
        cardMotionView.setRotation(0f);
        cardMotionView.setAlpha(1f);
        resetSwipeFeedback();
    }

    private void animateJudge(final boolean learned) {
        if (locked || index >= roundWords.size()) return;
        locked = true;
        swipeBackground.setBackgroundResource(learned
                ? R.drawable.bg_swipe_learned : R.drawable.bg_swipe_wrong);
        swipeBackground.setAlpha(0.75f);
        float width = Math.max(cardMotionView.getWidth(),
                getResources().getDisplayMetrics().widthPixels);
        float target = learned ? width * 1.1f : -width * 1.1f;
        cardMotionView.animate().translationX(target).rotation(learned ? 12f : -12f)
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
            // 낱말카드의 "못 외움"도 해당 단어의 누적 오답 횟수에 포함한다.
            db.incrementIncorrectCount(item.getId(), bookId);
            notLearnedCount++;
            notLearned.add(item);
        }
        index++;
        showingFront = true;
        verbFlipStage = 0;
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
            Toast.makeText(this, Lang.text(FlashcardActivity.this, R.string.ui_902135a7a433), Toast.LENGTH_SHORT).show();
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
            // 이전 단어로 돌아가 직전 "못 외움" 판정을 취소하면 오답 횟수도 함께 복구한다.
            db.decrementIncorrectCount(decision.itemId, bookId);
            notLearnedCount = Math.max(0, notLearnedCount - 1);
            removeLastById(notLearned, decision.itemId);
        }
        showingFront = true;
        verbFlipStage = 0;
        locked = false;
        resetSwipeVisuals();
        saveSession();
        renderCard();
    }

    private void finishRound() {
        if (!notLearned.isEmpty()) {
            final int remaining = notLearned.size();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_f582aa50a7cb))
                    .setMessage(Lang.text(FlashcardActivity.this, R.string.ui_7215496f65a5) + remaining
                            + Lang.text(FlashcardActivity.this, R.string.ui_2271a22517e3))
                    .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_995224fb92dd), (d, w) -> {
                        roundNumber++;
                        setRound(new ArrayList<VocabItem>(notLearned));
                    })
                    .setNeutralButton(Lang.text(FlashcardActivity.this, R.string.ui_82f639b9917f), (d, w) -> goToPreviousWord())
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
                .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_f9609c333c08))
                .setMessage(Lang.text(FlashcardActivity.this, R.string.ui_57ff445a7f6b) + sourceWords.size() + Lang.text(FlashcardActivity.this, R.string.ui_bfee04c75c70) + completedEnd
                        + Lang.text(FlashcardActivity.this, R.string.ui_08d3e3ea66f7) + nextCount + Lang.text(FlashcardActivity.this, R.string.ui_774e51945005))
                .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_80258af91e08) + nextCount + Lang.text(FlashcardActivity.this, R.string.ui_11600c9ada77), (d, w) -> {
                    batchStartIndex = completedEnd;
                    startBatch();
                })
                .setNegativeButton(Lang.text(FlashcardActivity.this, R.string.ui_ecbcedd66ec0), (d, w) -> {
                    batchStartIndex = completedEnd;
                    roundWords.clear();
                    batchWords.clear();
                    history.clear();
                    notLearned.clear();
                    index = 0;
                    saveSession();
                    finish();
                })
                .setNeutralButton(Lang.text(FlashcardActivity.this, R.string.ui_82f639b9917f), (d, w) -> goToPreviousWord())
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void showAllCompleted() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(Lang.text(FlashcardActivity.this, R.string.ui_994c3cd972db))
                .setMessage(Lang.text(FlashcardActivity.this, R.string.ui_57ff445a7f6b) + sourceWords.size() + Lang.text(FlashcardActivity.this, R.string.ui_0c63a0c68313))
                .setPositiveButton(Lang.text(FlashcardActivity.this, R.string.ui_d54c7410e96c), (d, w) -> {
                    clearSavedSession();
                    startNewSession();
                })
                .setNegativeButton(Lang.text(FlashcardActivity.this, R.string.ui_e34a53c95b0c), (d, w) -> {
                    clearSavedSession();
                    finish();
                })
                .setNeutralButton(Lang.text(FlashcardActivity.this, R.string.ui_82f639b9917f), (d, w) -> goToPreviousWord())
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void flipCard() {
        if (index >= roundWords.size()) return;
        if (roundWords.get(index).isVerb()) {
            verbFlipStage = (verbFlipStage + 1) % 4;
        } else {
            showingFront = !showingFront;
        }
        cardMotionView.animate().alpha(0f).setDuration(80).withEndAction(() -> {
            renderCard();
            cardMotionView.animate().alpha(1f).setDuration(80).start();
        }).start();
    }

    private void renderCard() {
        previousButton.setEnabled(!history.isEmpty());
        previousButton.setAlpha(history.isEmpty() ? 0.45f : 1f);
        if (roundWords.isEmpty() || index >= roundWords.size()) return;
        VocabItem item = roundWords.get(index);
        boolean showMeaning = showingFront ? meaningOnFront : !meaningOnFront;
        cardView.setTextSize(mainFontSize);
        if (item.isVerb()) {
            String[] stages = {item.getTerm(), item.getPast(), item.getPastParticiple(), item.getMeaning()};
            cardView.setText(stages[verbFlipStage]);
        } else {
            cardView.setText(showMeaning ? item.getMeaning() : item.getTerm());
        }
        // Verb cards show the 1/2/3 stage index in the example area even
        // when the imported workbook does not contain an Example column.
        // The final meaning stage has no index.
        String example = item.getExample() == null ? "" : item.getExample().trim();
        boolean showExample = item.isVerb()
                ? verbFlipStage < 3 || !example.isEmpty()
                : !showingFront && !example.isEmpty();
        exampleSection.setVisibility(showExample ? View.VISIBLE : View.GONE);
        if (showExample) {
            exampleTextView.setTextSize(exampleFontSize);
            if (item.isVerb() && verbFlipStage < 3) {
                String stageIndex = String.valueOf(verbFlipStage + 1);
                exampleTextView.setText(example.isEmpty()
                        ? stageIndex : stageIndex + ". " + example);
            } else {
                exampleTextView.setText(example);
            }
        }

        int totalBatches = (int) Math.ceil(sourceWords.size() / (double) batchSize);
        int currentBatch = Math.min(totalBatches, batchStartIndex / batchSize + 1);
        batchInfoView.setText(Lang.text(FlashcardActivity.this, R.string.ui_999e4d77039f) + currentBatch + " / " + totalBatches);
        roundInfoView.setText(Lang.text(FlashcardActivity.this, R.string.ui_c54e76624613) + roundNumber);
        wordInfoView.setText((index + 1) + " / " + roundWords.size());
        notLearnedCountView.setText(Lang.text(FlashcardActivity.this, R.string.ui_8aa58b6c42a8) + notLearnedCount);
        learnedCountView.setText(Lang.text(FlashcardActivity.this, R.string.ui_0aa87f7acd40) + learnedCount);
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
        verbFlipStage = 0;
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
