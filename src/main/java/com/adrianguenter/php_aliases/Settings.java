package com.adrianguenter.php_aliases;

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

    private State state = new State();

    public Settings(
    ) {
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

        this.state = state;
    }

    static class State {
        @Tag("aliasMappings")
        @NonNls
        public List<AliasMapping> aliasMappings = new ArrayList<>();
    }
}
