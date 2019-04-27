package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

public class MethodCallWithSuperFilter implements Filter<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        final Ref<Boolean> resultRef = new Ref<>(true);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(
                final @NotNull PsiMethodCallExpression expression
            ) {
                super.visitMethodCallExpression(expression);

                if (expression.getMethodExpression().getQualifierExpression() instanceof PsiSuperExpression) {
                    resultRef.set(false);
                }
            }
        }.visitElement(psiMethod);

        return resultRef.get();
    }
}
