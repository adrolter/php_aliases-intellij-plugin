package com.adrianguenter.php_aliases;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class SettingsConfigurable implements Configurable {

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
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());
        return !settingsComponent.getAliasMappings().equals(state.aliasMappings);
    }

    @Override
    public void apply() {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());
        state.aliasMappings = settingsComponent.getAliasMappings();
    }

    @Override
    public void reset() {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());
        settingsComponent.setAliasMappings(state.aliasMappings);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
