package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class StaticMethodsFilter implements Predicate<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        return !psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC);
    }
}
