package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods.EmptyMethodsFilter;

import java.util.function.Predicate;

public class EmptyClassesFilter implements Filter<PsiClass> {
    private static final @NotNull EmptyMethodsFilter isNotEmpty = new EmptyMethodsFilter();

    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        for (PsiMethod method : psiClass.getMethods()) {
            if (!method.isConstructor() && isNotEmpty.test(method)) {
                return true;
            }
        }

        return false;
    }
}
