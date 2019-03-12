package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

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
                !actualClass.equals(method.getContainingClass())
            ) {
                targets.add(actualClass);
            }
        }

        return targets;
    }
}
