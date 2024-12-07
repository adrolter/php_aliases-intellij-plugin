package com.adrianguenter.lib;

import com.intellij.icons.AllIcons;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;

import java.util.Optional;

public class AutoCompletionDataProvider {

    private final PhpIndex phpIndex;

    public AutoCompletionDataProvider(
            PhpIndex phpIndex
    ) {

        this.phpIndex = phpIndex;
    }

    public Optional<AutoCompletionData> forFqn(
            String fqn
    ) {
        var unprefixedFqn = fqn.substring(1);

        var optionalPhpClass = this.phpIndex.getAnyByFQN(fqn).stream().findFirst();
        if (optionalPhpClass.isEmpty()) {
            var optionalPhpNamespace = this.phpIndex.getNamespacesByName(fqn).stream().findFirst();
            if (optionalPhpNamespace.isPresent()) {
                return Optional.of(new AutoCompletionData(
                        PhpIcons.NAMESPACE,
                        null,
                        "Namespace",
                        unprefixedFqn
                ));
            }

            return Optional.empty();
        }

        var phpClass = optionalPhpClass.get();

        if (phpClass.isInterface()) {
            return Optional.of(new AutoCompletionData(
                    PhpIcons.INTERFACE,
                    null,
                    "Interface",
                    unprefixedFqn
            ));
        } else if (phpClass.isTrait()) {
            return Optional.of(new AutoCompletionData(
                    PhpIcons.TRAIT,
                    null,
                    "Trait",
                    unprefixedFqn
            ));
        } else if (phpClass.isEnum()) {
            return Optional.of(new AutoCompletionData(
                    AllIcons.Nodes.Enum,
                    null,
                    "Enum",
                    unprefixedFqn
            ));
        } else {
            var isException = false;
            var currentClass = phpClass;
            do {
                if ("\\Exception".equals(currentClass.getFQN())) {
                    isException = true;
                    break;
                }
            } while ((currentClass = currentClass.getSuperClass()) != null);

            return Optional.of(new AutoCompletionData(
                    isException ? PhpIcons.EXCEPTION : PhpIcons.CLASS,
                    null,
                    "Class",
                    unprefixedFqn
            ));
        }
    }
}
