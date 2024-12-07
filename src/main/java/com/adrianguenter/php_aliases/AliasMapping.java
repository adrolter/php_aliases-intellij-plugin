package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.UUIDConverter;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

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

    // Required for serialization(?)
    public AliasMapping() {
    }

    // Constructor for convenience
    public AliasMapping(UUID uuid, String alias, String fullyQualifiedName, boolean isActive) {
        this.uuid = uuid;
        this.alias = alias;
        this.fullyQualifiedName = fullyQualifiedName;
        this.isActive = isActive;
    }

    public AliasTableModel.AliasFqnPair getAliasFqnPair() {
        return new AliasTableModel.AliasFqnPair(this.alias, this.fullyQualifiedName);
    }
}
