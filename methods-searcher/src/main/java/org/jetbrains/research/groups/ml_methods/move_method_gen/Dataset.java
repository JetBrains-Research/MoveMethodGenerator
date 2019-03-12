package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
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
    private final @NotNull List<SmartPsiElementPointer<PsiClass>> classes;

    private final @NotNull List<Method> methods;

    private Dataset(final @NotNull ProjectInfo projectInfo) {
        List<PsiClass> psiClasses = Stream.concat(
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .flatMap(it -> projectInfo.possibleTargets(it).stream()),
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .map(PsiMember::getContainingClass)
        ).collect(Collectors.toList());

        classes = psiClasses.stream().map(
            it -> SmartPointerManager.getInstance(projectInfo.getProject())
                                     .createSmartPsiElementPointer(it)
        ).collect(Collectors.toList());

        Map<PsiClass, Integer> idOfClass = new HashMap<>();

        int classId = 0;
        for (PsiClass clazz : psiClasses) {
            idOfClass.put(clazz, classId);
            classId++;
        }

        methods =
            projectInfo.getMethodsAfterFiltration()
                .stream()
                .map(it -> new Method(it, projectInfo, idOfClass))
                .collect(Collectors.toList());
    }

    public static @NotNull Dataset createDataset(final @NotNull ProjectInfo projectInfo) {
        return ApplicationManager.getApplication().runReadAction(
            (Computable<Dataset>) () -> new Dataset(projectInfo)
        );
    }

    public List<SmartPsiElementPointer<PsiClass>> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<Method> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public class Method {
        private final @NotNull SmartPsiElementPointer<PsiMethod> psiMethod;

        private final int idOfContainingClass;

        private final @NotNull int[] idsOfPossibleTargets;

        private Method(
            final @NotNull PsiMethod psiMethod,
            final @NotNull ProjectInfo projectInfo,
            final @NotNull Map<PsiClass, Integer> idOfClass
        ) {
            this.psiMethod =
                SmartPointerManager.getInstance(projectInfo.getProject())
                    .createSmartPsiElementPointer(psiMethod);

            idOfContainingClass = idOfClass.get(psiMethod.getContainingClass());

            idsOfPossibleTargets =
                projectInfo.possibleTargets(psiMethod).stream().mapToInt(idOfClass::get).toArray();
        }

        public @NotNull SmartPsiElementPointer<PsiMethod> getPsiMethod() {
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
