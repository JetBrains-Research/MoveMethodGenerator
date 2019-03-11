package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class PrivateMethodsCallersFilter implements Predicate<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        final Ref<Boolean> resultRef = new Ref<>(true);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(
                final @NotNull PsiMethodCallExpression expression
            ) {
                super.visitMethodCallExpression(expression);

                PsiMethod calledMethod = expression.resolveMethod();
                if (calledMethod == null) {
                    return;
                }

                if (!calledMethod.hasModifierProperty(PsiModifier.PUBLIC)) {
                    resultRef.set(false);
                }
            }
        }.visitElement(psiMethod);

        return resultRef.get();
    }
}
