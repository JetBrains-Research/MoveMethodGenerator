package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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

        try (
            BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(CLASSES_FILE_NAME), CREATE_NEW);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180)
        ) {
            List<PsiClass> classes = dataset.getClasses();
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

                csvPrinter.printRecord(
                    methodId,
                    MethodUtils.fullyQualifiedName(method.getPsiMethod()),
                    getPathToContainingFile(method.getPsiMethod()),
                    method.getPsiMethod().getNode().getStartOffset(),
                    method.getIdOfContainingClass(),
                    Arrays.stream(method.getIdsOfPossibleTargets()).mapToObj(Integer::toString).collect(Collectors.joining(" "))
                );
            }
        }
    }

    private @NotNull Path getPathToContainingFile(final @NotNull PsiElement element) {
        return Paths.get(element.getProject().getBasePath()).toAbsolutePath().normalize().relativize(
            Paths.get(element.getContainingFile().getVirtualFile().getCanonicalPath()).toAbsolutePath().normalize()
        );
    }
}
