package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.VocabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FlashcardActivity extends Activity {
    private static final String PREFS_NAME = "flashcard_settings";
    private static final String PREF_ORDER = "order";
    private static final String PREF_MEANING_FRONT = "meaning_front";

    private long bookId;
    private DatabaseHelper db;
    private TextView progressView;
    private TextView countView;
    private TextView cardView;
    private View swipeBackground;

    private int selectedOrder = 0;
    private boolean meaningOnFront = false;

    private final List<VocabItem> sourceWords = new ArrayList<VocabItem>();
    private final List<VocabItem> roundWords = new ArrayList<VocabItem>();
    private final List<VocabItem> notLearned = new ArrayList<VocabItem>();
    private int index = 0;
    private int learnedCount = 0;
    private int notLearnedCount = 0;
    private int roundNumber = 1;
    private boolean showingFront = true;
    private boolean locked = false;
    private float downX;
    private float downY;

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

        progressView = findViewById(R.id.flashProgress);
        countView = findViewById(R.id.flashCounts);
        cardView = findViewById(R.id.flashCard);
        swipeBackground = findViewById(R.id.flashSwipeBackground);

        findViewById(R.id.flashBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.flashSettingsButton).setOnClickListener(v -> showSettingsDialog());
        cardView.setOnTouchListener((v, event) -> handleCardTouch(event));
        findViewById(R.id.notLearnedButton).setOnClickListener(v -> judge(false));
        findViewById(R.id.learnedButton).setOnClickListener(v -> judge(true));

        loadWords();
    }

    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_flashcard_settings, null);
        Spinner orderSpinner = view.findViewById(R.id.dialogFlashOrderSpinner);
        Switch frontSwitch = view.findViewById(R.id.dialogFrontSideSwitch);

        String[] orders = new String[]{"단어장 순서", "무작위", "알파벳순"};
        orderSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, orders));
        orderSpinner.setSelection(selectedOrder);
        frontSwitch.setChecked(meaningOnFront);

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
                    boolean orderChanged = newOrder != selectedOrder;

                    selectedOrder = newOrder;
                    meaningOnFront = newMeaningOnFront;
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putInt(PREF_ORDER, selectedOrder)
                            .putBoolean(PREF_MEANING_FRONT, meaningOnFront)
                            .apply();
                    dialog.dismiss();

                    showingFront = true;
                    if (orderChanged) startNewSession();
                    else renderCard();
                }));
        dialog.show();
    }

    private void loadWords() {
        List<VocabItem> all = db.getWords(bookId);
        String wrongIds = getIntent().getStringExtra("wrong_ids");
        if (wrongIds != null && !wrongIds.trim().isEmpty()) {
            Set<Long> ids = new HashSet<Long>();
            for (String value : wrongIds.split(",")) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (Exception ignored) {
                    // Ignore malformed IDs supplied by a stale result screen.
                }
            }
            for (VocabItem item : all) {
                if (ids.contains(item.getId())) sourceWords.add(item);
            }
        } else {
            sourceWords.addAll(all);
        }
        if (sourceWords.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("학습할 단어 없음")
                    .setMessage("학습할 단어가 없습니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        startNewSession();
    }

    private void startNewSession() {
        roundNumber = 1;
        learnedCount = 0;
        notLearnedCount = 0;
        setRound(sourceWords);
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
                    return a.getTerm().toLowerCase(Locale.getDefault())
                            .compareTo(b.getTerm().toLowerCase(Locale.getDefault()));
                }
            });
        }
        notLearned.clear();
        index = 0;
        learnedCount = 0;
        notLearnedCount = 0;
        showingFront = true;
        locked = false;
        resetSwipeVisuals();
        renderCard();
    }

    private boolean handleCardTouch(MotionEvent event) {
        if (locked || roundWords.isEmpty()) return true;
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
        if (locked) return;
        locked = true;
        swipeBackground.setBackgroundResource(learned
                ? R.drawable.bg_swipe_learned : R.drawable.bg_swipe_wrong);
        swipeBackground.setAlpha(0.75f);
        float width = Math.max(cardView.getWidth(), getResources().getDisplayMetrics().widthPixels);
        float target = learned ? width * 1.1f : -width * 1.1f;
        cardView.animate().translationX(target).rotation(learned ? 12f : -12f)
                .alpha(0f).setDuration(180).withEndAction(() -> {
                    processJudge(learned);
                    resetSwipeVisuals();
                }).start();
    }

    private void judge(boolean learned) {
        if (locked || roundWords.isEmpty()) return;
        animateJudge(learned);
    }

    private void processJudge(boolean learned) {
        if (index >= roundWords.size()) return;
        VocabItem item = roundWords.get(index);
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
        if (index >= roundWords.size()) {
            finishRound();
        } else {
            locked = false;
            renderCard();
        }
    }

    private void finishRound() {
        if (!notLearned.isEmpty()) {
            final int remaining = notLearned.size();
            new AlertDialog.Builder(this)
                    .setTitle("회차 완료")
                    .setMessage("아직 못 외운 단어 " + remaining
                            + "개로 다음 회차를 시작합니다.")
                    .setPositiveButton("계속", (d, w) -> {
                        roundNumber++;
                        setRound(new ArrayList<VocabItem>(notLearned));
                    })
                    .setCancelable(false)
                    .show();
        } else {
            showCompletion();
        }
    }

    private void showCompletion() {
        new AlertDialog.Builder(this)
                .setTitle("학습 완료")
                .setMessage("모든 단어를 외웠습니다.\n완료 회차: " + roundNumber + "회")
                .setPositiveButton("전체 다시 학습", (d, w) -> startNewSession())
                .setNeutralButton("암기 상태 초기화", (d, w) -> confirmReset())
                .setNegativeButton("단어장으로", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("암기 상태 초기화")
                .setMessage("이 단어장의 외움 표시를 모두 초기화할까요?")
                .setPositiveButton("초기화", (d, w) -> {
                    db.resetMemorized(bookId);
                    Toast.makeText(this, "초기화했습니다.", Toast.LENGTH_SHORT).show();
                    startNewSession();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void flipCard() {
        showingFront = !showingFront;
        cardView.animate().alpha(0f).setDuration(80).withEndAction(() -> {
            renderCard();
            cardView.animate().alpha(1f).setDuration(80).start();
        }).start();
    }

    private void renderCard() {
        if (roundWords.isEmpty() || index >= roundWords.size()) return;
        VocabItem item = roundWords.get(index);
        boolean showMeaning = showingFront ? meaningOnFront : !meaningOnFront;
        cardView.setText(showMeaning ? item.getMeaning() : item.getTerm());
        progressView.setText("회차 " + roundNumber + " · " + (index + 1)
                + " / " + roundWords.size());
        countView.setText("외움 " + learnedCount + " · 못 외움 " + notLearnedCount
                + "\n카드를 눌러 앞뒤 전환");
    }
}
