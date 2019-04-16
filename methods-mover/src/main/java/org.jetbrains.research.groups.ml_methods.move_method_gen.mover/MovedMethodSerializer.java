package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPsiElementPointer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.Dataset;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.JavaFileUtils.getPathToContainingFile;

public class MovedMethodSerializer {
    private static final @NotNull MovedMethodSerializer INSTANCE = new MovedMethodSerializer();

    private static final @NotNull String FILE_NAME = "moved-methods.csv";

    private static final @NotNull CSVFormat FILE_FORMAT = CSVFormat.RFC4180.withHeader("id", "name", "file", "offset", "original_class_id", "target_class_id");

    private MovedMethodSerializer() {
    }

    public static @NotNull MovedMethodSerializer getInstance() {
        return INSTANCE;
    }

    public void serialize(
        final @NotNull MovedMethodList list,
        final @NotNull Path targetDir
    ) throws IOException {
        Ref<IOException> exceptionRef = new Ref<>(null);
        ApplicationManager.getApplication().runReadAction(
            () -> {
                try (
                    BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(FILE_NAME), CREATE_NEW);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, FILE_FORMAT)
                ) {
                    for (MovedMethodList.Method method : list.getList()) {
                        PsiMethod psiMethod = method.getMethod().getElement();

                        csvPrinter.printRecord(
                            method.getMethodId(),
                            MethodUtils.fullyQualifiedName(psiMethod),
                            getPathToContainingFile(psiMethod),
                            psiMethod.getNode().getStartOffset(),
                            method.getOriginalClassId(),
                            method.getTargetClassId()
                        );
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
}
