package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.util.Optional;

public class PrivateClassUserFilter implements Filter<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        final Ref<Boolean> resultRef = new Ref<>(true);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitElement(final @NotNull PsiElement element) {
                super.visitElement(element);

                if (!(element instanceof PsiJavaCodeReferenceElement)) {
                    return;
                }

                PsiJavaCodeReferenceElement reference = (PsiJavaCodeReferenceElement) element;
                PsiElement resolved = reference.resolve();

                if (!(resolved instanceof PsiClass)) {
                    return;
                }

                PsiClass referencedClass = (PsiClass) resolved;
                if (referencedClass.hasModifier(JvmModifier.PRIVATE)) {
                    resultRef.set(false);
                }
            }
        }.visitElement(psiMethod);

        return resultRef.get();
    }
}
