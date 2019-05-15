package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractingUtils {
    private ExtractingUtils() { }

    public static @NotNull List<PsiJavaFile> extractAllJavaFiles(final @NotNull Project project) {
        List<PsiJavaFile> javaFiles = new ArrayList<>();

        ProjectFileIndex.SERVICE.getInstance(project).iterateContent(
            virtualFile -> {
                if (!virtualFile.isDirectory() && "java".equals(virtualFile.getExtension())) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

                    if (psiFile == null) {
                        System.err.println("Invalid virtual file encountered!");
                        return true;
                    }

                    if (!(psiFile instanceof PsiJavaFile)) {
                        System.err.println("Not a java file with java extension encountered!");
                        return true;
                    }

                    javaFiles.add((PsiJavaFile) psiFile);
                }

                return true;
            }
        );

        return javaFiles;
    }

    public static @NotNull List<PsiJavaFile> extractSourceJavaFiles(
        final @NotNull Project project
    ) {
        /*
        Possible alternative

        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            VirtualFile[] srcRoots =
                ModuleRootManager.getInstance(module).orderEntries().getAllSourceRoots();

            for (VirtualFile root : srcRoots) {
                // ...
            }
        }*/

        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        return extractAllJavaFiles(project).stream()
                .filter(it -> projectFileIndex.isInSource(it.getVirtualFile()))
                .collect(Collectors.toList());
    }

    public static @NotNull List<PsiClass> extractClasses(
        final @NotNull PsiJavaFile javaFile
    ) {
        return Arrays.stream(javaFile.getClasses()).collect(Collectors.toList());
    }

    public static @NotNull List<PsiClass> extractClasses(
        final @NotNull List<PsiJavaFile> javaFiles
    ) {
        return javaFiles.stream()
            .flatMap(file -> extractClasses(file).stream())
            .collect(Collectors.toList());
    }

    public static @NotNull List<PsiMethod> extractMethods(
        final @NotNull PsiClass psiClass
    ) {
        return Arrays.stream(psiClass.getMethods()).collect(Collectors.toList());
    }

    public static @NotNull List<PsiMethod> extractMethods(
        final @NotNull List<PsiClass> classes
    ) {
        return classes.stream()
            .flatMap(aClass -> extractMethods(aClass).stream())
            .collect(Collectors.toList());
    }
}
