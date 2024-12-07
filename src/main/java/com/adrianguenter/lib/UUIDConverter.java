package com.adrianguenter.lib;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDConverter extends Converter<UUID> {
    @Override
    public @Nullable UUID fromString(@NotNull String value) {
        return UUID.fromString(value); // Deserialize from string
    }

    @Override
    public @Nullable String toString(@NotNull UUID value) {
        return value.toString(); // Serialize to string
    }
}
