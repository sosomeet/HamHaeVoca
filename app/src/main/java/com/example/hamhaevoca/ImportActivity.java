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

public class ImportActivity extends LocalizedActivity {
    private static final int REQUEST_FILE = 101;
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private EditText bookNameInput;
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
        selectedFileText = findViewById(R.id.selectedFileText);
        summaryText = findViewById(R.id.importSummary);
        saveButton = findViewById(R.id.saveImportButton);
        dayFilterButton = findViewById(R.id.dayFilterButton);
        useDayColumnSwitch = findViewById(R.id.useDayColumnSwitch);
        daySelectionText = findViewById(R.id.daySelectionText);


        previewContainer = findViewById(R.id.previewContainer);

        findViewById(R.id.chooseFileButton).setOnClickListener(v -> chooseFile());
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
                    .setTitle(Lang.text(ImportActivity.this, R.string.ui_cbab1c6fb7b1))
                    .setMessage(Lang.text(ImportActivity.this, R.string.ui_1be4244e7061))
                    .setPositiveButton(Lang.text(ImportActivity.this, R.string.ui_468266d639b5), (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        titleText.setText(Lang.text(ImportActivity.this, R.string.ui_fb125709e406));
        descriptionText.setText(Lang.text(ImportActivity.this, R.string.ui_4317a678b4c6)
                + Lang.text(ImportActivity.this, R.string.ui_af8de48791d7));
        bookNameInput.setText(summary.getName());
        bookNameInput.setEnabled(false);
        saveButton.setText(Lang.text(ImportActivity.this, R.string.ui_74b4b36c7b69));
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Do not restrict providers by MIME type: some file managers report
        // XLSX as application/octet-stream and would disable valid files.
        // The selected file is validated by filename in parseCurrentInput().
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILE || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        selectedUri = data.getData();
        selectedFileName = queryDisplayName(selectedUri);
        selectedFileText.setText(Lang.text(ImportActivity.this, R.string.ui_0c46f732956b) + selectedFileName);
        if (!updateMode && bookNameInput.getText().toString().trim().isEmpty()) {
            String base = selectedFileName == null ? Lang.text(ImportActivity.this, R.string.ui_d455c82a86bc)
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
        return uri.getLastPathSegment() == null ? Lang.text(ImportActivity.this, R.string.ui_7fb5dc26bfee) : uri.getLastPathSegment();
    }

    private void parseCurrentInput() {
        try {
            if (selectedUri != null) {
                String lower = selectedFileName == null ? ""
                        : selectedFileName.toLowerCase(Locale.US);
                if (lower.endsWith(".xlsx")) {
                    InputStream input = getContentResolver().openInputStream(selectedUri);
                    if (input == null) throw new IllegalArgumentException(Lang.text(ImportActivity.this, R.string.ui_bbba049b6423));
                    try {
                        currentResult = XlsxParser.parseFirstSheet(input);
                    } finally {
                        input.close();
                    }
                } else if (lower.endsWith(".xls")) {
                    throw new IllegalArgumentException(
                            Lang.text(ImportActivity.this, R.string.ui_c7ed66e49813));
                } else if (lower.endsWith(".csv") || lower.endsWith(".tsv")
                        || lower.endsWith(".txt")) {
                    byte[] bytes = readBytes(selectedUri);
                    String text = new String(bytes, Charset.forName("UTF-8"));
                    currentResult = DelimitedTextParser.parse(text,
                            "자동");
                } else {
                    throw new IllegalArgumentException(
                            Lang.text(ImportActivity.this, R.string.ui_265b91ab8712));
                }
            } else {
                throw new IllegalArgumentException(getString(R.string.ui_choose_file_first));
            }
            showResult();
        } catch (Exception e) {
            currentResult = null;
            previewContainer.removeAllViews();
            summaryText.setText(Lang.text(ImportActivity.this, R.string.ui_b1272002cee0) + localizedParseError(safeMessage(e)));
            availableDays.clear();
            selectedDays.clear();
            updateDaySelectionControls();
            saveButton.setEnabled(false);
        }
    }

    private byte[] readBytes(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) throw new IllegalArgumentException(Lang.text(ImportActivity.this, R.string.ui_bbba049b6423));
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException(Lang.text(ImportActivity.this, R.string.ui_28c11c30d6be));
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
        // Display a contiguous, 1-based preview index, not the source Excel row number.
        int previewIndex = 1;
        for (ParsedRow row : currentResult.getRows()) {
            View rowView = LayoutInflater.from(this).inflate(
                    R.layout.row_preview, previewContainer, false);
            ((TextView) rowView.findViewById(R.id.previewRowNumber))
                    .setText(String.valueOf(previewIndex++));
            ((TextView) rowView.findViewById(R.id.previewTerm)).setText(row.getPast().isEmpty() ? row.getTerm() : row.getTerm() + " → " + row.getPast() + " → " + row.getPastParticiple());
            ((TextView) rowView.findViewById(R.id.previewMeaning)).setText(row.getMeaning());
            TextView exampleView = rowView.findViewById(R.id.previewExample);
            if (row.getExample() != null && !row.getExample().trim().isEmpty()) {
                exampleView.setText(row.getExample());
                exampleView.setVisibility(View.VISIBLE);
            } else {
                exampleView.setVisibility(View.GONE);
            }
            TextView dayView = rowView.findViewById(R.id.previewDay);
            dayView.setText(row.getDay().isEmpty() ? Lang.text(ImportActivity.this, R.string.ui_2f6183a4a2f5) : "Day " + row.getDay());

            TextView status = rowView.findViewById(R.id.previewStatus);
            if (!row.isValid()) {
                status.setText(Lang.text(ImportActivity.this, R.string.ui_8c7db7e963b9) + localizedParseError(row.getError()));
            } else if (row.isDuplicate()) {
                status.setText(Lang.text(ImportActivity.this, R.string.ui_fd0931151952));
            } else if (!isRowSelectedByDay(row)) {
                status.setText(Lang.text(ImportActivity.this, R.string.ui_37ae72b4911c));
            } else {
                status.setVisibility(View.GONE);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0,
                    getResources().getDimensionPixelSize(R.dimen.list_item_gap));
            rowView.setLayoutParams(params);
            previewContainer.addView(rowView);
        }

        int selectedCount = currentResult.countValidRowsForDays(selectedDaysForFiltering());
        summaryText.setText(Lang.text(ImportActivity.this, R.string.ui_57ff445a7f6b) + currentResult.getTotalRows()
                + Lang.text(ImportActivity.this, R.string.ui_97870453ac08) + selectedCount
                + Lang.text(ImportActivity.this, R.string.ui_9911597136a1) + currentResult.getInvalidRows()
                + Lang.text(ImportActivity.this, R.string.ui_e78e608baae9) + currentResult.getDuplicateRows() + Lang.text(ImportActivity.this, R.string.ui_91c4aeab21f8));
        saveButton.setEnabled(selectedCount > 0);
    }

    private String localizedParseError(String message) {
        if ("단어 또는 뜻이 비어 있습니다.".equals(message))
            return getString(R.string.ui_2f39f3f804c0);
        if ("중복 항목입니다.".equals(message))
            return getString(R.string.ui_bbb01346d71c);
        if ("Excel 시트를 찾을 수 없습니다.".equals(message))
            return getString(R.string.error_no_excel_sheet);
        if ("Excel 시트 데이터가 없습니다.".equals(message))
            return getString(R.string.error_excel_empty);
        return message == null ? "" : message;
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
            daySelectionText.setVisibility(View.GONE);
            dayFilterButton.setVisibility(View.GONE);
            dayFilterButton.setEnabled(false);
            dayFilterButton.setText(Lang.text(ImportActivity.this, R.string.ui_d10eb6b36e51));
            daySelectionText.setText(Lang.text(ImportActivity.this, R.string.ui_e418368bc7dc));
            return;
        }
        daySelectionText.setVisibility(View.VISIBLE);
        dayFilterButton.setVisibility(View.VISIBLE);
        if (availableDays.isEmpty()) {
            dayFilterButton.setEnabled(false);
            dayFilterButton.setText(Lang.text(ImportActivity.this, R.string.ui_2f6183a4a2f5));
            daySelectionText.setText(Lang.text(ImportActivity.this, R.string.ui_86fcef5275e5));
            return;
        }
        dayFilterButton.setEnabled(true);
        if (selectedDays.size() == availableDays.size()) {
            dayFilterButton.setText(Lang.text(ImportActivity.this, R.string.ui_65a4ffcb1f65));
            daySelectionText.setText(Lang.text(ImportActivity.this, R.string.ui_718ab5ddb7fd));
        } else {
            StringBuilder label = new StringBuilder();
            for (String day : selectedDays) {
                if (label.length() > 0) label.append(", ");
                label.append(day);
            }
            dayFilterButton.setText(Lang.text(ImportActivity.this, R.string.ui_db1f92e4c563) + label);
            daySelectionText.setText("Day " + label + Lang.text(ImportActivity.this, R.string.ui_862edc0d53af));
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
                .setTitle(Lang.text(ImportActivity.this, R.string.ui_79852cd300bf))
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> {
                    String day = availableDays.get(which);
                    if (isChecked) pending.add(day);
                    else pending.remove(day);
                })
                .setPositiveButton(Lang.text(ImportActivity.this, R.string.ui_e01dd02f527f), null)
                .setNeutralButton(Lang.text(ImportActivity.this, R.string.ui_934dd25ec5f7), (d, w) -> {
                    selectedDays.clear();
                    selectedDays.addAll(availableDays);
                    updateDaySelectionControls();
                    renderPreviewAndSummary();
                })
                .setNegativeButton(Lang.text(ImportActivity.this, R.string.ui_19b2d19bc141), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (pending.isEmpty()) {
                        Toast.makeText(this, Lang.text(ImportActivity.this, R.string.ui_ed67f1e0b572),
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
            bookNameInput.setError(Lang.text(ImportActivity.this, R.string.ui_8718a80eddfb));
            return;
        }
        if (currentResult == null || currentResult.countValidRowsForDays(selectedDaysForFiltering()) == 0) {
            Toast.makeText(this, Lang.text(ImportActivity.this, R.string.ui_7a9a46dcce0b), Toast.LENGTH_SHORT).show();
            return;
        }
        saveButton.setEnabled(false);
        try {
            String source = currentResult.isVerbFormat() ? "verb" : (selectedUri == null ? "text" : sourceType(selectedFileName));
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            if (updateMode) {
                MergeResult result = db.mergeBookWords(updateBookId, source,
                        currentResult.toVocabItems(selectedDaysForFiltering()));
                Toast.makeText(this, getString(R.string.merge_summary, result.getAddedCount(), result.getUpdatedCount(), result.getMatchedCount(), result.getKeptOldCount()), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, Lang.text(ImportActivity.this, R.string.ui_1c93b1b04454) + safeMessage(e),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String sourceType(String fileName) {
        if (fileName == null) return "file";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.US) : "file";
    }

}
