package com.example.hamhaevoca.parser;

import android.util.Xml;

import com.example.hamhaevoca.model.ParseResult;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class XlsxParser {
    private XlsxParser() {}

    public static ParseResult parseFirstSheet(InputStream input) throws Exception {
        Map<String, byte[]> entries = readZipEntries(input);
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        String sheetName = firstSheetEntry(entries);
        if (sheetName == null) throw new IllegalArgumentException("Excel 시트를 찾을 수 없습니다.");
        List<List<String>> table = parseSheet(entries.get(sheetName), sharedStrings);
        return DelimitedTextParser.normalize(table);
    }

    private static Map<String, byte[]> readZipEntries(InputStream input) throws Exception {
        Map<String, byte[]> result = new HashMap<String, byte[]>();
        ZipInputStream zip = new ZipInputStream(input);
        try {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if ("xl/sharedStrings.xml".equals(name) ||
                        (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int read;
                    while ((read = zip.read(buffer)) != -1) out.write(buffer, 0, read);
                    result.put(name, out.toByteArray());
                }
                zip.closeEntry();
            }
        } finally {
            zip.close();
        }
        return result;
    }

    private static String firstSheetEntry(Map<String, byte[]> entries) {
        List<String> names = new ArrayList<String>();
        for (String name : entries.keySet()) {
            if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) names.add(name);
        }
        Collections.sort(names, new Comparator<String>() {
            @Override public int compare(String a, String b) { return a.compareTo(b); }
        });
        return names.isEmpty() ? null : names.get(0);
    }

    private static List<String> parseSharedStrings(byte[] bytes) throws Exception {
        List<String> result = new ArrayList<String>();
        if (bytes == null) return result;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(bytes), "UTF-8");
        int event = parser.getEventType();
        boolean insideSi = false;
        StringBuilder value = new StringBuilder();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "si".equals(parser.getName())) {
                insideSi = true;
                value.setLength(0);
            } else if (event == XmlPullParser.START_TAG && insideSi && "t".equals(parser.getName())) {
                value.append(parser.nextText());
            } else if (event == XmlPullParser.END_TAG && "si".equals(parser.getName())) {
                result.add(value.toString());
                insideSi = false;
            }
            event = parser.next();
        }
        return result;
    }

    private static List<List<String>> parseSheet(byte[] bytes, List<String> sharedStrings) throws Exception {
        if (bytes == null) throw new IllegalArgumentException("Excel 시트 데이터가 없습니다.");
        Map<Integer, Map<Integer, String>> rowMap = new HashMap<Integer, Map<Integer, String>>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(bytes), "UTF-8");
        int event = parser.getEventType();
        int currentRow = 0;
        int currentColumn = 0;
        String cellType = null;
        String cellValue = null;
        boolean inCell = false;

        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG && "row".equals(name)) {
                String r = parser.getAttributeValue(null, "r");
                currentRow = r == null ? currentRow + 1 : safeInt(r, currentRow + 1);
            } else if (event == XmlPullParser.START_TAG && "c".equals(name)) {
                inCell = true;
                String ref = parser.getAttributeValue(null, "r");
                currentColumn = columnIndex(ref);
                cellType = parser.getAttributeValue(null, "t");
                cellValue = "";
            } else if (event == XmlPullParser.START_TAG && inCell && ("v".equals(name) || "t".equals(name))) {
                cellValue = parser.nextText();
            } else if (event == XmlPullParser.END_TAG && "c".equals(name)) {
                String resolved = resolveCell(cellValue, cellType, sharedStrings);
                Map<Integer, String> row = rowMap.get(currentRow);
                if (row == null) {
                    row = new HashMap<Integer, String>();
                    rowMap.put(currentRow, row);
                }
                row.put(currentColumn, resolved);
                inCell = false;
            }
            event = parser.next();
        }

        List<Integer> rowNumbers = new ArrayList<Integer>(rowMap.keySet());
        Collections.sort(rowNumbers);
        List<List<String>> table = new ArrayList<List<String>>();
        for (Integer rowNumber : rowNumbers) {
            Map<Integer, String> row = rowMap.get(rowNumber);
            int maxColumn = -1;
            for (Integer col : row.keySet()) if (col > maxColumn) maxColumn = col;
            List<String> cells = new ArrayList<String>();
            for (int col = 0; col <= maxColumn; col++) {
                String value = row.get(col);
                cells.add(value == null ? "" : value);
            }
            table.add(cells);
        }
        return table;
    }

    private static String resolveCell(String value, String type, List<String> sharedStrings) {
        if (value == null) return "";
        if ("s".equals(type)) {
            int index = safeInt(value, -1);
            if (index >= 0 && index < sharedStrings.size()) return sharedStrings.get(index);
        }
        return value;
    }

    private static int columnIndex(String reference) {
        if (reference == null) return 0;
        int result = 0;
        int i = 0;
        while (i < reference.length() && Character.isLetter(reference.charAt(i))) {
            result = result * 26 + (Character.toUpperCase(reference.charAt(i)) - 'A' + 1);
            i++;
        }
        return Math.max(0, result - 1);
    }

    private static int safeInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return fallback; }
    }
}
