package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes;

import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils.getDirectoryWithRootPackageFor;

public class TestsFilter implements Predicate<PsiClass> {
    @Override
    public boolean test(final @NotNull PsiClass psiClass) {
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (TestSourcesFilter.isTestSources(virtualFile, psiClass.getProject())) {
            return false;
        }

        PsiJavaFile file = (PsiJavaFile) psiClass.getContainingFile();
        if (file != null && (!testFile(file) || !testPackage(file))) {
            return false;
        }

        return testClass(psiClass);
    }

    private boolean testClass(final @NotNull PsiClass psiClass) {
        if (!testClassName(psiClass)) {
            return false;
        }

        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass == null) {
            return true;
        }

        return testClass(containingClass);
    }

    private boolean testClassName(final @NotNull PsiClass psiClass) {
        String className = psiClass.getName();
        if (className == null) {
            return true;
        }

        className = className.toLowerCase();
        return !className.endsWith("test") && !className.endsWith("tests");
    }

    private boolean testFile(final @NotNull PsiJavaFile file) {
        Optional<PsiDirectory> optionalDirectory = getDirectoryWithRootPackageFor(file);

        if (!optionalDirectory.isPresent()) {
            return false;
        }

        PsiDirectory directory = optionalDirectory.get();

        while (directory != null) {
            String dirName = directory.getName().toLowerCase();
            if (dirName.equals("test") || dirName.equals("tests") || dirName.equals("jmh")) {
                return false;
            }

            directory = directory.getParentDirectory();
        }

        return true;
    }

    private boolean testPackage(final @NotNull PsiJavaFile file) {
        String packageName = file.getPackageName();
        String[] packageSequence;

        if ("".equals(packageName)) {
            packageSequence = new String[0];
        } else {
            packageSequence = packageName.split("\\.");
        }

        for (String packageElement : packageSequence) {
            String str = packageElement.toLowerCase();
            if (str.equals("test") || str.equals("tests")) {
                return false;
            }
        }

        return true;
    }
}
