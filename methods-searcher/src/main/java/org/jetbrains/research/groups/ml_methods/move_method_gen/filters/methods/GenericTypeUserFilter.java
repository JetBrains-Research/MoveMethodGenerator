package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;

public class GenericTypeUserFilter implements Filter<PsiMethod> {
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

                if (!(resolved instanceof PsiTypeParameter)) {
                    return;
                }

                PsiTypeParameter typeParameter = (PsiTypeParameter) resolved;
                PsiTypeParameterListOwner owner = typeParameter.getOwner();
                if (!psiMethod.equals(owner)) {
                    resultRef.set(false);
                }
            }
        }.visitElement(psiMethod);

        return resultRef.get();
    }
}
