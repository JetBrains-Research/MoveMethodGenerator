package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectInfo;

import java.util.function.Predicate;

public class NoTargetsMethodsFilter implements Predicate<PsiMethod> {
    private final @NotNull ProjectInfo projectInfo;

    public NoTargetsMethodsFilter(final @NotNull ProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        return !projectInfo.possibleTargets(psiMethod).isEmpty();
    }
}
