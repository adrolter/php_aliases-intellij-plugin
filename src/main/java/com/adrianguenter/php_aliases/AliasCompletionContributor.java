package com.adrianguenter.php_aliases;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class AliasCompletionContributor
        extends CompletionContributor {

    public AliasCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {

                Settings.State state = Objects.requireNonNull(Settings.getInstance(parameters.getOriginalFile().getProject()).getState());

                // Fetch the current typed text
                String currentPrefix = resultSet.getPrefixMatcher().getPrefix();

                if (currentPrefix.isEmpty()) {
                    return;
                }

                // Add matching aliases based on user-defined mappings
                for (Map.Entry<String, String> entry : state.aliasMappings.entrySet()) {
                    String alias = entry.getKey();
                    String fqcn = entry.getValue();

                    boolean aliasStartsWithInput = alias.startsWith(currentPrefix);
                    boolean fqcnEndsWithInput = fqcn.endsWith("\\" + currentPrefix);

                    if (!aliasStartsWithInput && !fqcnEndsWithInput) {
                        continue;
                    }

                    PhpFile phpFile = (PhpFile) parameters.getOriginalFile();

                    PsiElement elementContext = parameters.getOriginalPosition();
                    PhpNamespace namespace = PsiTreeUtil.getParentOfType(elementContext, PhpNamespace.class);

                    if (isUseStatementPresent(namespace != null ? namespace : phpFile, fqcn, alias)) {
                        continue;
                    }

                    LookupElementBuilder builder = LookupElementBuilder.create(alias)
                            .withInsertHandler(new AliasInsertHandler(fqcn, alias))
                            .withLookupString(fqcn)
                            .withTypeText(fqcn.substring(1)) // strip leading \
                            .withItemTextItalic(true)
                            .withIcon(AllIcons.Nodes.Alias);

                    if (fqcnEndsWithInput) {
                        builder = builder.withLookupString(currentPrefix);
                    }

                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder
//                                            .withAutoCompletionPolicy(AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE)
                                    , 1000.0)
                    );
                }

                // Ensure fully qualified names are still suggested
                resultSet.restartCompletionOnAnyPrefixChange();
            }
        });
    }

    // TODO: only look at DIRECT children when provided a PhpFile! Otherwise we find use statements in namespaces too
    // TODO: Deduplicate this method...where to put it?
    private boolean isUseStatementPresent(PhpPsiElement context, String fqcn, String alias) {
        Collection<PhpUse> useStatements = PsiTreeUtil.findChildrenOfType(context, PhpUse.class);
        for (PhpUse useStatement : useStatements) {
            if (useStatement.getFQN().equals(fqcn) && alias.equals(useStatement.getAliasName())) {
                return true;
            }
        }

        return false;
    }
}
