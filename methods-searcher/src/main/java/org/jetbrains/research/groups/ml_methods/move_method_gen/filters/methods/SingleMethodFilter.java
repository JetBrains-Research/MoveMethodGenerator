package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class SingleMethodFilter implements Predicate<PsiMethod> {
    private static final @NotNull EmptyMethodsFilter isNotEmpty = new EmptyMethodsFilter();

    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        for (PsiMethod method : psiMethod.getContainingClass().getMethods()) {
            if (!method.equals(psiMethod) && !method.isConstructor() && isNotEmpty.test(method)) {
                return true;
            }
        }

        return false;
    }
}
