package com.minios.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minimal fixed-width table renderer for console output. Numbers are right-aligned. */
public final class AsciiTable {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public AsciiTable(String... headers) {
        this.headers = headers.clone();
    }

    public AsciiTable addRow(Object... cells) {
        if (cells.length != headers.length) {
            throw new IllegalArgumentException("expected " + headers.length + " cells, got " + cells.length);
        }
        String[] row = new String[cells.length];
        for (int i = 0; i < cells.length; i++) {
            row[i] = format(cells[i]);
        }
        rows.add(row);
        return this;
    }

    public String render() {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        String separator = separator(widths);
        StringBuilder sb = new StringBuilder(separator);
        appendRow(sb, headers, widths, false);
        sb.append(separator);
        for (String[] row : rows) {
            appendRow(sb, row, widths, true);
        }
        return sb.append(separator).toString();
    }

    private static String format(Object value) {
        if (value instanceof Double d) {
            return String.format(Locale.ROOT, "%.2f", d);
        }
        return String.valueOf(value);
    }

    private static String separator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append('+');
        }
        return sb.append('\n').toString();
    }

    private static void appendRow(StringBuilder sb, String[] cells, int[] widths, boolean alignNumbers) {
        sb.append('|');
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i];
            String pad = " ".repeat(widths[i] - cell.length());
            boolean right = alignNumbers && isNumeric(cell);
            sb.append(' ').append(right ? pad + cell : cell + pad).append(" |");
        }
        sb.append('\n');
    }

    private static boolean isNumeric(String s) {
        return !s.isEmpty() && s.matches("-?[0-9.,%]+");
    }
}
