package com.adrianguenter.php_aliases;

import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AliasInsertHandler implements InsertHandler<LookupElement> {

    private final String fqcn;
    private final String alias;

    public AliasInsertHandler(String fqcn, String alias) {
        this.fqcn = fqcn;
        this.alias = alias;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        PsiFile file = context.getFile();
        if (!(file instanceof PhpFile phpFile)) {
            return;
        }

        PsiElement contextElement = phpFile.findElementAt(context.getStartOffset());
        if (contextElement == null) {
            return;
        }

        Project project = context.getProject();
        PhpNamespace namespace = PsiTreeUtil.getParentOfType(contextElement, PhpNamespace.class);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            // Insert alias at caret position
            context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), this.alias);

            if (!this.isUseStatementPresent(namespace != null ? namespace : phpFile, this.fqcn, this.alias)) {
                this.addUseStatementWithAlias(project, namespace != null ? namespace : phpFile, this.fqcn, this.alias);
                var optimizer = new OptimizeImportsProcessor(phpFile.getProject(), phpFile);
                optimizer.run();
            }
        });
    }

    private void addUseStatementWithAlias(Project project, PhpPsiElement target, String fqcn, String alias) {
        PhpUseList useStmt = PhpPsiElementFactory.createUseStatement(project, fqcn, alias);
        var child = target.getFirstPsiChild();
        while (child != null && !(child instanceof GroupStatement)) {
            child = child.getNextPsiSibling();
        }

        if (child == null) {
            return;
        }

        child.addBefore(useStmt, child.getFirstPsiChild());
    }

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
