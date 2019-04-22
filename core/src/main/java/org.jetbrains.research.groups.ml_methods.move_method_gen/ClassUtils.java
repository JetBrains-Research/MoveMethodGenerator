package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class ClassUtils {
    private ClassUtils() {
    }

    public static boolean hasMethodWithName(final @NotNull PsiClass psiClass, final @NotNull String name) {
        for (PsiMethod method : psiClass.getAllMethods()) {
            if (name.equals(method.getName())) {
                return true;
            }
        }

        return false;
    }
}
