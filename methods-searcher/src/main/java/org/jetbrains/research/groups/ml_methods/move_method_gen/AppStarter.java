package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppStarter extends ProjectAppStarter {
    private Path outputDir;

    @Override
    public String getCommandName() {
        return "generate-dataset";
    }

    @Override
    public void premain(String[] args) {
        super.premain(args);

        if (args == null || args.length != 3) {
            System.err.println("Invalid number of arguments!");
            System.exit(1);
            return;
        }

        Path tmp = Paths.get(projectFolderPath);
        outputDir = Paths.get(args[2]).resolve(tmp.getName(tmp.getNameCount() - 1));

        String logFileName = outputDir.resolve("log").toString();

        try {
            log.addAppender(new FileAppender(new PatternLayout("%d [%p] %m%n"), logFileName));
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + logFileName);
        }
    }

    @Override
    protected void run(@NotNull Project project) throws Exception {
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
                        CsvSerializer.getInstance().serialize(Dataset.createDataset(info), outputDir);
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
