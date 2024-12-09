package com.adrianguenter.php_aliases;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Service(Service.Level.PROJECT)
@State(
        name = "com.adrianguenter.php_aliases.Settings",
        storages = @Storage("PhpAliases.xml")
)
final class Settings
        implements PersistentStateComponent<Settings.State> {

    private final Project project;
    private State state = new State();

    public Settings(
            Project project
    ) {
        this.project = project;
    }

    public static Settings getInstance(@NotNull Project project) {
        return project.getService(Settings.class);
    }

    @Override
    @Nullable
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {

        final var seenUuids = new HashSet<UUID>();
        final var aliasFqnPairs = new HashSet<AliasTableModel.AliasFqnPair>();

        for (int i = 0; i < state.aliasMappings.size(); i++) {
            var mapping = state.aliasMappings.get(i);
            var aliasFqnPair = mapping.getAliasFqnPair();

            if (seenUuids.contains(mapping.uuid)) {
                mapping.uuid = UUID.randomUUID();
//                throw new IllegalArgumentException("Duplicate UUID: " + mapping.uuid);
            }

            if (aliasFqnPairs.contains(aliasFqnPair)) {
                state.aliasMappings.remove(i--);
            }

            seenUuids.add(mapping.uuid);
            aliasFqnPairs.add(aliasFqnPair);
        }

//        ApplicationManager.getApplication().invokeLater(() -> {
//            ApplicationManager.getApplication().runWriteAction(() -> {
//                this.project.save();
//            });
//        });

//        for (var mapping : state.aliasMappings) {
//            var aliasFqnPair = mapping.getAliasFqnPair();
//            if (seenUuids.contains(mapping.uuid)) {
//                mapping.uuid = UUID.randomUUID();
////                throw new IllegalArgumentException("Duplicate UUID: " + mapping.uuid);
//            }
//
//            if (aliasFqnPairs.contains(aliasFqnPair)) {
//                continue;
//            }
//
//            seenUuids.add(mapping.uuid);
//            aliasFqnPairs.add(aliasFqnPair);
//        }
//
//        state.aliasMappings = state.aliasMappings.stream().filter(mapping -> seenUuids.contains(mapping.uuid)).toList();

        this.state = state;
    }

    static class State {
        @Tag("aliasMappings")
        @NonNls
        public List<AliasMapping> aliasMappings = new ArrayList<>();
    }
}
