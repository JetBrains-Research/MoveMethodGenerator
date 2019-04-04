package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

import java.util.function.Predicate;

public class AnonymousClassesFilter implements Filter<PsiClass> {
    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        String className = psiClass.getName();
        return className != null && !className.isEmpty();
    }
}
