package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

import java.util.function.Predicate;

public class OverriddenMethodsFilter implements Filter<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        final PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
            throw new IllegalStateException();
        }

        final Query<PsiMethod> query = OverridingMethodsSearch.search(psiMethod);
        return query.findFirst() == null;
    }
}
