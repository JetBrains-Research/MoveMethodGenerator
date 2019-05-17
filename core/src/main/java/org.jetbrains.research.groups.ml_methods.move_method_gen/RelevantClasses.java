package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelevantClasses {
    private final @NotNull Set<PsiClass> classesSet;

    public RelevantClasses(final @NotNull List<PsiClass> classes) {
        classesSet = new HashSet<>(classes);
    }

    public @NotNull Set<PsiClass> possibleTargets(final @NotNull PsiMethod method) {
        Set<PsiClass> targets = new HashSet<>();

        Module methodModule = ModuleUtil.findModuleForFile(method.getContainingFile());

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            PsiType type = parameter.getType();
            if (!(type instanceof PsiClassType)) {
                continue;
            }

            PsiClassType classType = (PsiClassType) type;
            PsiClass actualClass = classType.resolve();

            if (
                actualClass != null &&
                classesSet.contains(actualClass) &&
                !actualClass.equals(method.getContainingClass()) &&
                methodModule.equals(ModuleUtil.findModuleForFile(actualClass.getContainingFile())) &&
                isCandidate(method, parameter)
            ) {
                targets.add(actualClass);
            }
        }

        return targets;
    }

    private boolean isCandidate(final @NotNull PsiMethod method, final @NotNull PsiParameter parameter) {
        Ref<Boolean> result = new Ref<>(true);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceExpression(
                final @NotNull PsiReferenceExpression expression
            ) {
                super.visitReferenceExpression(expression);

                if (!MethodUtils.isInLeftSideOfAssignment(expression)) {
                    return;
                }

                JavaResolveResult resolveResult = expression.advancedResolve(false);
                PsiElement referencedElement = resolveResult.getElement();

                if (referencedElement == parameter) {
                    result.set(false);
                }
            }
        }.visitElement(method);

        return result.get();
    }
}
