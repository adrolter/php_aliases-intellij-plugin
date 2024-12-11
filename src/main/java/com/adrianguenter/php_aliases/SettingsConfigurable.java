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
    private final Settings settingsService;

    public SettingsConfigurable(@NotNull Project project) {
        this.project = project;
        this.settingsService = project.getService(Settings.class);
    }

    @Override
    public String getDisplayName() {
        return "Aliases";
    }

    @Override
    public @Nullable JComponent createComponent() {
        this.settingsComponent = new SettingsComponent(
                this.project,
                () -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (this.settingsComponent == null) {
                        // User clicked "Cancel", etc.
                        return;
                    }

                    DialogWrapper dialog = DialogWrapper.findInstance(this.settingsComponent.getPanel());
                    if (dialog != null) {
                        dialog.setOKActionEnabled(this.settingsComponent.getTableModel().isValid());
                    }
                })
        );
        return this.settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (this.settingsComponent == null) {
            return false;
        }

        if (!this.settingsComponent.getTableModel().isValid()) {
            // An invalid model always implies modification
            return true;
        }

        return this.settingsComponent.getTableModel().isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!this.settingsComponent.getTableModel().isValid()) {
            throw new ConfigurationException("There are validation errors in the alias mappings. Please fix them before applying.");
        }

        this.getSettingsState().aliasMappings = this.settingsComponent.getTableModel().getAliasMappings();

        this.reset();
    }

    @Override
    public void reset() {
        this.settingsComponent.getTableModel().setAliasMappings(this.getSettingsState().aliasMappings);

        DialogWrapper dialog = DialogWrapper.findInstance(this.settingsComponent.getPanel());
        if (dialog != null) {
            dialog.setOKActionEnabled(true);
        }
    }

    @Override
    public void disposeUIResources() {
        this.settingsComponent = null;
    }

    private Settings.State getSettingsState() {
        return Objects.requireNonNull(this.settingsService.getState());
    }
}
