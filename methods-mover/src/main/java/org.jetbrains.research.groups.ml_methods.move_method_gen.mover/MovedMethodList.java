package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

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
        final int methodId,
        final int originalClassId,
        final int targetClassId
    ) {
        list.add(new Method(method, methodId, originalClassId, targetClassId));
    }

    public @NotNull List<Method> getList() {
        return Collections.unmodifiableList(list);
    }

    public class Method {
        private final @NotNull SmartPsiElementPointer<PsiMethod> method;

        private final int methodId;

        private final int originalClassId;

        private final int targetClassId;

        private Method(
            final @NotNull SmartPsiElementPointer<PsiMethod> method,
            final int methodId,
            final int originalClassId,
            final int targetClassId
        ) {
            this.method = method;
            this.methodId = methodId;
            this.originalClassId = originalClassId;
            this.targetClassId = targetClassId;
        }

        public @NotNull SmartPsiElementPointer<PsiMethod> getMethod() {
            return method;
        }

        public int getMethodId() {
            return methodId;
        }

        public int getOriginalClassId() {
            return originalClassId;
        }

        public int getTargetClassId() {
            return targetClassId;
        }
    }
}
