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
import org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions.InvalidClassException;
import org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions.InvalidCsvInputException;
import org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions.InvalidFileLocationException;
import org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions.InvalidMethodException;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils.getClassByLocation;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils.getMethodByLocation;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils.getPathToContainingFile;

public class CsvSerializer {
    private static final @NotNull CsvSerializer INSTANCE = new CsvSerializer();

    private static final @NotNull String METHODS_FILE_NAME = "methods.csv";

    private static final @NotNull String CLASSES_FILE_NAME = "classes.csv";

    private static final @NotNull CSVFormat CLASSES_FILE_FORMAT = CSVFormat.RFC4180.withHeader("id", "name", "file", "offset");

    private static final @NotNull CSVFormat METHODS_FILE_FORMAT = CSVFormat.RFC4180.withHeader("id", "name", "file", "offset", "containing_class_id", "target_ids");

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
                        CSVPrinter csvPrinter = new CSVPrinter(writer, CLASSES_FILE_FORMAT)
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
                        CSVPrinter csvPrinter = new CSVPrinter(writer, METHODS_FILE_FORMAT)
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

    public @NotNull Dataset deserialize(
        final @NotNull Project project,
        final @NotNull Path dir
    ) throws Exception {
        Ref<Exception> exceptionRef = new Ref<>(null);
        Dataset dataset = ApplicationManager.getApplication().runReadAction(
            (Computable<Dataset>) () -> {
                try {
                    List<PsiClass> classes = new ArrayList<>();

                    try (
                        BufferedReader reader = Files.newBufferedReader(dir.resolve(CLASSES_FILE_NAME));
                    ) {
                        for (CSVRecord record : CSVFormat.RFC4180.withHeader().parse(reader)) {
                            String filePath = record.get("file");
                            Optional<PsiJavaFile> fileOptional = JavaFileUtils.getFileByPath(project, filePath, true);
                            if (!fileOptional.isPresent()) {
                                throw new InvalidFileLocationException(filePath);
                            }

                            PsiJavaFile file = fileOptional.get();
                            String className = record.get("name");

                            String classOffsetString = record.get("offset");
                            int classOffset;
                            try {
                                classOffset = Integer.parseInt(classOffsetString);
                            } catch (NumberFormatException e) {
                                throw new InvalidCsvInputException("Failed to parse class offset. Input: " + classOffsetString);
                            }

                            Optional<PsiClass> classOptional = getClassByLocation(file, className, classOffset);

                            if (!classOptional.isPresent()) {
                                throw new InvalidClassException(className, getPathToContainingFile(file).toString(), classOffset);
                            }

                            classes.add(classOptional.get());
                        }
                    }

                    List<Dataset.Method> methods = new ArrayList<>();
                    try (
                        BufferedReader reader = Files.newBufferedReader(dir.resolve(METHODS_FILE_NAME));
                    ) {
                        for (CSVRecord record : CSVFormat.RFC4180.withHeader().parse(reader)) {
                            String filePath = record.get("file");
                            Optional<PsiJavaFile> fileOptional = JavaFileUtils.getFileByPath(project, filePath, true);
                            if (!fileOptional.isPresent()) {
                                throw new InvalidFileLocationException(filePath);
                            }

                            PsiJavaFile file = fileOptional.get();
                            String methodName = record.get("name");

                            String methodOffsetString = record.get("offset");
                            int methodOffset;
                            try {
                                methodOffset = Integer.parseInt(methodOffsetString);
                            } catch (NumberFormatException e) {
                                throw new InvalidCsvInputException("Failed to parse method offset. Input: " + methodOffsetString);
                            }

                            Optional<PsiMethod> methodOptional = getMethodByLocation(file, methodName, methodOffset);

                            if (!methodOptional.isPresent()) {
                                throw new InvalidMethodException(methodName, getPathToContainingFile(file).toString(), methodOffset);
                            }

                            String idOfContainingClassString = record.get("containing_class_id");
                            int idOfContainingClass;
                            try {
                                idOfContainingClass = Integer.parseInt(idOfContainingClassString);
                            } catch (NumberFormatException e) {
                                throw new InvalidCsvInputException("Failed to parse if of containing class. Input: " + idOfContainingClassString);
                            }

                            int[] idsOfPossibleTargets = Arrays.stream(record.get("target_ids").split(" ")).map(Integer::parseInt).mapToInt(it -> it).toArray();

                            methods.add(new Dataset.Method(project, methodOptional.get(), idOfContainingClass, idsOfPossibleTargets));
                        }
                    }

                    return new Dataset(project, classes, methods);
                } catch (IOException | InvalidCsvInputException exception) {
                    exceptionRef.set(exception);
                }

                return null;
            }
        );

        if (!exceptionRef.isNull()) {
            throw exceptionRef.get();
        }

        return dataset;
    }
}
