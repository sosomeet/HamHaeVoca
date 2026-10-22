package com.example.hamhaevoca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.view.inputmethod.InputMethodManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hamhaevoca.data.DatabaseHelper;
import com.example.hamhaevoca.model.BookSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.FrameLayout;
import android.graphics.drawable.ColorDrawable;

public class MainActivity extends LocalizedActivity {
    private DatabaseHelper db;
    private BookAdapter adapter;
    private final List<BookSummary> books = new ArrayList<BookSummary>();
    private TextView emptyView;
    private TextView bookSectionTitle;
    private EditText searchInput;
    private View searchContainer;
    private final List<BookSummary> allBooks = new ArrayList<>();
    private String searchQuery = "";
    private Long activeFolderId = null;
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private boolean selectionMode = false;
    private int sortMode = 0; // 0 latest, 1 oldest, 2 name ascending, 3 name descending
    private ListView bookList;
    private View actionBar;
    private TextView selectionLabel;
    private String lang = "ko";
    private View drawerLayer;

    private static final String[] LANG_CODES = {"ko", "en", "fr", "es", "vi", "ja", "zh"};
    private static final String[][] WORDS = {
        {"내 단어장", "모든 단어장", "기본", "새 폴더", "폴더 관리", "정렬", "언어", "이름순 (가나다)", "이름순 (역순)", "최근 수정순", "오래된 수정순", "선택", "폴더로 이동", "선택 취소", "단어장을 선택하세요", "단어장", "이름 변경", "삭제", "저장", "취소", "이미 존재하는 폴더입니다.", "폴더 이름을 입력하세요.", "새 단어장", "단어장 검색", "오늘도 가볍게 단어를 익혀보세요", "전체", "외움", "미암기", "수정", "단어장 열기", "폴더 변경", "삭제됨.", "아직 저장된 단어장이 없어요.", "검색 조건에 맞는 단어장이 없습니다.", "단어장을 선택한 후 이동할 폴더를 선택하세요.", "이동 완료", "폴더를 삭제해도 단어장과 학습 기록은 유지됩니다.", "폴더 삭제", "단어장 이름 변경", "폴더 이름 변경", "설정"},
        {"My wordbooks", "All wordbooks", "Default", "New folder", "Manage folders", "Sort", "Language", "Name (A–Z)", "Name (Z–A)", "Recently modified", "Oldest modified", "Select", "Move to folder", "Cancel selection", "Select wordbooks", "Wordbook", "Rename", "Delete", "Save", "Cancel", "Folder already exists.", "Enter a folder name.", "New wordbook", "Search wordbooks", "Learn words a little every day", "Total", "Known", "To learn", "Modified", "Open wordbook", "Change folder", "Deleted.", "No saved wordbooks yet.", "No matching wordbooks.", "Select wordbooks and then choose a folder.", "Moved successfully", "Deleting a folder keeps its wordbooks and progress.", "Delete folder", "Rename wordbook", "Rename folder", "Settings"},
        {"Mes listes", "Toutes les listes", "Par défaut", "Nouveau dossier", "Gérer les dossiers", "Trier", "Langue", "Nom (A–Z)", "Nom (Z–A)", "Modification récente", "Modification ancienne", "Sélectionner", "Déplacer vers un dossier", "Annuler la sélection", "Sélectionnez des listes", "Liste", "Renommer", "Supprimer", "Enregistrer", "Annuler", "Ce dossier existe déjà.", "Saisissez un nom de dossier.", "Nouvelle liste", "Rechercher", "Apprenez quelques mots chaque jour", "Total", "Acquis", "À apprendre", "Modifié", "Ouvrir", "Changer de dossier", "Supprimé.", "Aucune liste enregistrée.", "Aucun résultat.", "Sélectionnez les listes à déplacer.", "Déplacement terminé", "Supprimer un dossier conserve les listes et leur progression.", "Supprimer le dossier", "Renommer la liste", "Renommer le dossier", "Paramètres"},
        {"Mis listas", "Todas las listas", "Predeterminado", "Nueva carpeta", "Administrar carpetas", "Ordenar", "Idioma", "Nombre (A–Z)", "Nombre (Z–A)", "Modificación reciente", "Modificación antigua", "Seleccionar", "Mover a carpeta", "Cancelar selección", "Selecciona listas", "Lista", "Renombrar", "Eliminar", "Guardar", "Cancelar", "La carpeta ya existe.", "Introduce un nombre de carpeta.", "Nueva lista", "Buscar listas", "Aprende palabras cada día", "Total", "Aprendidas", "Pendientes", "Modificado", "Abrir lista", "Cambiar carpeta", "Eliminado.", "No hay listas guardadas.", "No se encontraron listas.", "Selecciona las listas para moverlas.", "Movimiento completado", "Eliminar la carpeta conserva las listas y el progreso.", "Eliminar carpeta", "Renombrar lista", "Renombrar carpeta", "Ajustes"},
        {"Sổ từ vựng", "Tất cả sổ từ", "Mặc định", "Thư mục mới", "Quản lý thư mục", "Sắp xếp", "Ngôn ngữ", "Tên (A–Z)", "Tên (Z–A)", "Mới sửa gần đây", "Sửa lâu nhất", "Chọn", "Chuyển vào thư mục", "Hủy chọn", "Chọn sổ từ", "Sổ từ", "Đổi tên", "Xóa", "Lưu", "Hủy", "Thư mục đã tồn tại.", "Nhập tên thư mục.", "Sổ từ mới", "Tìm sổ từ", "Học vài từ mỗi ngày", "Tổng", "Đã thuộc", "Chưa thuộc", "Đã sửa", "Mở sổ từ", "Đổi thư mục", "Đã xóa.", "Chưa có sổ từ nào.", "Không có kết quả.", "Chọn sổ từ để di chuyển.", "Đã chuyển", "Xóa thư mục vẫn giữ sổ từ và tiến độ.", "Xóa thư mục", "Đổi tên sổ từ", "Đổi tên thư mục", "Cài đặt"}
        ,{"単語帳", "すべての単語帳", "デフォルト", "新しいフォルダー", "フォルダー管理", "並べ替え", "言語", "名前（昇順）", "名前（降順）", "更新日時（新しい順）", "更新日時（古い順）", "選択", "フォルダーに移動", "選択を解除", "単語帳を選択", "単語帳", "名前を変更", "削除", "保存", "キャンセル", "同じ名前のフォルダーがあります。", "フォルダー名を入力してください。", "新しい単語帳", "単語帳を検索", "今日も少しずつ単語を覚えましょう", "合計", "暗記済み", "未暗記", "更新", "単語帳を開く", "フォルダーを変更", "削除しました。", "保存された単語帳はありません。", "一致する単語帳はありません。", "移動する単語帳を選択してください。", "移動しました", "フォルダーを削除しても単語帳と学習履歴は残ります。", "フォルダーを削除", "単語帳名を変更", "フォルダー名を変更", "設定"},
        {"词汇本", "所有词汇本", "默认", "新建文件夹", "管理文件夹", "排序", "语言", "名称（升序）", "名称（降序）", "最近修改", "最早修改", "选择", "移至文件夹", "取消选择", "请选择词汇本", "词汇本", "重命名", "删除", "保存", "取消", "文件夹已存在。", "请输入文件夹名称。", "新建词汇本", "搜索词汇本", "每天轻松学几个单词", "总计", "已掌握", "未掌握", "修改于", "打开词汇本", "更改文件夹", "已删除。", "还没有保存的词汇本。", "没有匹配的词汇本。", "请先选择要移动的词汇本。", "移动成功", "删除文件夹不会删除词汇本或学习记录。", "删除文件夹", "重命名词汇本", "重命名文件夹", "设置"}
    };
    private String t(int idx) {
        int n = 0;
        for (int i=0; i<LANG_CODES.length; i++) if (LANG_CODES[i].equals(lang)) n=i;
        return WORDS[n][idx];
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DatabaseHelper.getInstance(this);

        lang = getPreferences(MODE_PRIVATE).getString("ui_language", "ko");
        sortMode = getPreferences(MODE_PRIVATE).getInt("sort_mode", 0);
        bookList = findViewById(R.id.bookList);
        ListView listView = bookList;
        emptyView = findViewById(R.id.emptyView);
        adapter = new BookAdapter(this, books, db);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            BookSummary b = books.get(position);
            if (selectionMode) toggleSelected(b.getId()); else openBook(b.getId());
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!selectionMode) {
                selectionMode = true;
                selectedIds.clear();
            }
            toggleSelected(books.get(position).getId());
            return true;
        });

        searchInput = findViewById(R.id.bookSearchInput);
        searchContainer = findViewById(R.id.bookSearchContainer);
        ImageButton searchToggle = findViewById(R.id.bookSearchButton);
        searchToggle.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.VISIBLE) {
                searchInput.setText("");
                searchInput.clearFocus();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                searchContainer.setVisibility(View.GONE);
            } else {
                searchContainer.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
                searchInput.post(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT));
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        bookSectionTitle = findViewById(R.id.bookSectionTitle);
        findViewById(R.id.folderButton).setOnClickListener(v -> showSideMenu());
        findViewById(R.id.importButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ImportActivity.class)));
        actionBar = findViewById(R.id.selectionBar);
        selectionLabel = findViewById(R.id.selectionCount);
        findViewById(R.id.moveSelectedButton).setOnClickListener(v -> moveSelectedBooks());
        findViewById(R.id.cancelSelectionButton).setOnClickListener(v -> leaveSelectionMode());
        refreshLabels();
        if (savedInstanceState != null && savedInstanceState.getBoolean("drawer_open", false)) {
            bookList.post(this::showSideMenu);
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("drawer_open", drawerLayer != null);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        allBooks.clear();
        allBooks.addAll(db.getBookSummaries());
        updateFolderTitle();
        applyFilters();
    }

    private void applyFilters() {
        books.clear();
        for (BookSummary book : allBooks) {
            if (!book.getName().toLowerCase(Locale.ROOT).contains(searchQuery)) continue;
            Long folderId = db.getBookFolderId(book.getId());
            if (activeFolderId != null) {
                if (activeFolderId == -1L && folderId != null) continue;
                if (activeFolderId != -1L && !activeFolderId.equals(folderId)) continue;
            }
            books.add(book);
        }
        switch (sortMode) {
            case 1: Collections.sort(books, (a,b) -> a.getUpdatedAt().compareTo(b.getUpdatedAt())); break;
            case 2: Collections.sort(books, (a,b) -> a.getName().compareToIgnoreCase(b.getName())); break;
            case 3: Collections.sort(books, (a,b) -> b.getName().compareToIgnoreCase(a.getName())); break;
            default: Collections.sort(books, (a,b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        }
        adapter.setSelection(selectedIds, selectionMode);
        adapter.notifyDataSetChanged();
        emptyView.setText(allBooks.isEmpty() ? t(32) : t(33));
        emptyView.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateFolderTitle() {
        if (activeFolderId == null) {
            bookSectionTitle.setText(t(0));
        } else if (activeFolderId == -1L) {
            bookSectionTitle.setText(t(2));
        } else {
            String folderName = null;
            for (DatabaseHelper.Folder folder : db.getFolders()) {
                if (folder.id == activeFolderId) {
                    folderName = folder.name;
                    break;
                }
            }
            if (folderName == null) {
                activeFolderId = null;
                bookSectionTitle.setText(t(0));
            } else {
                bookSectionTitle.setText(folderName);
            }
        }
    }

    private int countBooksInFolder(Long folderId) {
        int count = 0;
        for (BookSummary book : allBooks) {
            Long actual = db.getBookFolderId(book.getId());
            if (folderId == null ? actual == null : folderId.equals(actual)) count++;
        }
        return count;
    }

    private void showSideMenu() {
        if (drawerLayer != null) return;
        FrameLayout root = findViewById(android.R.id.content);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x66000000);
        overlay.setOnClickListener(v -> closeSideMenu());
        drawerLayer = overlay;
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getResources().getColor(R.color.surface));
        int width = Math.min(dp(320), (int)(getResources().getDisplayMetrics().widthPixels * .84f));
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(width, -1, Gravity.START);
        overlay.addView(scroll, drawerParams);
        scroll.setOnClickListener(v -> {});
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(18), dp(32), dp(18), dp(20));
        scroll.addView(menu);
        TextView header = new TextView(this);
        header.setText("HamHaeVoca");
        header.setTextSize(22);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setTextColor(getResources().getColor(R.color.text_primary));
        header.setPadding(dp(10), 0, 0, dp(24));
        menu.addView(header);
        addMenuItem(menu, t(1), String.valueOf(allBooks.size()), activeFolderId == null, () -> { activeFolderId=null; reload(); closeSideMenu(); });
        addMenuItem(menu, t(2), String.valueOf(countBooksInFolder(null)), Long.valueOf(-1).equals(activeFolderId), () -> { activeFolderId=-1L; reload(); closeSideMenu(); });
        for (DatabaseHelper.Folder folder : db.getFolders()) {
            addMenuItem(menu, folder.name, String.valueOf(countBooksInFolder(folder.id)), Long.valueOf(folder.id).equals(activeFolderId), () -> {
                activeFolderId=folder.id; reload(); closeSideMenu();
            });
        }
        addMenuItem(menu, t(4), null, false, () -> manageFolders());
        View line = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(1));
        lp.setMargins(0,dp(10),0,dp(10));
        line.setLayoutParams(lp); line.setBackgroundColor(getResources().getColor(R.color.border)); menu.addView(line);
        String[] sorted = {t(9), t(10), t(7), t(8)};
        String[] languages = {"한국어", "English", "Français", "Español", "Tiếng Việt", "日本語", "简体中文"};
        int languageIndex=0;
        for(int i=0;i<LANG_CODES.length;i++) if (LANG_CODES[i].equals(lang)) languageIndex=i;
        addMenuItem(menu, t(5), sorted[Math.max(0, Math.min(sortMode, 3))], false, () -> showSortDialog());
        addMenuItem(menu, t(6), languages[languageIndex], false, () -> showLanguageDialog());
        scroll.setTranslationX(-width);
        scroll.animate().translationX(0).setDuration(220).start();
    }
    private void closeSideMenu() {
        if (drawerLayer == null) return;
        View layer = drawerLayer;
        drawerLayer = null;
        layer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            if (layer.getParent() instanceof ViewGroup) ((ViewGroup)layer.getParent()).removeView(layer);
        }).start();
    }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private void addMenuItem(LinearLayout parent, String title, String value, boolean active, Runnable callback) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(14), dp(12), dp(14));
        if (active) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(getResources().getColor(R.color.border));
            bg.setCornerRadius(dp(14));
            row.setBackground(bg);
        }
        TextView label = new TextView(this);
        label.setText(title); label.setTextSize(16); label.setTextColor(getResources().getColor(R.color.text_primary));
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        if (value != null) {
            TextView detail = new TextView(this);
            if (title.equals(t(5))) detail.setTag("drawer_sort_value");
            if (title.equals(t(6))) detail.setTag("drawer_language_value");
            detail.setText(value); detail.setTextSize(13); detail.setTextColor(getResources().getColor(R.color.text_secondary));
            detail.setGravity(Gravity.END);
            row.addView(detail, new LinearLayout.LayoutParams(-2, -2));
        }
        parent.addView(row, new LinearLayout.LayoutParams(-1,-2));
        row.setOnClickListener(v -> callback.run());
    }
    private void refreshDrawerSettings() {
        if (drawerLayer == null) return;
        TextView sortValue = drawerLayer.findViewWithTag("drawer_sort_value");
        if (sortValue != null) {
            String[] choices = {t(9), t(10), t(7), t(8)};
            sortValue.setText(choices[Math.max(0, Math.min(sortMode, 3))]);
        }
    }

    private void showSortDialog() {
        String[] choices = {t(9), t(10), t(7), t(8)};
        new AlertDialog.Builder(this).setTitle(t(5)).setSingleChoiceItems(choices,sortMode,(dialog,idx)->{
            sortMode=idx;getPreferences(MODE_PRIVATE).edit().putInt("sort_mode",idx).apply();
            applyFilters(); dialog.dismiss();
            refreshDrawerSettings();
        }).setNegativeButton(t(19),null).show();
    }
    private void showLanguageDialog() {
        String[] labels = {"한국어", "English", "Français", "Español", "Tiếng Việt", "日本語", "简体中文（普通话）"};
        int current = 0; for (int i=0;i<LANG_CODES.length;i++) if (LANG_CODES[i].equals(lang)) current=i;
        new AlertDialog.Builder(this).setTitle(t(6)).setSingleChoiceItems(labels,current,(dialog,idx)->{
            lang=LANG_CODES[idx];Lang.saveLanguage(this, lang);
            dialog.dismiss(); recreate();
        }).setNegativeButton(t(19),null).show();
    }
    private void refreshLabels() {
        ((TextView)findViewById(R.id.homeSubtitle)).setText(t(24));
        ((TextView)findViewById(R.id.bookSearchInput)).setHint(t(23));
        ((TextView)findViewById(R.id.moveSelectedButton)).setText(t(12));
        ((TextView)findViewById(R.id.cancelSelectionButton)).setText(t(13));
        findViewById(R.id.importButton).setContentDescription(t(22));
        findViewById(R.id.folderButton).setContentDescription(t(40));
        updateSelectionUi();
    }
    private void toggleSelected(long id) {
        if (!selectedIds.add(id)) selectedIds.remove(id);
        updateSelectionUi(); adapter.notifyDataSetChanged();
    }
    private void updateSelectionUi() {
        if (actionBar==null) return;
        actionBar.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        selectionLabel.setText(t(11)+" ("+selectedIds.size()+")");
        findViewById(R.id.moveSelectedButton).setEnabled(!selectedIds.isEmpty());
    }
    private void leaveSelectionMode() {
        selectionMode=false; selectedIds.clear(); updateSelectionUi();adapter.notifyDataSetChanged();
    }
    private void moveSelectedBooks() {
        if (selectedIds.isEmpty()) { Toast.makeText(this,t(14),Toast.LENGTH_SHORT).show(); return; }
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] names = new String[folders.size()+1]; names[0] = t(2);
        for (int i=0;i<folders.size();i++) names[i+1]=folders.get(i).name;
        new AlertDialog.Builder(this).setTitle(t(12)).setItems(names,(d,i)->{
            Long destination=i==0?null:folders.get(i-1).id;
            for (Long id : selectedIds) db.setBookFolder(id,destination);
            leaveSelectionMode();reload();
            Toast.makeText(this,t(35),Toast.LENGTH_SHORT).show();
        }).setNegativeButton(t(19),null).show();
    }

    private void manageFolders() {
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] items = new String[folders.size() + 1];
        items[0] = "+ " + t(3);
        for (int i = 0; i < folders.size(); i++) items[i + 1] = folders.get(i).name;
        new AlertDialog.Builder(this).setTitle(t(4)).setItems(items, (d, index) -> {
            if (index == 0) { editFolder(null); return; }
            DatabaseHelper.Folder folder = folders.get(index - 1);
            new AlertDialog.Builder(this).setTitle(folder.name)
                    .setItems(new String[]{t(16), t(17)}, (dialog, action) -> {
                        if (action == 0) editFolder(folder);
                        else confirmDeleteFolder(folder);
                    }).show();
        }).show();
    }

    private void editFolder(DatabaseHelper.Folder folder) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        if (folder != null) { input.setText(folder.name); input.setSelection(input.length()); }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(folder == null ? t(3) : t(39))
                .setView(input).setPositiveButton(t(18), null)
                .setNegativeButton(t(19), null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) { input.setError(t(21)); return; }
                    try {
                        if (folder == null) db.createFolder(name);
                        else db.renameFolder(folder.id, name);
                        dialog.dismiss();
                        reload();
                        updateFolderTitle();
                    } catch (SQLiteConstraintException e) { input.setError(t(20)); }
                }));
        dialog.show();
    }

    private void confirmDeleteFolder(DatabaseHelper.Folder folder) {
        new AlertDialog.Builder(this).setTitle(t(37))
                .setMessage(t(36))
                .setPositiveButton(t(17), (d, w) -> {
                    db.deleteFolder(folder.id);
                    if (activeFolderId != null && activeFolderId == folder.id) {
                        activeFolderId = null;
                        updateFolderTitle();
                    }
                    reload();
                }).setNegativeButton(t(19), null).show();
    }

    private void changeBookFolder(BookSummary book) {
        List<DatabaseHelper.Folder> folders = db.getFolders();
        String[] items = new String[folders.size() + 1]; items[0] = t(2);
        for (int i = 0; i < folders.size(); i++) items[i + 1] = folders.get(i).name;
        new AlertDialog.Builder(this).setTitle(book.getName() + " · " + t(30))
                .setItems(items, (d, index) -> {
                    db.setBookFolder(book.getId(), index == 0 ? null : folders.get(index - 1).id);
                    reload();
                }).show();
    }

    private void openBook(long bookId) {
        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book_id", bookId);
        startActivity(intent);
    }

    private void showBookMenu(final BookSummary book) {
        final String[] actions = new String[]{t(29), t(16), t(30), t(17)};
        new AlertDialog.Builder(this)
                .setTitle(book.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openBook(book.getId());
                    else if (which == 1) renameBook(book);
                    else if (which == 2) changeBookFolder(book);
                    else confirmDelete(book);
                })
                .show();
    }

    private void renameBook(final BookSummary book) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(book.getName());
        input.setSelection(input.length());
        int p = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(p, p / 2, p, p / 2);
        final AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(t(38))
                .setView(input)
                .setPositiveButton(t(18), null)
                .setNegativeButton(t(19), null)
                .create();
        alert.setOnShowListener(dialog ->
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        input.setError("이름을 입력하세요.");
                        return;
                    }
                    db.renameBook(book.getId(), name);
                    alert.dismiss();
                    reload();
                }));
        alert.show();
    }

    private void confirmDelete(final BookSummary book) {
        new AlertDialog.Builder(this)
                .setTitle("단어장 삭제")
                .setMessage("'" + book.getName() + "' 단어장과 학습 기록을 모두 삭제할까요?")
                .setPositiveButton(t(17), (dialog, which) -> {
                    db.deleteBook(book.getId());
                    Toast.makeText(MainActivity.this, t(31), Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton(t(19), null)
                .show();
    }

    @Override public void onBackPressed() {
        if (drawerLayer != null) closeSideMenu(); else if (selectionMode) leaveSelectionMode(); else super.onBackPressed();
    }

    private static class BookAdapter extends ArrayAdapter<BookSummary> {
        private final LayoutInflater inflater;
        private Set<Long> selected = Collections.emptySet();
        private boolean selecting = false;
        void setSelection(Set<Long> ids, boolean mode) { selected = ids; selecting = mode; }

        BookAdapter(Context context, List<BookSummary> data, DatabaseHelper db) {
            super(context, 0, data);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_book, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.bookName);
                holder.stats = convertView.findViewById(R.id.bookStats);
                holder.date = convertView.findViewById(R.id.bookDate);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            BookSummary item = getItem(position);
            holder.name.setText(item.getName());
            if (selecting && selected.contains(item.getId())) {
                convertView.setBackgroundResource(R.drawable.bg_book_selected_glow);
                convertView.setElevation(dpCard(getContext(), 7));
            } else {
                convertView.setBackgroundResource(R.drawable.bg_card);
                convertView.setElevation(0f);
            }
            // Recycled ListView rows must always preserve the original card padding.
            int pad = (int) dpCard(getContext(), 20);
            convertView.setPadding(pad, pad, pad, pad);
            convertView.setAlpha(1f);
            String code = getContext().getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("ui_language","ko");
            int li=0; for(int j=0;j<LANG_CODES.length;j++) if(LANG_CODES[j].equals(code))li=j;
            holder.stats.setText(WORDS[li][25]+" " + item.getTotalCount() + " · " + WORDS[li][26]+" " + item.getMemorizedCount() + " · " + WORDS[li][27]+" " + item.getUnmemorizedCount());
            holder.date.setText(WORDS[li][28]+": " + item.getUpdatedAt().replace('T', ' '));
            return convertView;
        }

        private static float dpCard(Context context,int v) { return v*context.getResources().getDisplayMetrics().density; }
        private static class ViewHolder {
            TextView name;
            TextView stats;
            TextView date;
        }
    }
}
