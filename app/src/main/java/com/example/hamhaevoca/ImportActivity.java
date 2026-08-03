package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;
import com.example.hamhaevoca.model.MergeResult;
import com.example.hamhaevoca.model.ParseResult;
import com.example.hamhaevoca.model.ParsedRow;
import com.example.hamhaevoca.parser.DelimitedTextParser;
import com.example.hamhaevoca.parser.XlsxParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ImportActivity extends Activity {
    private static final int REQUEST_FILE = 101;
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private EditText bookNameInput;
    private EditText textInput;
    private Spinner delimiterSpinner;
    private TextView selectedFileText;
    private TextView summaryText;
    private TextView titleText;
    private TextView descriptionText;
    private Button saveButton;
    private Button dayFilterButton;
    private Switch useDayColumnSwitch;
    private TextView daySelectionText;
    private LinearLayout previewContainer;

    private final List<String> availableDays = new ArrayList<String>();
    private final LinkedHashSet<String> selectedDays = new LinkedHashSet<String>();

    private ParseResult currentResult;
    private Uri selectedUri;
    private String selectedFileName;
    private long updateBookId = -1;
    private boolean updateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        updateBookId = getIntent().getLongExtra("update_book_id", -1);
        updateMode = updateBookId > 0;

        titleText = findViewById(R.id.importTitle);
        descriptionText = findViewById(R.id.importDescription);
        bookNameInput = findViewById(R.id.bookNameInput);
        textInput = findViewById(R.id.importTextInput);
        delimiterSpinner = findViewById(R.id.delimiterSpinner);
        selectedFileText = findViewById(R.id.selectedFileText);
        summaryText = findViewById(R.id.importSummary);
        saveButton = findViewById(R.id.saveImportButton);
        dayFilterButton = findViewById(R.id.dayFilterButton);
        useDayColumnSwitch = findViewById(R.id.useDayColumnSwitch);
        daySelectionText = findViewById(R.id.daySelectionText);

        String[] delimiters = new String[]{"자동", "탭", "쉼표", "세미콜론", "파이프"};
        delimiterSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, delimiters));

        previewContainer = findViewById(R.id.previewContainer);

        findViewById(R.id.chooseFileButton).setOnClickListener(v -> chooseFile());
        findViewById(R.id.parseButton).setOnClickListener(v -> parseCurrentInput());
        findViewById(R.id.clearImportButton).setOnClickListener(v -> confirmClear());
        dayFilterButton.setOnClickListener(v -> showDaySelectionDialog());
        useDayColumnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDaySelectionControls();
            if (currentResult != null) renderPreviewAndSummary();
        });
        saveButton.setOnClickListener(v -> saveBook());
        saveButton.setEnabled(false);
        dayFilterButton.setEnabled(false);

        configureMode();
    }

    private void configureMode() {
        if (!updateMode) return;
        BookSummary summary = DatabaseHelper.getInstance(this).getBookSummary(updateBookId);
        if (summary == null) {
            new AlertDialog.Builder(this)
                    .setTitle("단어장을 찾을 수 없음")
                    .setMessage("갱신하려는 단어장이 삭제되었거나 존재하지 않습니다.")
                    .setPositiveButton("확인", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        titleText.setText("단어장 갱신");
        descriptionText.setText("같은 단어의 암기·정답·오답 기록은 유지합니다. "
                + "새 단어는 추가하고, 파일에 없는 기존 단어는 삭제하지 않습니다.");
        bookNameInput.setText(summary.getName());
        bookNameInput.setEnabled(false);
        saveButton.setText("기록을 유지하고 갱신");
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/plain", "text/csv", "text/tab-separated-values",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        });
        startActivityForResult(intent, REQUEST_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILE || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        selectedUri = data.getData();
        selectedFileName = queryDisplayName(selectedUri);
        selectedFileText.setText("선택 파일: " + selectedFileName);
        if (!updateMode && bookNameInput.getText().toString().trim().isEmpty()) {
            String base = selectedFileName == null ? "새 단어장"
                    : selectedFileName.replaceFirst("\\.[^.]+$", "");
            bookNameInput.setText(base);
        }
        parseCurrentInput();
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) return cursor.getString(0);
            } finally {
                cursor.close();
            }
        }
        return uri.getLastPathSegment() == null ? "선택한 파일" : uri.getLastPathSegment();
    }

    private void parseCurrentInput() {
        try {
            if (selectedUri != null) {
                String lower = selectedFileName == null ? ""
                        : selectedFileName.toLowerCase(Locale.US);
                if (lower.endsWith(".xlsx")) {
                    InputStream input = getContentResolver().openInputStream(selectedUri);
                    if (input == null) throw new IllegalArgumentException("파일을 열 수 없습니다.");
                    try {
                        currentResult = XlsxParser.parseFirstSheet(input);
                    } finally {
                        input.close();
                    }
                } else if (lower.endsWith(".xls")) {
                    throw new IllegalArgumentException(
                            "이 버전은 .xls를 지원하지 않습니다. Excel에서 .xlsx로 저장해 주세요.");
                } else {
                    byte[] bytes = readBytes(selectedUri);
                    String text = new String(bytes, Charset.forName("UTF-8"));
                    currentResult = DelimitedTextParser.parse(text,
                            delimiterSpinner.getSelectedItem().toString());
                }
            } else {
                String text = textInput.getText().toString();
                if (text.trim().isEmpty()) {
                    throw new IllegalArgumentException("내용을 붙여넣거나 파일을 선택하세요.");
                }
                currentResult = DelimitedTextParser.parse(text,
                        delimiterSpinner.getSelectedItem().toString());
            }
            showResult();
        } catch (Exception e) {
            currentResult = null;
            previewContainer.removeAllViews();
            summaryText.setText("불러오기 실패: " + safeMessage(e));
            availableDays.clear();
            selectedDays.clear();
            updateDaySelectionControls();
            saveButton.setEnabled(false);
        }
    }

    private byte[] readBytes(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) throw new IllegalArgumentException("파일을 열 수 없습니다.");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
                ? e.getClass().getSimpleName() : message;
    }

    private void showResult() {
        availableDays.clear();
        availableDays.addAll(currentResult.getAvailableDays());
        selectedDays.clear();
        selectedDays.addAll(availableDays);
        updateDaySelectionControls();
        renderPreviewAndSummary();
    }

    private void renderPreviewAndSummary() {
        previewContainer.removeAllViews();
        for (ParsedRow row : currentResult.getRows()) {
            View rowView = LayoutInflater.from(this).inflate(
                    R.layout.row_preview, previewContainer, false);
            ((TextView) rowView.findViewById(R.id.previewRowNumber))
                    .setText(String.valueOf(row.getRowNumber()));
            ((TextView) rowView.findViewById(R.id.previewTerm)).setText(row.getTerm());
            ((TextView) rowView.findViewById(R.id.previewMeaning)).setText(row.getMeaning());
            TextView dayView = rowView.findViewById(R.id.previewDay);
            dayView.setText(row.getDay().isEmpty() ? "Day 정보 없음" : "Day " + row.getDay());

            TextView status = rowView.findViewById(R.id.previewStatus);
            if (!row.isValid()) {
                status.setText("오류: " + row.getError());
            } else if (row.isDuplicate()) {
                status.setText("중복 제외");
            } else if (!isRowSelectedByDay(row)) {
                status.setText("선택한 Day에 포함되지 않아 제외");
            } else {
                status.setText("저장 가능");
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0,
                    getResources().getDimensionPixelSize(R.dimen.list_item_gap));
            rowView.setLayoutParams(params);
            previewContainer.addView(rowView);
        }

        int selectedCount = currentResult.countValidRowsForDays(selectedDaysForFiltering());
        summaryText.setText("전체 " + currentResult.getTotalRows()
                + "행 · 선택 저장 " + selectedCount
                + "행 · 오류 " + currentResult.getInvalidRows()
                + "행 · 중복 " + currentResult.getDuplicateRows() + "행");
        saveButton.setEnabled(selectedCount > 0);
    }

    private boolean isRowSelectedByDay(ParsedRow row) {
        if (!useDayColumnSwitch.isChecked()) return true;
        if (availableDays.isEmpty() || selectedDays.size() == availableDays.size()) return true;
        return !row.getDay().isEmpty() && selectedDays.contains(row.getDay());
    }

    private Set<String> selectedDaysForFiltering() {
        if (!useDayColumnSwitch.isChecked()) return null;
        if (availableDays.isEmpty() || selectedDays.size() == availableDays.size()) return null;
        return selectedDays;
    }

    private void updateDaySelectionControls() {
        if (!useDayColumnSwitch.isChecked()) {
            dayFilterButton.setEnabled(false);
            dayFilterButton.setText("Day 선택 사용 안 함");
            daySelectionText.setText("세 번째 열을 Day로 해석하지 않고 모든 단어를 저장합니다.");
            return;
        }
        if (availableDays.isEmpty()) {
            dayFilterButton.setEnabled(false);
            dayFilterButton.setText("Day 정보 없음");
            daySelectionText.setText("세 번째 열에 Day 정보가 없어 모든 단어를 저장합니다.");
            return;
        }
        dayFilterButton.setEnabled(true);
        if (selectedDays.size() == availableDays.size()) {
            dayFilterButton.setText("Day 선택: 전체");
            daySelectionText.setText("세 번째 열의 Day를 선택할 수 있습니다. 현재는 모든 Day를 저장합니다.");
        } else {
            StringBuilder label = new StringBuilder();
            for (String day : selectedDays) {
                if (label.length() > 0) label.append(", ");
                label.append(day);
            }
            dayFilterButton.setText("Day 선택: " + label);
            daySelectionText.setText("Day " + label + "에 해당하는 단어만 저장합니다.");
        }
    }

    private void showDaySelectionDialog() {
        if (!useDayColumnSwitch.isChecked() || availableDays.isEmpty()) return;
        String[] labels = new String[availableDays.size()];
        boolean[] checked = new boolean[availableDays.size()];
        final LinkedHashSet<String> pending = new LinkedHashSet<String>(selectedDays);
        for (int i = 0; i < availableDays.size(); i++) {
            labels[i] = "Day " + availableDays.get(i);
            checked[i] = pending.contains(availableDays.get(i));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("저장할 Day 선택")
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> {
                    String day = availableDays.get(which);
                    if (isChecked) pending.add(day);
                    else pending.remove(day);
                })
                .setPositiveButton("적용", null)
                .setNeutralButton("전체", (d, w) -> {
                    selectedDays.clear();
                    selectedDays.addAll(availableDays);
                    updateDaySelectionControls();
                    renderPreviewAndSummary();
                })
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (pending.isEmpty()) {
                        Toast.makeText(this, "하나 이상의 Day를 선택하세요.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedDays.clear();
                    selectedDays.addAll(pending);
                    dialog.dismiss();
                    updateDaySelectionControls();
                    renderPreviewAndSummary();
                }));
        dialog.show();
    }

    private void saveBook() {
        String name = bookNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            bookNameInput.setError("단어장 이름을 입력하세요.");
            return;
        }
        if (currentResult == null || currentResult.countValidRowsForDays(selectedDaysForFiltering()) == 0) {
            Toast.makeText(this, "저장할 유효한 단어가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveButton.setEnabled(false);
        try {
            String source = selectedUri == null ? "text" : sourceType(selectedFileName);
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            if (updateMode) {
                MergeResult result = db.mergeBookWords(updateBookId, source,
                        currentResult.toVocabItems(selectedDaysForFiltering()));
                Toast.makeText(this, result.toUserMessage(), Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                long bookId = db.createBookWithWords(name, source,
                        currentResult.toVocabItems(selectedDaysForFiltering()));
                Intent intent = new Intent(this, BookDetailActivity.class);
                intent.putExtra("book_id", bookId);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            saveButton.setEnabled(true);
            Toast.makeText(this, "저장하지 못했습니다: " + safeMessage(e),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String sourceType(String fileName) {
        if (fileName == null) return "file";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.US) : "file";
    }

    private void confirmClear() {
        if (textInput.getText().toString().trim().isEmpty()
                && selectedUri == null && currentResult == null) {
            clearAll();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("입력 초기화")
                .setMessage("현재 입력과 미리보기를 모두 지울까요?")
                .setPositiveButton("초기화", (dialog, which) -> clearAll())
                .setNegativeButton("취소", null)
                .show();
    }

    private void clearAll() {
        selectedUri = null;
        selectedFileName = null;
        selectedFileText.setText("선택한 파일 없음");
        textInput.setText("");
        currentResult = null;
        availableDays.clear();
        selectedDays.clear();
        updateDaySelectionControls();
        previewContainer.removeAllViews();
        summaryText.setText("미리보기를 생성하세요.");
        saveButton.setEnabled(false);
    }

}
