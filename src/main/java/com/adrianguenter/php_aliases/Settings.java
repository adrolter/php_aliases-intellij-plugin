package com.adrianguenter.php_aliases;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
@State(
        name = "com.adrianguenter.php_aliases.Settings",
        storages = @Storage("PhpAliases.xml")
)
final class Settings
        implements PersistentStateComponent<Settings.State> {

    static class State {
        @NonNls
        public Map<String, String> aliasMappings = new HashMap<>();
    }

    private State state = new State();

    public static Settings getInstance(@NotNull Project project) {
        return project.getService(Settings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
}
