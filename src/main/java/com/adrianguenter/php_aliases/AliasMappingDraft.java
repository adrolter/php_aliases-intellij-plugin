package com.adrianguenter.php_aliases;

import org.jetbrains.annotations.Nullable;

import java.util.*;

class AliasMappingDraft {
    public record ValidationError(String message, @Nullable AliasTableModel.Column column) {
    }

    public record ValidationWarning(String message, @Nullable AliasTableModel.Column column) {
    }

    final private UUID uuid;
    private int index;
    private String alias;
    private String fqn;
    private final String originalAlias;
    private final String originalFqn;
    private boolean isValidated;
    final private Set<String> fqnSet;
    final private List<ValidationError> validationErrors;
    final private List<ValidationWarning> validationWarnings;

    public static AliasMappingDraft fromAliasMapping(AliasMapping mapping, int index, Set<String> fqnSet) {
        return new AliasMappingDraft(
                mapping.uuid,
                index,
                mapping.alias,
                mapping.fullyQualifiedName,
                fqnSet
        );
    }

    public AliasMappingDraft(UUID uuid, int index, String alias, String fqn, Set<String> fqnSet) {
        this.uuid = uuid;
        this.index = index;
        this.setAlias(alias);
        this.setFqn(fqn);
        this.originalAlias = this.alias;
        this.originalFqn = this.fqn;
        this.isValidated = false;
        this.fqnSet = fqnSet;
        this.validationErrors = new ArrayList<>();
        this.validationWarnings = new ArrayList<>();
    }

    public AliasMapping toAliasMapping() {
        if (!this.isValid()) {
            throw new RuntimeException("Draft must be valid before converting it to an AliasMapping");
        }

        return new AliasMapping(
                this.uuid,
                this.alias,
                this.fqn,
                true
        );
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        if (index == this.index) {
            return;
        }

        this.isValidated = false;
        this.index = index;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        if (alias.equals(this.alias)) {
            return;
        }

        this.isValidated = false;
        this.alias = alias;
    }

    public String getFqn() {
        return this.fqn;
    }

    public void setFqn(String fqn) {
        if (!fqn.isEmpty() && !fqn.startsWith("\\")) {
            fqn = "\\" + fqn;
        }

        if (fqn.equals(this.fqn)) {
            return;
        }

        this.isValidated = false;
        this.fqn = fqn;
    }

    public String getValueAtColumn(AliasTableModel.Column column) {
        return switch (column) {
            case Alias -> this.getAlias();
            case Fqn -> this.getFqn();
        };
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

    public List<ValidationError> getValidationErrors() {
        if (!this.isValidated) {
            this.validate();
        }

        return Collections.unmodifiableList(this.validationErrors);
    }

    public List<ValidationError> getValidationErrorsAtColumn(AliasTableModel.Column column) {
        return this.getValidationErrors().stream()
                .filter(v -> v.column == column)
                .toList();
    }

    public List<ValidationWarning> getValidationWarnings() {
        if (!this.isValidated) {
            this.validate();
        }

        return Collections.unmodifiableList(this.validationWarnings);
    }

    public List<ValidationWarning> getValidationWarningsAtColumn(AliasTableModel.Column column) {
        return this.getValidationWarnings().stream()
                .filter(v -> v.column == column)
                .toList();
    }

    public boolean isDirty() {
        return !Objects.equals(this.alias, this.originalAlias) || !Objects.equals(this.fqn, this.originalFqn);
    }

    public boolean isEmpty() {
        return this.fqn.isEmpty() && this.alias.isEmpty();
    }

    public boolean isValid() {
        this.validate();

        return this.validationErrors.isEmpty();
    }

    public boolean isStrictlyValid() {
        if (!this.isValid()) {
            return false;
        }

        return this.validationWarnings.isEmpty();
    }

    private void validate() {
        if (this.isValidated) {
            return;
        }

        this.validationErrors.clear();
        this.validationWarnings.clear();

        // Validate Alias
        if (this.alias.isEmpty()) {
            this.validationErrors.add(new ValidationError("Alias cannot be empty.", AliasTableModel.Column.Alias));
        } else if (!alias.matches("^[A-Za-z0-9_]+$")) {
            this.validationErrors.add(new ValidationError("Invalid alias.", AliasTableModel.Column.Alias));
        }

        // Validate FQN
        if (this.fqn.isEmpty()) {
            this.validationErrors.add(new ValidationError("Fully qualified name cannot be empty.", AliasTableModel.Column.Fqn));
        } else if (!fqn.matches("^(\\\\[A-Za-z0-9_]+)+$")) {
            this.validationErrors.add(new ValidationError("Invalid fully qualified name.", AliasTableModel.Column.Fqn));
        } else if (!fqnSet.contains(this.fqn)) {
            this.validationWarnings.add(new ValidationWarning("Unknown FQN", AliasTableModel.Column.Fqn));
        }

        this.isValidated = true;
    }
}
