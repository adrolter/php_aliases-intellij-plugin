package com.adrianguenter.php_aliases;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AliasCompletionContributor
        extends CompletionContributor {

    public AliasCompletionContributor() {
        this.extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet resultSet
                    ) {
                        var settingsService = Settings.getInstance(parameters.getOriginalFile().getProject());
                        var state = Objects.requireNonNull(settingsService.getState());
                        var phpFile = (PhpFile) parameters.getOriginalFile();
                        var elementContext = parameters.getOriginalPosition();
                        var namespace = PsiTreeUtil.getParentOfType(elementContext, PhpNamespace.class);

                        var currentPrefix = resultSet.getPrefixMatcher().getPrefix();
                        if (currentPrefix.isEmpty()) {
                            return;
                        }

                        for (var aliasMapping : state.aliasMappings) {
                            var alias = aliasMapping.alias;
                            var fqn = aliasMapping.fullyQualifiedName;

                            if (AliasCompletionContributor.this.isUseStatementPresent(
                                    namespace != null ? namespace : phpFile,
                                    fqn,
                                    alias
                            )) {
                                continue;
                            }

                            var builder = LookupElementBuilder.create(alias)
                                    .withInsertHandler(new AliasInsertHandler(fqn, alias))
                                    .withLookupString(fqn)
                                    .withTypeText(fqn.substring(1))
                                    .withItemTextItalic(true)
                                    .withIcon(AllIcons.Nodes.Alias);

                            resultSet.addElement(PrioritizedLookupElement.withPriority(builder, 1000.0));
                        }

                        // Ensure fully qualified names are still suggested
                        resultSet.restartCompletionOnAnyPrefixChange();
                    }
                }
        );
    }

    // TODO: only look at DIRECT children when provided value PhpFile! Otherwise we find use statements in namespaces too
    // TODO: Deduplicate this method...where to put it?
    private boolean isUseStatementPresent(PhpPsiElement context, String fqcn, String alias) {
        var useStatements = PsiTreeUtil.findChildrenOfType(context, PhpUse.class);
        for (var useStatement : useStatements) {
            if (useStatement.getFQN().equals(fqcn) && alias.equals(useStatement.getAliasName())) {
                return true;
            }
        }

        return false;
    }
}
