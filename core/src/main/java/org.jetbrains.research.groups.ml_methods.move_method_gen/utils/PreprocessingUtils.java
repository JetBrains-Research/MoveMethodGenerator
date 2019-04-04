package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.exceptions.UnsupportedDirectoriesLayoutException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PreprocessingUtils {
    private PreprocessingUtils() {}

    public static void addMainModuleIfLost(
        final @NotNull Project project
    ) {
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206756995-create-module-in-a-project-programmatically

        ModuleManager manager = ModuleManager.getInstance(project);
        Module[] modules = manager.getModules();
        if (modules.length == 0) {
            Module module = manager.newModule(project.getBasePath(), StdModuleTypes.JAVA.getId());

            ModifiableRootModel model =
                    ModuleRootManager.getInstance(module).getModifiableModel();

            model.addContentEntry(project.getBaseDir()).addSourceFolder(project.getBaseDir(), false);
            model.commit();
        }
    }

    public static void addAllPossibleSourceRoots(
        final @NotNull Project project
    ) throws UnsupportedDirectoriesLayoutException {
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206768495-Mark-directory-as-sources-root-from-plugin

        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        List<PsiJavaFile> notSourceFiles = ExtractingUtils.extractAllJavaFiles(project).stream()
                .filter(it -> !projectFileIndex.isInSource(it.getVirtualFile()))
                .collect(Collectors.toList());

        Set<PsiDirectory> javaDirectories = notSourceFiles.stream()
            .map(JavaFileUtils::getDirectoryWithRootPackageFor)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        for (PsiDirectory directory : javaDirectories) {
            if (!"java".equals(directory.getName())) {
                continue;
            }

            PsiDirectory parent = directory.getParentDirectory();
            if (parent == null) {
                throw new UnsupportedDirectoriesLayoutException(directory);
            }

            VirtualFile directoryVF = directory.getVirtualFile();
            Module module = projectFileIndex.getModuleForFile(directoryVF);
            if (module == null) {
                throw new IllegalStateException("Directory does not belong to any module");
            }

            switch (parent.getName()) {
                case "main":
                case "test":
                    ModifiableRootModel model =
                        ModuleRootManager.getInstance(module).getModifiableModel();

                    model.addContentEntry(directoryVF).addSourceFolder(directoryVF, "test".equals(parent.getName()));
                    model.commit();
                    break;

                default:
            }
        }
    }
}
