package com.adrianguenter.lib;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;

import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class AutoCompletionDataProvider {

    private final PhpIndex phpIndex;

    public AutoCompletionDataProvider(
            Project project
    ) {
        this.phpIndex = PhpIndex.getInstance(project);
    }

    public Optional<AutoCompletionData> forFqn(
            String fqn
    ) {
        var unprefixedFqn = fqn.substring(1);

        var maybePhpClass = this.phpIndex.getAnyByFQN(fqn).stream().findFirst();
        if (maybePhpClass.isEmpty()) {
            var maybePhpNamespace = this.phpIndex.getNamespacesByName(fqn).stream().findFirst();
            if (maybePhpNamespace.isPresent()) {
                return Optional.of(new AutoCompletionData(
                        FqnType.Namespace,
                        PhpIcons.NAMESPACE,
                        null,
                        "Namespace",
                        unprefixedFqn
                ));
            }

            return Optional.empty();
        }

        var phpClass = maybePhpClass.get();

        if (phpClass.isInterface()) {
            return Optional.of(new AutoCompletionData(
                    FqnType.Interface,
                    PhpIcons.INTERFACE,
                    null,
                    "Interface",
                    unprefixedFqn
            ));
        } else if (phpClass.isTrait()) {
            return Optional.of(new AutoCompletionData(
                    FqnType.Trait,
                    PhpIcons.TRAIT,
                    null,
                    "Trait",
                    unprefixedFqn
            ));
        } else if (phpClass.isEnum()) {
            return Optional.of(new AutoCompletionData(
                    FqnType.Enum,
                    AllIcons.Nodes.Enum,
                    null,
                    "Enum",
                    unprefixedFqn
            ));
        } else {
            var currentClass = phpClass;
            do {
                if ("\\Exception".equals(currentClass.getFQN())) {
                    return Optional.of(new AutoCompletionData(
                            FqnType.Exception,
                            PhpIcons.EXCEPTION,
                            null,
                            "Exception",
                            unprefixedFqn
                    ));
                }
            } while ((currentClass = currentClass.getSuperClass()) != null);

            return Optional.of(new AutoCompletionData(
                    FqnType.Class,
                    PhpIcons.CLASS,
                    null,
                    "Class",
                    unprefixedFqn
            ));
        }
    }
}
