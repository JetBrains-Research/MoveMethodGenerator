package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.util.function.Predicate;

public class GettersFilter implements Predicate<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        return !MethodUtils.whoseGetter(psiMethod).isPresent();
    }
}
