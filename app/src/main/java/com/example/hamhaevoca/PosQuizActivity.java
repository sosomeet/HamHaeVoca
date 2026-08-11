package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.QuizAnswerData;
import com.example.hamhaevoca.model.VocabItem;
import com.example.hamhaevoca.util.PosTagParser;
import com.example.hamhaevoca.util.QuizEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 한 단어에 해당하는 품사를 8품사 중 복수 선택하는 시험 화면.
 * 정답은 뜻 문자열에 포함된 (n), (v), (adj) 등의 태그 집합과 정확히 일치해야 한다.
 */
public class PosQuizActivity extends Activity {
    private final Button[] tagButtons = new Button[8];
    private final String[] tagKeys = new String[]{
            PosTagParser.NOUN,
            PosTagParser.PRONOUN,
            PosTagParser.VERB,
            PosTagParser.ADJECTIVE,
            PosTagParser.ADVERB,
            PosTagParser.PREPOSITION,
            PosTagParser.CONJUNCTION,
            PosTagParser.INTERJECTION
    };

    private long bookId;
    private int timeLimit;
    private ArrayList<VocabItem> questions;
    private final ArrayList<QuizAnswerData> answers = new ArrayList<QuizAnswerData>();
    private final LinkedHashSet<String> selectedTags = new LinkedHashSet<String>();

    private TextView progressText;
    private TextView timerText;
    private TextView termText;
    private TextView meaningText;
    private ProgressBar progressBar;
    private Button submitButton;

    private int index = 0;
    private boolean answered = false;
    private CountDownTimer timer;
    private long questionStartedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos_quiz);

        bookId = getIntent().getLongExtra("book_id", -1);
        timeLimit = getIntent().getIntExtra("time_limit", 5);
        int maximumWords = getIntent().getIntExtra("max_words", 0);

        List<VocabItem> all = DatabaseHelper.getInstance(this).getWords(bookId);
        questions = PosTagParser.filterTaggedItems(all);
        Collections.shuffle(questions);
        if (maximumWords > 0 && questions.size() > maximumWords) {
            questions = new ArrayList<VocabItem>(questions.subList(0, maximumWords));
        }

        progressText = findViewById(R.id.posQuizProgressText);
        timerText = findViewById(R.id.posQuizTimerText);
        termText = findViewById(R.id.posQuizTermText);
        meaningText = findViewById(R.id.posQuizMeaningText);
        progressBar = findViewById(R.id.posQuizProgressBar);
        submitButton = findViewById(R.id.posQuizSubmitButton);

        tagButtons[0] = findViewById(R.id.posNounButton);
        tagButtons[1] = findViewById(R.id.posPronounButton);
        tagButtons[2] = findViewById(R.id.posVerbButton);
        tagButtons[3] = findViewById(R.id.posAdjectiveButton);
        tagButtons[4] = findViewById(R.id.posAdverbButton);
        tagButtons[5] = findViewById(R.id.posPrepositionButton);
        tagButtons[6] = findViewById(R.id.posConjunctionButton);
        tagButtons[7] = findViewById(R.id.posInterjectionButton);

        for (int i = 0; i < tagButtons.length; i++) {
            final int buttonIndex = i;
            tagButtons[i].setText(PosTagParser.displayName(tagKeys[i]));
            tagButtons[i].setOnClickListener(v -> toggleTag(buttonIndex));
        }

        findViewById(R.id.posQuizBackButton).setOnClickListener(v -> confirmExit());
        submitButton.setOnClickListener(v -> submitAnswer(false));

        if (questions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("품사 문제가 없음")
                    .setMessage("뜻 앞에 (n), (v), (adj) 같은 지원 품사 태그가 있는 단어가 없습니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        progressBar.setMax(questions.size());
        showQuestion();
    }

    private void showQuestion() {
        if (index >= questions.size()) {
            finishQuiz();
            return;
        }
        answered = false;
        selectedTags.clear();
        VocabItem item = questions.get(index);

        progressText.setText((index + 1) + " / " + questions.size());
        progressBar.setProgress(index + 1);
        termText.setText(item.getTerm());
        meaningText.setText(PosTagParser.stripTags(item.getMeaning()));
        submitButton.setEnabled(true);
        submitButton.setAlpha(1f);

        for (int i = 0; i < tagButtons.length; i++) {
            tagButtons[i].setEnabled(true);
            renderSelection(i);
        }

        questionStartedAt = System.currentTimeMillis();
        startTimer(timeLimit * 1000L);
    }

    private void toggleTag(int buttonIndex) {
        if (answered) return;
        String tag = tagKeys[buttonIndex];
        if (selectedTags.contains(tag)) selectedTags.remove(tag);
        else selectedTags.add(tag);
        renderSelection(buttonIndex);
    }

    private void renderSelection(int buttonIndex) {
        Button button = tagButtons[buttonIndex];
        boolean selected = selectedTags.contains(tagKeys[buttonIndex]);
        button.setBackgroundResource(selected
                ? R.drawable.bg_choice_selected : R.drawable.bg_choice_default);
        button.setTextColor(getResources().getColor(R.color.text_primary));
    }

    private void submitAnswer(boolean timedOut) {
        if (answered) return;
        if (!timedOut && selectedTags.isEmpty()) {
            Toast.makeText(this, "품사를 하나 이상 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        answered = true;
        cancelTimer();

        VocabItem item = questions.get(index);
        LinkedHashSet<String> correctTags = PosTagParser.parseTags(item.getMeaning());
        LinkedHashSet<String> answerSnapshot = new LinkedHashSet<String>(selectedTags);
        boolean correct = !timedOut && correctTags.equals(answerSnapshot);
        long responseTime = System.currentTimeMillis() - questionStartedAt;

        answers.add(new QuizAnswerData(
                item.getId(),
                QuizEngine.PART_OF_SPEECH,
                item.getTerm(),
                item.getMeaning(),
                item.getTerm(),
                timedOut ? null : PosTagParser.compactLabel(answerSnapshot),
                PosTagParser.compactLabel(correctTags),
                correct,
                timedOut,
                responseTime));

        submitButton.setEnabled(false);
        submitButton.setAlpha(0.45f);
        for (int i = 0; i < tagButtons.length; i++) {
            String tag = tagKeys[i];
            tagButtons[i].setEnabled(false);
            if (correctTags.contains(tag)) {
                tagButtons[i].setBackgroundResource(R.drawable.bg_choice_correct);
            } else if (answerSnapshot.contains(tag)) {
                tagButtons[i].setBackgroundResource(R.drawable.bg_choice_wrong);
            } else {
                tagButtons[i].setBackgroundResource(R.drawable.bg_choice_default);
            }
            tagButtons[i].setTextColor(getResources().getColor(R.color.text_primary));
        }

        handler.postDelayed(() -> {
            index++;
            showQuestion();
        }, 800);
    }

    private void startTimer(long duration) {
        cancelTimer();
        timer = new CountDownTimer(duration, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                timerText.setText(seconds + "초");
            }

            @Override
            public void onFinish() {
                timerText.setText("0초");
                submitAnswer(true);
            }
        }.start();
    }

    private void finishQuiz() {
        cancelTimer();
        long attemptId = DatabaseHelper.getInstance(this).saveQuizAttempt(
                bookId, QuizEngine.PART_OF_SPEECH, timeLimit, answers);
        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("attempt_id", attemptId);
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("품사 시험 종료")
                .setMessage("현재 시험 기록은 저장되지 않습니다. 나갈까요?")
                .setPositiveButton("나가기", (d, w) -> finish())
                .setNegativeButton("계속", null)
                .show();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
