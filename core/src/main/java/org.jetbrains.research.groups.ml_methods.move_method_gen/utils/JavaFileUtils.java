package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class JavaFileUtils {
    private JavaFileUtils() {}

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
