package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Program dataset representation which might be used by someone who wants to perform
 * modifications on resulting dataset during runtime.
 */
public class Dataset {
    private final @NotNull List<SmartPsiElementPointer<PsiClass>> classes;

    private final @NotNull List<Method> methods;

    private Dataset(
        final @NotNull Project project,
        final @NotNull RelevantClasses relevantClasses,
        final @NotNull List<PsiMethod> relevantMethods
    ) {
        Set<PsiClass> psiClasses = Stream.concat(
            relevantMethods.stream()
                .flatMap(it -> relevantClasses.possibleTargets(it).stream()),
            relevantMethods.stream()
                .map(PsiMember::getContainingClass)
        ).collect(Collectors.toSet());

        classes = psiClasses.stream().map(
            it -> SmartPointerManager.getInstance(project)
                                     .createSmartPsiElementPointer(it)
        ).collect(Collectors.toList());

        Map<PsiClass, Integer> idOfClass = new HashMap<>();

        int classId = 0;
        for (PsiClass clazz : psiClasses) {
            idOfClass.put(clazz, classId);
            classId++;
        }

        methods =
            relevantMethods.stream()
                .map(it -> new Method(project, it, relevantClasses, idOfClass))
                .collect(Collectors.toList());
    }

    public Dataset(
        final @NotNull Project project,
        final @NotNull List<PsiClass> classes,
        final @NotNull List<Method> methods
    ) {
        this.classes = classes.stream()
            .map(it -> SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it))
            .collect(Collectors.toList());

        this.methods = methods;
    }

    public static @NotNull Dataset createDataset(
        final @NotNull Project project,
        final @NotNull RelevantClasses relevantClasses,
        final @NotNull List<PsiMethod> relevantMethods
    ) {
        return ApplicationManager.getApplication().runReadAction(
            (Computable<Dataset>) () -> new Dataset(project, relevantClasses, relevantMethods)
        );
    }

    public List<SmartPsiElementPointer<PsiClass>> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public List<Method> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public static class Method {
        private final @NotNull SmartPsiElementPointer<PsiMethod> psiMethod;

        private final int idOfContainingClass;

        private final @NotNull int[] idsOfPossibleTargets;

        private Method(
            final @NotNull Project project,
            final @NotNull PsiMethod psiMethod,
            final @NotNull RelevantClasses relevantClasses,
            final @NotNull Map<PsiClass, Integer> idOfClass
        ) {
            this.psiMethod =
                SmartPointerManager.getInstance(project)
                    .createSmartPsiElementPointer(psiMethod);

            idOfContainingClass = idOfClass.get(psiMethod.getContainingClass());

            idsOfPossibleTargets =
                relevantClasses.possibleTargets(psiMethod).stream().mapToInt(idOfClass::get).toArray();
        }

        public Method(
            final @NotNull Project project,
            final @NotNull PsiMethod method,
            final int idOfContainingClass,
            final @NotNull int[] idsOfPossibleTargets
        ) {
            this.psiMethod = SmartPointerManager.getInstance(project)
                .createSmartPsiElementPointer(method);

            this.idOfContainingClass = idOfContainingClass;
            this.idsOfPossibleTargets = idsOfPossibleTargets;
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
