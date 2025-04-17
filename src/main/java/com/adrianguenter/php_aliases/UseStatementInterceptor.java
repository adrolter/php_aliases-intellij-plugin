package com.adrianguenter.php_aliases;

import com.adrianguenter.lib.AutoCompletionDataProvider;
import com.adrianguenter.lib.FqnType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import com.jetbrains.php.lang.psi.elements.PhpUseList;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class UseStatementInterceptor {

    private final Settings settingsService;
    private final AutoCompletionDataProvider autocompletionDataProvider;

    public UseStatementInterceptor(
            Project project
    ) {
        this.settingsService = project.getService(Settings.class);
        this.autocompletionDataProvider = project.getService(AutoCompletionDataProvider.class);

        var disposable = Disposer.newDisposable("PhpAliasesPluginTreeListener");

        PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
            @Override
            public void childAdded(@NotNull PsiTreeChangeEvent event) {
                PsiElement element = event.getChild();

                if (element instanceof PhpUseList) {
                    UseStatementInterceptor.this.handleUseStatement((PhpUseList) element, project);
                }
            }
        }, disposable);
    }

    private void handleUseStatement(
            PhpUseList useList,
            Project project
    ) {
        var aliasMappings = Objects.requireNonNull(this.settingsService.getState()).aliasMappings;

        for (var useStatement : useList.getDeclarations()) {
            String fqn = useStatement.getFQN();

            for (var aliasMapping : aliasMappings) {
                if (!aliasMapping.fullyQualifiedName.equals(fqn)) {
                    continue;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        var autoCompletionData = this.autocompletionDataProvider.forFqn(fqn).orElseThrow();

                        if (autoCompletionData.type() == FqnType.Namespace) {
                            return;
                        }

                        PsiElement fqnPsi = switch (autoCompletionData.type()) {
//                            case Namespace -> PhpIndex.getInstance(project).getNamespacesByName(fqn).iterator().next();
                            case Class, Enum, Exception ->
                                    PhpIndex.getInstance(project).getClassesByFQN(fqn).iterator().next();
                            case Interface -> PhpIndex.getInstance(project).getInterfacesByFQN(fqn).iterator().next();
                            case Trait -> PhpIndex.getInstance(project).getTraitsByFQN(fqn).iterator().next();
                            case Namespace -> throw new RuntimeException("Invalid type");
                        };

                        var references = ReferencesSearch
                                .search(fqnPsi, new LocalSearchScope(useList.getContainingFile()))
                                .findAll();

                        var aliasReference = PhpPsiElementFactory.createClassReference(project, aliasMapping.alias);

                        for (PsiReference reference : references) {
                            if (!(reference instanceof ClassReference fqnReference)
                                    || fqnReference.getParent() instanceof PhpUse) {
                                continue;
                            }

                            fqnReference.replace(aliasReference);
                        }

                        var aliasUseList = PhpPsiElementFactory.createUseStatement(project, fqn, aliasMapping.alias);
                        useList.replace(aliasUseList);
                    });
                });

                break;
            }
        }
    }

    public static final class StartupActivity
            implements ProjectActivity {
        @Override
        public @Nullable Object execute(
                @NotNull Project project,
                @NotNull Continuation<? super Unit> continuation
        ) {
            new UseStatementInterceptor(project);

            return null;
        }
    }
}
