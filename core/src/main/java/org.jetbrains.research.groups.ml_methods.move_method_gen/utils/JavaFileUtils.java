package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class JavaFileUtils {
    private JavaFileUtils() {}

    public static @NotNull Optional<PsiJavaFile> getFileByPath(
        final @NotNull Project project,
        final @NotNull String path,
        final boolean refresh
    ) {
        VirtualFile virtualFile = project.getBaseDir().findFileByRelativePath(path);
        if (virtualFile == null) {
            return Optional.empty();
        }

        if (refresh) {
            virtualFile.refresh(false, false);
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return Optional.empty();
        }

        return Optional.of((PsiJavaFile) psiFile);
    }

    public static @NotNull Optional<PsiDirectory> getDirectoryWithRootPackageFor(final @NotNull PsiJavaFile file) {
        String packageName = file.getPackageName();
        String[] packageSequence;

        if ("".equals(packageName)) {
            packageSequence = new String[0];
        } else {
            packageSequence = packageName.split("\\.");
        }

        ArrayUtils.reverse(packageSequence);

        PsiDirectory directory = file.getParent();
        if (directory == null) {
            throw new IllegalStateException("File has no parent directory");
        }

        for (String packagePart : packageSequence) {
            if (!packagePart.equals(directory.getName())) {
                return Optional.empty();
            }

            directory = directory.getParentDirectory();
            if (directory == null) {
                return Optional.empty();
            }
        }

        return Optional.of(directory);
    }

    public static @NotNull Optional<PsiClass> getClassByLocation(
        final @NotNull PsiJavaFile file,
        final @NotNull String className,
        final int classOffset
    ) {
        Ref<PsiClass> resultRef = new Ref<>(null);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitClass(final @NotNull PsiClass aClass) {
                super.visitClass(aClass);

                if (
                    className.equals(aClass.getQualifiedName()) &&
                    classOffset == aClass.getNode().getStartOffset()
                ) {
                    resultRef.set(aClass);
                }
            }
        }.visitElement(file);

        return Optional.ofNullable(resultRef.get());
    }

    public static @NotNull Optional<PsiMethod> getMethodByLocation(
        final @NotNull PsiJavaFile file,
        final @NotNull String methodName,
        final int methodOffset
    ) {
        Ref<PsiMethod> resultRef = new Ref<>(null);

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethod(final @NotNull PsiMethod method) {
                super.visitMethod(method);

                if (
                    methodName.equals(MethodUtils.fullyQualifiedName(method)) &&
                    methodOffset == method.getNode().getStartOffset()
                ) {
                    resultRef.set(method);
                }
            }
        }.visitElement(file);

        return Optional.ofNullable(resultRef.get());
    }

    public static @NotNull Path getPathToContainingFile(final @NotNull PsiElement element) {
        return Paths.get(element.getProject().getBasePath()).toAbsolutePath().normalize().relativize(
                Paths.get(element.getContainingFile().getVirtualFile().getCanonicalPath()).toAbsolutePath().normalize()
        );
    }
}
