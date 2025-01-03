package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.UUIDConverter;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

import java.util.Set;
import java.util.UUID;

@Tag("AliasMapping")
public class AliasMapping {

    @Attribute(value = "uuid", converter = UUIDConverter.class)
    public UUID uuid;

    @Attribute(value = "active")
    public boolean isActive;

    @Tag("alias")
    public String alias;

    @Tag("fqn")
    public String fullyQualifiedName;

    @Tag("scopes")
    public Set<Scope> scopes;

    // Required for serialization(?)
    public AliasMapping() {
    }

    // Constructor for convenience
    public AliasMapping(
            UUID uuid,
            String alias,
            String fullyQualifiedName,
            Set<Scope> scopes,
            boolean isActive
    ) {
        this.uuid = uuid;
        this.alias = alias;
        this.fullyQualifiedName = fullyQualifiedName;
        this.scopes = scopes;
        this.isActive = isActive;
    }

    public AliasTableModel.AliasFqnPair getAliasFqnPair() {
        return new AliasTableModel.AliasFqnPair(
                this.alias,
                this.fullyQualifiedName
        );
    }

    public record Scope(
            String name
    ) {
    }
}
