package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.ide.impl.PatchProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.exceptions.UnsupportedDirectoriesLayoutException;

import java.io.File;

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

        // Path tmp = Paths.get(projectFolderPath);
        // outputDir = Paths.get(args[2]).resolve(tmp.getName(tmp.getNameCount() - 1));

        // String logFileName = outputDir.resolve("log").toString();

        /*try {
            log.addAppender(new FileAppender(new PatternLayout("%d [%p] %m%n"), logFileName));
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + logFileName);
        }*/
    }

    @Override
    public void main(String[] args) {
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

            run(project);
            // doStuff(project, outputDir);
        } catch (Throwable e) {
            log.error("Exception occurred: " + e.getMessage() + " [" + e + "]");
            for (StackTraceElement element : e.getStackTrace()) {
                log.error(element);
            }
        }

        application.exit(true, true);
    }

    protected abstract void run(final @NotNull Project project) throws Exception;
}
