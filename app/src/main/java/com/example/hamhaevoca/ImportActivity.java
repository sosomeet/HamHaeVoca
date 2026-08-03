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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.ParseResult;
import com.example.hamhaevoca.model.ParsedRow;
import com.example.hamhaevoca.parser.DelimitedTextParser;
import com.example.hamhaevoca.parser.XlsxParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImportActivity extends Activity {
    private static final int REQUEST_FILE = 101;
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private EditText bookNameInput;
    private EditText textInput;
    private Spinner delimiterSpinner;
    private TextView selectedFileText;
    private TextView summaryText;
    private Button saveButton;
    private PreviewAdapter previewAdapter;
    private final List<ParsedRow> previewRows = new ArrayList<ParsedRow>();

    private ParseResult currentResult;
    private Uri selectedUri;
    private String selectedFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        bookNameInput = findViewById(R.id.bookNameInput);
        textInput = findViewById(R.id.importTextInput);
        delimiterSpinner = findViewById(R.id.delimiterSpinner);
        selectedFileText = findViewById(R.id.selectedFileText);
        summaryText = findViewById(R.id.importSummary);
        saveButton = findViewById(R.id.saveImportButton);

        String[] delimiters = new String[]{"자동", "탭", "쉼표", "세미콜론", "파이프"};
        delimiterSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, delimiters));

        ListView previewList = findViewById(R.id.previewList);
        previewAdapter = new PreviewAdapter();
        previewList.setAdapter(previewAdapter);

        findViewById(R.id.chooseFileButton).setOnClickListener(v -> chooseFile());
        findViewById(R.id.parseButton).setOnClickListener(v -> parseCurrentInput());
        findViewById(R.id.clearImportButton).setOnClickListener(v -> confirmClear());
        saveButton.setOnClickListener(v -> saveBook());
        saveButton.setEnabled(false);
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
        if (requestCode != REQUEST_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectedUri = data.getData();
        selectedFileName = queryDisplayName(selectedUri);
        selectedFileText.setText("선택 파일: " + selectedFileName);
        if (bookNameInput.getText().toString().trim().isEmpty()) {
            String base = selectedFileName == null ? "새 단어장" : selectedFileName.replaceFirst("\\.[^.]+$", "");
            bookNameInput.setText(base);
        }
        parseCurrentInput();
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
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
                String lower = selectedFileName == null ? "" : selectedFileName.toLowerCase(Locale.US);
                if (lower.endsWith(".xlsx")) {
                    InputStream input = getContentResolver().openInputStream(selectedUri);
                    if (input == null) throw new IllegalArgumentException("파일을 열 수 없습니다.");
                    try {
                        currentResult = XlsxParser.parseFirstSheet(input);
                    } finally {
                        input.close();
                    }
                } else if (lower.endsWith(".xls")) {
                    throw new IllegalArgumentException("이 버전은 .xls를 지원하지 않습니다. Excel에서 .xlsx로 저장해 주세요.");
                } else {
                    byte[] bytes = readBytes(selectedUri);
                    String text = new String(bytes, Charset.forName("UTF-8"));
                    currentResult = DelimitedTextParser.parse(text, delimiterSpinner.getSelectedItem().toString());
                }
            } else {
                String text = textInput.getText().toString();
                if (text.trim().isEmpty()) throw new IllegalArgumentException("내용을 붙여넣거나 파일을 선택하세요.");
                currentResult = DelimitedTextParser.parse(text, delimiterSpinner.getSelectedItem().toString());
            }
            showResult();
        } catch (Exception e) {
            currentResult = null;
            previewRows.clear();
            previewAdapter.notifyDataSetChanged();
            summaryText.setText("불러오기 실패: " + safeMessage(e));
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
                if (total > MAX_BYTES) throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }

    private void showResult() {
        previewRows.clear();
        int limit = Math.min(20, currentResult.getRows().size());
        for (int i = 0; i < limit; i++) previewRows.add(currentResult.getRows().get(i));
        previewAdapter.notifyDataSetChanged();
        summaryText.setText("전체 " + currentResult.getTotalRows() + "행 · 저장 가능 " + currentResult.getValidRows()
                + "행 · 오류 " + currentResult.getInvalidRows() + "행 · 중복 " + currentResult.getDuplicateRows() + "행");
        saveButton.setEnabled(currentResult.getValidRows() > 0);
    }

    private void saveBook() {
        String name = bookNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            bookNameInput.setError("단어장 이름을 입력하세요.");
            return;
        }
        if (currentResult == null || currentResult.getValidRows() == 0) {
            Toast.makeText(this, "저장할 유효한 단어가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveButton.setEnabled(false);
        try {
            String source = selectedUri == null ? "text" : sourceType(selectedFileName);
            long bookId = DatabaseHelper.getInstance(this).createBookWithWords(name, source, currentResult.toVocabItems());
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            saveButton.setEnabled(true);
            Toast.makeText(this, "저장하지 못했습니다: " + safeMessage(e), Toast.LENGTH_LONG).show();
        }
    }

    private String sourceType(String fileName) {
        if (fileName == null) return "file";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.US) : "file";
    }

    private void confirmClear() {
        if (textInput.getText().toString().trim().isEmpty() && selectedUri == null && currentResult == null) {
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
        previewRows.clear();
        previewAdapter.notifyDataSetChanged();
        summaryText.setText("미리보기를 생성하세요.");
        saveButton.setEnabled(false);
    }

    private class PreviewAdapter extends ArrayAdapter<ParsedRow> {
        PreviewAdapter() { super(ImportActivity.this, 0, previewRows); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(ImportActivity.this).inflate(R.layout.row_preview, parent, false);
            ParsedRow row = getItem(position);
            ((TextView) convertView.findViewById(R.id.previewRowNumber)).setText(String.valueOf(row.getRowNumber()));
            ((TextView) convertView.findViewById(R.id.previewTerm)).setText(row.getTerm());
            ((TextView) convertView.findViewById(R.id.previewMeaning)).setText(row.getMeaning());
            TextView status = convertView.findViewById(R.id.previewStatus);
            if (!row.isValid()) status.setText("오류: " + row.getError());
            else if (row.isDuplicate()) status.setText("중복 제외");
            else status.setText("저장 가능");
            return convertView;
        }
    }
}
