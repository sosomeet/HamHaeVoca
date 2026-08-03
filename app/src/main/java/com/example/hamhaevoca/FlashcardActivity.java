package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    private long bookId;
    private DatabaseHelper db;
    private TextView progressView;
    private TextView countView;
    private TextView cardView;
    private Spinner orderSpinner;
    private Switch frontSwitch;

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

        progressView = findViewById(R.id.flashProgress);
        countView = findViewById(R.id.flashCounts);
        cardView = findViewById(R.id.flashCard);
        orderSpinner = findViewById(R.id.flashOrderSpinner);
        frontSwitch = findViewById(R.id.frontSideSwitch);
        findViewById(R.id.flashBackButton).setOnClickListener(v -> finish());

        String[] orders = new String[]{"단어장 순서", "무작위", "알파벳순"};
        orderSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, orders));
        orderSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (first) { first = false; return; }
                new AlertDialog.Builder(FlashcardActivity.this)
                        .setTitle("학습 순서 변경")
                        .setMessage("현재 회차를 처음부터 다시 시작합니다.")
                        .setPositiveButton("변경", (d, w) -> startNewSession())
                        .setNegativeButton("취소", (d, w) -> { })
                        .show();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        frontSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showingFront = true;
            renderCard();
        });

        cardView.setOnTouchListener((v, event) -> handleCardTouch(event));
        findViewById(R.id.notLearnedButton).setOnClickListener(v -> judge(false));
        findViewById(R.id.learnedButton).setOnClickListener(v -> judge(true));

        loadWords();
    }

    private void loadWords() {
        List<VocabItem> all = db.getWords(bookId);
        String wrongIds = getIntent().getStringExtra("wrong_ids");
        if (wrongIds != null && !wrongIds.trim().isEmpty()) {
            Set<Long> ids = new HashSet<Long>();
            for (String value : wrongIds.split(",")) {
                try { ids.add(Long.parseLong(value.trim())); } catch (Exception ignored) { }
            }
            for (VocabItem item : all) if (ids.contains(item.getId())) sourceWords.add(item);
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
        int order = orderSpinner.getSelectedItemPosition();
        if (order == 1) Collections.shuffle(roundWords);
        else if (order == 2) {
            Collections.sort(roundWords, new Comparator<VocabItem>() {
                @Override public int compare(VocabItem a, VocabItem b) {
                    return a.getTerm().toLowerCase(Locale.getDefault()).compareTo(b.getTerm().toLowerCase(Locale.getDefault()));
                }
            });
        }
        notLearned.clear();
        index = 0;
        learnedCount = 0;
        notLearnedCount = 0;
        showingFront = true;
        locked = false;
        cardView.setTranslationX(0f);
        cardView.setAlpha(1f);
        renderCard();
    }

    private boolean handleCardTouch(MotionEvent event) {
        if (locked || roundWords.isEmpty()) return true;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = event.getRawX() - downX;
            cardView.setTranslationX(dx);
            cardView.setRotation(dx / 30f);
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            float dx = event.getRawX() - downX;
            float dy = event.getRawY() - downY;
            if (Math.abs(dx) > 120) animateJudge(dx > 0);
            else {
                cardView.animate().translationX(0f).rotation(0f).setDuration(120).start();
                if (Math.abs(dx) < 20 && Math.abs(dy) < 20) flipCard();
            }
            return true;
        }
        return false;
    }

    private void animateJudge(final boolean learned) {
        if (locked) return;
        locked = true;
        float target = learned ? cardView.getWidth() * 1.2f : -cardView.getWidth() * 1.2f;
        cardView.animate().translationX(target).alpha(0f).setDuration(160).withEndAction(() -> {
            processJudge(learned);
            cardView.setTranslationX(0f);
            cardView.setRotation(0f);
            cardView.setAlpha(1f);
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
        if (learned) learnedCount++;
        else {
            notLearnedCount++;
            notLearned.add(item);
        }
        index++;
        showingFront = true;
        if (index >= roundWords.size()) finishRound();
        else {
            locked = false;
            renderCard();
        }
    }

    private void finishRound() {
        if (!notLearned.isEmpty()) {
            final int remaining = notLearned.size();
            new AlertDialog.Builder(this)
                    .setTitle("회차 완료")
                    .setMessage("아직 못 외운 단어 " + remaining + "개로 다음 회차를 시작합니다.")
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
        boolean englishFront = !frontSwitch.isChecked();
        boolean showEnglish = showingFront ? englishFront : !englishFront;
        cardView.setText(showEnglish ? item.getTerm() : item.getMeaning());
        progressView.setText("회차 " + roundNumber + " · " + (index + 1) + " / " + roundWords.size());
        countView.setText("외움 " + learnedCount + " · 못 외움 " + notLearnedCount + "\n카드를 눌러 앞뒤 전환");
    }
}
