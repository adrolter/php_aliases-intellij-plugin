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

    public record AliasFqnPair(String alias, String fqn) {
    }

    private final List<AliasMappingDraft> draftsList = new ArrayList<>();
    private final Runnable validationListener;
    private final Set<String> fqnSet;
    private int originalAliasMappingsCount;
    private boolean isValid;

    AliasTableModel(Runnable validationListener, Set<String> fqnSet) {
        this.validationListener = validationListener;
        this.fqnSet = fqnSet;
        this.isValid = true;
    }

    private void notifyValidationStateChange() {
        if (this.validationListener != null) {
            this.validationListener.run();
        }
    }

    public List<AliasMapping> getAliasMappings() {
        return this.draftsList.stream()
                .filter(AliasMappingDraft::isValid)
                .map(AliasMappingDraft::toAliasMapping)
                .toList();
    }

    public void setAliasMappings(List<AliasMapping> aliasMappings) {
        this.draftsList.clear();
        this.draftsList.addAll(
                aliasMappings.stream()
                        .map(v -> AliasMappingDraft.fromAliasMapping(v, 0, this.fqnSet))
                        .toList());
        this.draftsList.add(new AliasMappingDraft(UUID.randomUUID(), this.draftsList.size(), "", "", this.fqnSet));

        this.originalAliasMappingsCount = this.draftsList.size();
        this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return this.draftsList.size();
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int index) {
        return Column.forIndex(index).label();
    }

    public String getValueAt(int rowIndex, Column column) {
        return this.draftsList.get(rowIndex).getValueAtColumn(column);
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        return this.getValueAt(rowIndex, Column.forIndex(columnIndex));
    }

    public void setValueAt(Object aValue, int rowIndex, Column column) {
        String value = aValue != null ? aValue.toString().trim() : "";

        this.draftsList.get(rowIndex).setValueAtColumn(value, column);
        this.fireTableCellUpdated(rowIndex, column.index());

        if (rowIndex == this.draftsList.size() - 1 && column == Column.Alias && !value.isEmpty()) {
            // Add value new entry when typing into the blank row
            var newRowIndex = this.draftsList.size();
            this.draftsList.add(new AliasMappingDraft(UUID.randomUUID(), newRowIndex, "", "", this.fqnSet));
            this.fireTableRowsInserted(newRowIndex, newRowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        this.setValueAt(aValue, rowIndex, Column.forIndex(columnIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return Column.forIndex(columnIndex) == Column.Alias || !this.draftsList.get(rowIndex).getAlias().isEmpty();
    }

    public void removeRowAt(int index) {
        if (index < 0 || index > this.draftsList.size()) {
            throw new IndexOutOfBoundsException();
        }

        this.draftsList.remove(index);

        // TODO: Do we need this at all? We should remove AliasMappingDraft.index if it isn't used
        int i = 0;
        for (AliasMappingDraft draft : this.draftsList) {
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
        this.fireTableRowsDeleted(index, index);
        if (index > 0) {
            this.fireTableRowsUpdated(index, index - 1);
        }
    }

    public List<AliasMappingDraft.ValidationError> getValidationErrorsAt(int index, Column column) {
        return this.draftsList.get(index).getValidationErrorsAtColumn(column);
    }

    public List<AliasMappingDraft.ValidationError> getValidationErrorsAt(int index) {
        return this.draftsList.get(index).getValidationErrors();
    }

    public List<AliasMappingDraft.ValidationWarning> getValidationWarningsAt(int index, Column column) {
        return this.draftsList.get(index).getValidationWarningsAtColumn(column);
    }

    public List<AliasMappingDraft.ValidationWarning> getValidationWarningsAt(int index) {
        return this.draftsList.get(index).getValidationWarnings();
    }

    public boolean rowIsEmpty(int index) {
        return this.draftsList.get(index).isEmpty();
    }

    public boolean rowIsValid(int index) {
        return this.draftsList.get(index).isValid();
    }

    public boolean rowIsLast(int index) {
        return index == this.draftsList.size() - 1;
    }

    public boolean isValid() {
        int i = 0;
        for (AliasMappingDraft draft : this.draftsList) {
            try {
                if (!draft.isValid() && (!this.rowIsLast(i) || !draft.isEmpty())) {
                    if (this.isValid) {
                        this.notifyValidationStateChange();
                    }

                    this.isValid = false;
                    return false;
                }
            } finally {
                ++i;
            }
        }

        if (!this.isValid) {
            this.notifyValidationStateChange();
        }

        this.isValid = true;
        return true;
    }

    public boolean isModified() {
        if (this.draftsList.size() != this.originalAliasMappingsCount) {
            return true;
        }

        for (AliasMappingDraft draft : this.draftsList) {
            if (draft.isDirty()) {
                return true;
            }
        }

        return false;
    }
}
