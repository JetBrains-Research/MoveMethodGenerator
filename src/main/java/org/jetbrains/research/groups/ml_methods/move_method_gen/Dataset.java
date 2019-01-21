package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Program dataset representation which might be used by someone who wants to perform
 * modifications on resulting dataset during runtime.
 */
public class Dataset {
    private final @NotNull List<PsiClass> classes;

    private final @NotNull List<Method> methods;

    private final @NotNull Map<PsiClass, Integer> idOfClass = new HashMap<>();

    public Dataset(final @NotNull ProjectInfo projectInfo) {
        classes = Stream.concat(
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .flatMap(it -> projectInfo.possibleTargets(it).stream()),
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .map(PsiMember::getContainingClass)
        ).collect(Collectors.toList());

        int classId = 0;
        for (PsiClass clazz : classes) {
            idOfClass.put(clazz, classId);
            classId++;
        }

        methods =
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .map(it -> new Method(it, projectInfo))
                .collect(Collectors.toList());
    }

    public List<PsiClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<Method> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public class Method {
        private final @NotNull PsiMethod psiMethod;

        private final int idOfContainingClass;

        private final @NotNull int[] idsOfPossibleTargets;

        public Method(final @NotNull PsiMethod psiMethod, final @NotNull ProjectInfo projectInfo) {
            this.psiMethod = psiMethod;

            idOfContainingClass = idOfClass.get(psiMethod.getContainingClass());

            idsOfPossibleTargets =
                projectInfo.possibleTargets(psiMethod).stream().mapToInt(idOfClass::get).toArray();
        }

        public @NotNull PsiMethod getPsiMethod() {
            return psiMethod;
        }

        public int getIdOfContainingClass() {
            return idOfContainingClass;
        }

        public @NotNull int[] getIdsOfPossibleTargets() {
            return idsOfPossibleTargets;
        }
    }
}
