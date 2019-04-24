package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.impl.PatchProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.exceptions.UnsupportedDirectoriesLayoutException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.PreprocessingUtils.addAllPossibleSourceRoots;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.PreprocessingUtils.addMainModuleIfLost;

public abstract class ProjectAppStarter implements ApplicationStarter {
    protected String projectFolderPath = "";

    protected static final @NotNull Logger log = Logger.getLogger(ProjectAppStarter.class);

    @Override
    public void premain(String[] args) {
        if (args == null || args.length < 2) {
            System.err.println("Invalid number of arguments!");
            System.exit(1);
            return;
        }

        projectFolderPath = new File(args[1]).getAbsolutePath().replace(File.separatorChar, '/');
    }

    @Override
    public void main(String[] args) {
        String logFileName = getOutputDir().resolve("log").toString();

        try {
            log.addAppender(new FileAppender(new PatternLayout("%d [%p] %m%n"), logFileName));
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + logFileName);
        }

        ApplicationEx application = (ApplicationEx) ApplicationManager.getApplication();

        try {
            application.doNotSave();
            Project project = ProjectUtil.openOrImport(
                projectFolderPath,
                null,
                false
            );

            if (project == null) {
                log.error("Unable to open project: " + projectFolderPath);
                System.exit(1);
                return;
            }

            application.runWriteAction(() ->
                VirtualFileManager.getInstance()
                    .refreshWithoutFileWatcher(false)
            );

            PatchProjectUtil.patchProject(project);

            log.info("Project " + projectFolderPath + " is opened");

            application.runWriteAction(() -> {
                try {
                    addMainModuleIfLost(project);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            application.runWriteAction(() -> {
                try {
                    addAllPossibleSourceRoots(project);
                } catch (UnsupportedDirectoriesLayoutException e) {
                    throw new RuntimeException(e);
                }
            });

            Sdk jdk = JavaSdk.getInstance().createJdk("java 1.8", System.getenv("JAVA_HOME"), false);
            ProjectJdkTable.getInstance().addJdk(jdk);
            ProjectRootManager.getInstance(project).setProjectSdk(jdk);
            NewProjectUtil.applyJdkToProject(project, jdk);

            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                ModuleRootModificationUtil.setModuleSdk(module, jdk);
            }

            run(project);
        } catch (Throwable e) {
            log.error("Exception occurred: " + e.getMessage() + " [" + e + "]");
            for (StackTraceElement element : e.getStackTrace()) {
                log.error(element);
            }
        }

        application.exit(true, true);
    }

    protected abstract void run(final @NotNull Project project) throws Exception;

    protected abstract @NotNull Path getOutputDir();
}
