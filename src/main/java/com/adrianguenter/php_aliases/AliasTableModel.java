package com.adrianguenter.php_aliases;

import javax.swing.table.AbstractTableModel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AliasTableModel extends AbstractTableModel {
    private final List<Map.Entry<String, String>> aliasList = new ArrayList<>();

    private static final String[] COLUMN_NAMES = {"Alias", "Fully Qualified Name"};

    @Override
    public int getRowCount() {
        return aliasList.size() + 1; // Always show one extra row for new entry
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= aliasList.size()) {
            return ""; // Return empty for the "new entry" row
        }
        Map.Entry<String, String> entry = aliasList.get(rowIndex);
        return columnIndex == 0 ? entry.getKey() : entry.getValue();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 1 && !aValue.toString().startsWith("\\")) {
            aValue = "\\" + aValue;
        }

        if (rowIndex >= aliasList.size()) {
            if (columnIndex == 0 && aValue != null && !aValue.toString().trim().isEmpty()) {
                // Add a new entry when typing into the blank row
                aliasList.add(new AbstractMap.SimpleEntry<>(aValue.toString().trim(), ""));
                fireTableRowsInserted(aliasList.size() - 1, aliasList.size() - 1);
            }
        } else {
            Map.Entry<String, String> entry = aliasList.get(rowIndex);
            entry.setValue((String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true; // All cells should be editable
    }

    public Map<String, String> getAliasMappings() {
        return aliasList.stream().filter(entry -> !entry.getKey().trim().isEmpty() && !entry.getValue().trim().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setAliasMappings(Map<String, String> aliasMappings) {
        aliasList.clear();
        aliasList.addAll(aliasMappings.entrySet());
        fireTableDataChanged();
    }

    public void removeRow(int rowIndex) {
        if (rowIndex < aliasList.size()) {
            aliasList.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
}
