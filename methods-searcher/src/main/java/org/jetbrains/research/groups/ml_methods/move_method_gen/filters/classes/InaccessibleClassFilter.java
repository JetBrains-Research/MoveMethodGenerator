package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

public class InaccessibleClassFilter implements Filter<PsiClass> {
    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        return psiClass.hasModifier(JvmModifier.PUBLIC);
    }
}
