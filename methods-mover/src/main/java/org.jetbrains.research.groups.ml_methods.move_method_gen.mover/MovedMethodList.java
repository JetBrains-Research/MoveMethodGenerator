package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovedMethodList {
    private final @NotNull List<Method> list = new ArrayList<>();

    public synchronized void addMethod(
        final @NotNull SmartPsiElementPointer<PsiMethod> method,
        final @NotNull SmartPsiElementPointer<PsiClass> originalClass
    ) {
        list.add(new Method(method, originalClass));
    }

    public @NotNull List<Method> getList() {
        return Collections.unmodifiableList(list);
    }

    public class Method {
        private final @NotNull SmartPsiElementPointer<PsiMethod> method;

        private final @NotNull SmartPsiElementPointer<PsiClass> originalClass;

        private Method(
            final @NotNull SmartPsiElementPointer<PsiMethod> method,
            final @NotNull SmartPsiElementPointer<PsiClass> originalClass
        ) {
            this.method = method;
            this.originalClass = originalClass;
        }

        public @NotNull SmartPsiElementPointer<PsiMethod> getMethod() {
            return method;
        }

        public @NotNull SmartPsiElementPointer<PsiClass> getOriginalClass() {
            return originalClass;
        }
    }
}
