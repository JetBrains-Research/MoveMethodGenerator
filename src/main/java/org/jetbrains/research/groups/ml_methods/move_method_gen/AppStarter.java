package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.ide.impl.PatchProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.exceptions.UnsupportedDirectoriesLayoutException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.PreprocessingUtils.addAllPossibleSourceRoots;

public class AppStarter implements ApplicationStarter {
    private String projectFolderPath = "";

    private Path outputDir;

    private static final @NotNull Logger log = Logger.getLogger(AppStarter.class);

    @Override
    public String getCommandName() {
        return "generate-dataset";
    }

    @Override
    public void premain(String[] args) {
        if (args == null || args.length != 3) {
            System.err.println("Invalid number of arguments!");
            System.exit(1);
            return;
        }

        projectFolderPath = new File(args[1]).getAbsolutePath().replace(File.separatorChar, '/');

        Path tmp = Paths.get(projectFolderPath);
        outputDir = Paths.get(args[2]).resolve(tmp.getName(tmp.getNameCount() - 1));
    }

    @Override
    public void main(String[] args) {
        String logFileName = outputDir.resolve("log").toString();

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
                    addAllPossibleSourceRoots(project);
                } catch (UnsupportedDirectoriesLayoutException e) {
                    throw new RuntimeException(e);
                }
            });

            doStuff(project, outputDir);
        } catch (Throwable e) {
            log.error("Exception occurred: " + e.getMessage() + " [" + e + "]");
            for (StackTraceElement element : e.getStackTrace()) {
                log.error(element);
            }
        }

        application.exit(true, true);
    }

    private void doStuff(final @NotNull Project project, final @NotNull Path outputDir) throws Exception {
        final Ref<Exception> exceptionRef = new Ref<>(null);
        ApplicationManager.getApplication().runReadAction(
            (Computable<ProjectInfo>) () -> {
                ProjectInfo info = new ProjectInfo(project);

                log.info("Total number of java files: " + info.getAllJavaFiles().size());
                log.info("Total number of source java files: " + info.getSourceJavaFiles().size());
                log.info("Total number of classes: " + info.getClasses().size());
                log.info("Total number of methods: " + info.getMethods().size());
                log.info("Number of methods after filtration: " + info.getMethodsAfterFiltration().size());

                try {
                    CsvSerializer.getInstance().serialize(new Dataset(info), outputDir);
                } catch (Exception e) {
                    exceptionRef.set(e);
                }

                return info;
            }
        );

        if (!exceptionRef.isNull()) {
            throw exceptionRef.get();
        }
    }
}
