package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class CsvSerializer {
    private static final @NotNull CsvSerializer INSTANCE = new CsvSerializer();

    private static final @NotNull String METHODS_FILE_NAME = "methods.csv";

    private static final @NotNull String CLASSES_FILE_NAME = "classes.csv";

    private CsvSerializer() {
    }

    public static @NotNull CsvSerializer getInstance() {
        return INSTANCE;
    }

    public void serialize(
        final @NotNull Dataset dataset,
        final @NotNull Path targetDir
    ) throws IOException {
        targetDir.toFile().mkdirs();

        Ref<IOException> exceptionRef = new Ref<>(null);
        ApplicationManager.getApplication().runReadAction(
            () -> {
                try {
                    try (
                        BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(CLASSES_FILE_NAME), CREATE_NEW);
                        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180)
                    ) {
                        List<PsiClass> classes = dataset.getClasses().stream().map(SmartPsiElementPointer::getElement).collect(Collectors.toList());
                        for (int classId = 0; classId < classes.size(); classId++) {
                            PsiClass clazz = classes.get(classId);

                            csvPrinter.printRecord(
                                classId,
                                clazz.getQualifiedName(),
                                getPathToContainingFile(clazz),
                                clazz.getNode().getStartOffset()
                            );
                        }
                    }

                    try (
                        BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(METHODS_FILE_NAME), CREATE_NEW);
                        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180)
                    ) {
                        List<Dataset.Method> methods = dataset.getMethods();
                        for (int methodId = 0; methodId < methods.size(); methodId++) {
                            Dataset.Method method = methods.get(methodId);

                            PsiMethod psiMethod = method.getPsiMethod().getElement();

                            csvPrinter.printRecord(
                                methodId,
                                MethodUtils.fullyQualifiedName(psiMethod),
                                getPathToContainingFile(psiMethod),
                                psiMethod.getNode().getStartOffset(),
                                method.getIdOfContainingClass(),
                                Arrays.stream(method.getIdsOfPossibleTargets()).mapToObj(Integer::toString).collect(Collectors.joining(" "))
                            );
                        }
                    }
                } catch (IOException exception) {
                    exceptionRef.set(exception);
                }
            }
        );

        if (!exceptionRef.isNull()) {
            throw exceptionRef.get();
        }
    }

    public void deserialize(
        final @NotNull Project project,
        final @NotNull Path dir
    ) throws IOException {
        Ref<IOException> exceptionRef = new Ref<>(null);
        ApplicationManager.getApplication().runReadAction(
            () -> {
                try {
                    try (
                        BufferedReader reader = Files.newBufferedReader(dir.resolve(CLASSES_FILE_NAME));
                    ) {
                        for (CSVRecord record : CSVFormat.RFC4180.parse(reader)) {
                            Optional<PsiJavaFile> optional = JavaFileUtils.getFileByPath(project, record.get(2));
                            System.out.println(optional.isPresent());
                        }
                    }
                } catch (IOException exception) {
                    exceptionRef.set(exception);
                }
            }
        );

        if (!exceptionRef.isNull()) {
            throw exceptionRef.get();
        }
    }

    private @NotNull Path getPathToContainingFile(final @NotNull PsiElement element) {
        return Paths.get(element.getProject().getBasePath()).toAbsolutePath().normalize().relativize(
            Paths.get(element.getContainingFile().getVirtualFile().getCanonicalPath()).toAbsolutePath().normalize()
        );
    }
}