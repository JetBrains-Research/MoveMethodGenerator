package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class AnnotationTypesFilter implements Predicate<PsiClass> {
    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        return !psiClass.isAnnotationType();
    }
}
