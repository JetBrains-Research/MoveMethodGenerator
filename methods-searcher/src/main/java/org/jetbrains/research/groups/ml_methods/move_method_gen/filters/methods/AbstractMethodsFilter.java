package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

import java.util.function.Predicate;

public class AbstractMethodsFilter implements Filter<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        return psiMethod.getBody() != null;
    }
}
