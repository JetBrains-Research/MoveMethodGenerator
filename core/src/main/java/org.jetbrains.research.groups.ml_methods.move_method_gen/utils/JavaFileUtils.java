package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class JavaFileUtils {
    private JavaFileUtils() {}

    public static @NotNull Optional<PsiJavaFile> getFileByPath(
        final @NotNull Project project,
        final @NotNull String path
    ) {
        VirtualFile virtualFile = project.getBaseDir().findFileByRelativePath(path);
        if (virtualFile == null) {
            return Optional.empty();
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
}
