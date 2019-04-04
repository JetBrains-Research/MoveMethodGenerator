package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.RelevantClasses;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

import java.util.function.Predicate;

public class NoTargetsMethodsFilter implements Filter<PsiMethod> {
    private final @NotNull
    RelevantClasses relevantClasses;

    public NoTargetsMethodsFilter(final @NotNull RelevantClasses relevantClasses) {
        this.relevantClasses = relevantClasses;
    }

    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        return !relevantClasses.possibleTargets(psiMethod).isEmpty();
    }
}
