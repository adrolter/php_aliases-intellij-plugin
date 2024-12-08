package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.AutoCompletionData;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class SettingsConfigurable
        implements Configurable {

    private SettingsComponent settingsComponent;
    private final Project project;
    private final HashMap<String, AutoCompletionData> knownFqns = new HashMap<>();

    public SettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Aliases";
    }

    @Override
    public @Nullable JComponent createComponent() {
        this.settingsComponent = new SettingsComponent(
                this.project,
                Collections.unmodifiableMap(this.knownFqns),
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
        this.populateKnownFqns();
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

        state.aliasMappings = settingsComponent.getTableModel().getAliasMappings();

        reset();
    }

    @Override
    public void reset() {
        Settings.State state = Objects.requireNonNull(Settings.getInstance(project).getState());
        settingsComponent.getTableModel().setAliasMappings(state.aliasMappings);

        DialogWrapper dialog = DialogWrapper.findInstance(settingsComponent.getPanel());
        if (dialog != null) {
            dialog.setOKActionEnabled(true);
        }
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

    private void populateKnownFqns() {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        this.knownFqns.clear();

        var processNamespace = new Function<String, Void>() {
            static final Pattern regex = Pattern.compile("^(?<namespace>(?:\\\\[A-Za-z0-9_]+)+)\\\\(?<name>[A-Za-z0-9_]+)$");

            @Override
            public Void apply(String arg) {
                var matcher = regex.matcher(arg);
                if (!matcher.find()) {
                    return null;
                }

                var namespace = matcher.group("namespace");

                if (knownFqns.containsKey(namespace)) {
                    return null;
                }

                knownFqns.put(namespace, new AutoCompletionData(PhpIcons.NAMESPACE, null, "Namespace", namespace.substring(1)));

                this.apply(namespace);

                return null;
            }
        };

        for (String className : phpIndex.getAllClassNames(null)) {
            phpIndex.getClassesByName(className).forEach(phpClass -> {
                if (phpClass.isEnum()) {
                    this.knownFqns.put(phpClass.getFQN(), new AutoCompletionData(AllIcons.Nodes.Enum, null, "Enum", phpClass.getFQN().substring(1)));
                } else {
                    this.knownFqns.put(phpClass.getFQN(), new AutoCompletionData(PhpIcons.CLASS, null, "Class", phpClass.getFQN().substring(1)));
                }

                processNamespace.apply(phpClass.getFQN());
            });
        }

        for (String interfaceName : phpIndex.getAllInterfaceNames()) {
            phpIndex.getInterfacesByName(interfaceName).forEach(phpClass -> {
                this.knownFqns.put(phpClass.getFQN(), new AutoCompletionData(PhpIcons.INTERFACE, null, "Interface", phpClass.getFQN().substring(1)));

                processNamespace.apply(phpClass.getFQN());
            });
        }

        for (String traitName : phpIndex.getAllTraitNames()) {
            phpIndex.getTraitsByName(traitName).forEach(phpClass -> {
                this.knownFqns.put(phpClass.getFQN(), new AutoCompletionData(PhpIcons.TRAIT, null, "Trait", phpClass.getFQN().substring(1)));

                processNamespace.apply(phpClass.getFQN());
            });
        }
    }
}
