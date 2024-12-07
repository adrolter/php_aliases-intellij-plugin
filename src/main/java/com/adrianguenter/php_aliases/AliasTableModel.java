package com.adrianguenter.php_aliases;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class AliasTableModel extends AbstractTableModel {
    public enum Column {
        Alias,
        Fqn;

        public static Column forIndex(int index) {
            return Column.values()[index];
        }

        public int index() {
            return this.ordinal();
        }

        public String label() {
            return switch (this) {
                case Alias -> "Alias";
                case Fqn -> "Fully Qualified Name";
            };
        }
    }

    private final List<AliasMappingDraft> draftsList = new ArrayList<>();

    private boolean isValid;

    private Runnable validationListener;

    AliasTableModel() {
        this.isValid = true;
    }

    public void setValidationListener(Runnable listener) {
        this.validationListener = listener;
    }

    private void notifyValidationStateChange() {
        if (validationListener != null) {
            validationListener.run();
        }
    }

    public List<AliasMapping> getAliasMappings(boolean resetDirty) {
        return draftsList.stream()
                .filter(AliasMappingDraft::isValid)
                .map(v -> v.toAliasMapping(resetDirty))
                .toList();
    }

    public void setAliasMappings(List<AliasMapping> aliasMappings) {
        draftsList.clear();
        draftsList.addAll(
                aliasMappings.stream()
                        .map(v -> AliasMappingDraft.fromAliasMapping(v, 0))
                        .toList());
        draftsList.add(new AliasMappingDraft(UUID.randomUUID(), draftsList.size(), "", ""));

        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return draftsList.size();
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int index) {
        return Column.forIndex(index).label();
    }

    public Object getValueAt(int rowIndex, Column column) {
        return draftsList.get(rowIndex).getValueAtColumn(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return this.getValueAt(rowIndex, Column.forIndex(columnIndex));
    }

    public void setValueAt(Object aValue, int rowIndex, Column column) {
        String value = aValue != null ? aValue.toString().trim() : "";

        draftsList.get(rowIndex).setValueAtColumn(value, column);
        fireTableCellUpdated(rowIndex, column.index());

        if (rowIndex == draftsList.size() - 1 && column == Column.Alias && !value.isEmpty()) {
            // Add a new entry when typing into the blank row
            var newRowIndex = draftsList.size();
            draftsList.add(new AliasMappingDraft(UUID.randomUUID(), newRowIndex, "", ""));
            fireTableRowsInserted(newRowIndex, newRowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        this.setValueAt(aValue, rowIndex, Column.forIndex(columnIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return Column.forIndex(columnIndex) == Column.Alias || !draftsList.get(rowIndex).getAlias().isEmpty();
    }

    public void removeRowAt(int index) {
        if (index < 0 || index > draftsList.size()) {
            throw new IndexOutOfBoundsException();
        }

        draftsList.remove(index);

        // TODO: Do we need this at all? We should remove AliasMappingDraft.index if it isn't used
        int i = 0;
        for (AliasMappingDraft draft : draftsList) {
            try {
                if (i < index) {
                    continue;
                }

                draft.setIndex(i);
            } finally {
                ++i;
            }
        }

        // TODO: Before rewriting indices?
        fireTableRowsDeleted(index, index);
    }

    public List<AliasMappingDraft.ValidationError> getValidationErrorsAt(int index) {
        return draftsList.get(index).getValidationErrors();
    }

    public boolean rowIsEmpty(int index) {
        return draftsList.get(index).isEmpty();
    }

    public boolean rowIsValid(int index) {
        return draftsList.get(index).isValid();
    }

    public boolean rowIsLast(int index) {
        return index == draftsList.size() - 1;
    }

    public boolean isValid() {
        int i = 0;
        for (AliasMappingDraft draft : draftsList) {
            try {
                if (!draft.isValid() && (i != (draftsList.size() - 1) || !draft.isEmpty())) {
                    if (this.isValid) {
                        notifyValidationStateChange();
                    }

                    this.isValid = false;
                    return false;
                }
            }
            finally {
                ++i;
            }
        }

        if (!this.isValid) {
            notifyValidationStateChange();
        }

        this.isValid = true;
        return true;
    }

    public boolean isModified() {
        for (AliasMappingDraft draft : draftsList) {
            if (draft.isDirty()) {
                return true;
            }
        }

        return false;
    }
}
