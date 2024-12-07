package com.adrianguenter.php_aliases;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class SettingsConfigurable
        implements Configurable {

    private SettingsComponent settingsComponent;
    private final Project project;

    public SettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Aliases";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new SettingsComponent();
        settingsComponent.getTableModel().setValidationListener(() -> ApplicationManager.getApplication().invokeLater(this::fireStateChanged));
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsComponent == null) {
            return false;
        }

        if (!settingsComponent.getTableModel().isValid()) {
            // An invalid model always implies modification
            return true;
        }

        return settingsComponent.getTableModel().isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());

        if (!settingsComponent.getTableModel().isValid()) {
            throw new ConfigurationException("There are validation errors in the alias mappings. Please fix them before applying.");
        }

        state.aliasMappings = settingsComponent.getTableModel().getAliasMappings(true);
    }

    @Override
    public void reset() {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());
        settingsComponent.getTableModel().setAliasMappings(state.aliasMappings);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

    private void fireStateChanged() {
        if (settingsComponent == null) {
            // User clicked "Cancel", etc.
            return;
        }

        DialogWrapper dialog = DialogWrapper.findInstance(settingsComponent.getPanel());
        if (dialog != null) {
            dialog.setOKActionEnabled(isModified() && settingsComponent.getTableModel().isValid());
        }
    }
}
