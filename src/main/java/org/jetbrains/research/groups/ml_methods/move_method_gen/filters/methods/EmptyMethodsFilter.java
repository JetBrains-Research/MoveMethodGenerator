package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.isConstExpression;

public class EmptyMethodsFilter implements Predicate<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        PsiCodeBlock codeBlock = psiMethod.getBody();
        if (codeBlock == null) {
            return false;
        }

        PsiStatement[] statements = codeBlock.getStatements();
        if (statements.length == 0) {
            return false;
        }

        if (statements.length > 1) {
            return true;
        }

        PsiStatement theStatement = statements[0];

        if (!(theStatement instanceof PsiReturnStatement)) {
            return true;
        }

        PsiReturnStatement returnStatement = (PsiReturnStatement) theStatement;
        return !isConstExpression(returnStatement.getReturnValue());
    }
}
