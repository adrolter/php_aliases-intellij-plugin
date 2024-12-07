package com.adrianguenter.php_aliases;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class AliasMappingDraft {
    public record ValidationError(AliasTableModel.Column column, String message) {
    }

    final private UUID uuid;
    private int index;
    private String alias;
    private String fqn;
    private boolean isValidated;
    final private List<ValidationError> validationErrors;
    private boolean isDirty;

    public static AliasMappingDraft fromAliasMapping(AliasMapping mapping, int index) {
        return new AliasMappingDraft(
                mapping.uuid,
                index,
                mapping.alias,
                mapping.fullyQualifiedName
        );
    }

    public AliasMappingDraft(UUID uuid, int index, String alias, String fqn) {
        this.uuid = uuid;
        this.index = index;
        this.setAlias(alias);
        this.setFqn(fqn);
        this.isValidated = false;
        this.validationErrors = new ArrayList<>();
        this.isDirty = false;
    }

    public AliasMapping toAliasMapping(boolean resetDirty) {
        if (!this.isValid()) {
            throw new RuntimeException("Draft must be valid before converting it to an AliasMapping");
        }

        var mapping = new AliasMapping(
                this.uuid,
                this.alias,
                this.fqn,
                true
        );

        if (resetDirty) {
            this.isDirty = false;
        }

        return mapping;
    }

    public AliasMapping toAliasMapping() {
        return this.toAliasMapping(false);
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        if (index != this.index) {
            this.isDirty = true;
            this.isValidated = false;
            this.index = index;
        }


    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        if (!alias.equals(this.alias)) {
            this.isDirty = true;
            this.isValidated = false;
            this.alias = alias;
        }
    }

    public String getFqn() {
        return this.fqn;
    }

    public void setFqn(String fqn) {
        if (!fqn.isEmpty() && !fqn.startsWith("\\")) {
            fqn = "\\" + fqn;
        }

        if (!fqn.equals(this.fqn)) {
            this.isDirty = true;
            this.isValidated = false;
            this.fqn = fqn;
        }
    }

    public String getValueAtColumn(AliasTableModel.Column column) {
        return switch (column) {
            case Alias -> this.getAlias();
            case Fqn -> this.getFqn();
        };
    }

    public String getValueAtColumn(int column) {
        return this.getValueAtColumn(AliasTableModel.Column.forIndex(column));
    }

    public void setValueAtColumn(String value, AliasTableModel.Column column) {
        switch (column) {
            case Alias:
                this.setAlias(value);
                break;
            case Fqn:
                this.setFqn(value);
                break;
        }
    }

    public void setValueAtColumn(String value, int column) {
        this.setValueAtColumn(value, AliasTableModel.Column.forIndex(column));
    }

    public List<ValidationError> getValidationErrors() {
        if (!this.isValidated) {
            this.validate();
        }

        return List.copyOf(this.validationErrors);
    }

    public List<ValidationError> getValidationErrorsAtColumn(AliasTableModel.Column column) {
        return this.getValidationErrors().stream()
                .filter(v -> v.column == column)
                .toList();
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public boolean isEmpty() {
        return this.fqn.isEmpty() && this.alias.isEmpty();
    }

    public boolean isValid() {
        this.validate();

        return this.validationErrors.isEmpty();
    }

    private void validate() {
        if (this.isValidated) {
            return;
        }

        this.validationErrors.clear();

        // Validate Alias
        if (this.alias.isEmpty()) {
            this.validationErrors.add(new ValidationError(AliasTableModel.Column.Alias, "Alias cannot be empty."));
        } else if (!alias.matches("^[A-Za-z0-9_]+$")) {
            this.validationErrors.add(new ValidationError(AliasTableModel.Column.Alias, "Invalid alias."));
        }

        // Validate FQN
        if (this.fqn.isEmpty()) {
            this.validationErrors.add(new ValidationError(AliasTableModel.Column.Fqn, "Fully qualified name cannot be empty."));
        } else if (!fqn.matches("^(\\\\[A-Za-z0-9_]+)+$")) {
            this.validationErrors.add(new ValidationError(AliasTableModel.Column.Fqn, "Invalid fully qualified name."));
        }

        this.isValidated = true;

//            var previous = getValidationError(rowIndex);
//            validationErrors.set(rowIndex, error);
//
//            if (!Objects.equals(previous, error)) {
//                fireTableCellUpdated(rowIndex, 0);
//                fireTableCellUpdated(rowIndex, 1);
//                notifyValidationStateChange();
//            }
    }
}
