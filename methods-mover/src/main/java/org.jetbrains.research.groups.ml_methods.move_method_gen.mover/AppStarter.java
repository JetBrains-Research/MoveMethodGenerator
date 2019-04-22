package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodHandler;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor;
import com.intellij.refactoring.rename.RenameProcessor;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.AccessorsMap;
import org.jetbrains.research.groups.ml_methods.move_method_gen.CsvSerializer;
import org.jetbrains.research.groups.ml_methods.move_method_gen.Dataset;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectAppStarter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.ClassUtils.hasMethodWithName;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.fullyQualifiedName;

public class AppStarter extends ProjectAppStarter {
    private Path csvFilesDir;

    @Override
    public String getCommandName() {
        return "methods-mover";
    }

    @Override
    public void premain(String[] args) {
        super.premain(args);

        if (args == null || args.length != 3) {
            System.err.println("Invalid number of arguments!");
            System.exit(1);
            return;
        }

        csvFilesDir = Paths.get(args[2]);
    }

    @Override
    protected void run(@NotNull Project project) throws Exception {
        Dataset dataset = CsvSerializer.getInstance().deserialize(project, csvFilesDir);
        int potentialMoves = dataset.getMethods().stream().mapToInt(it -> it.getIdsOfPossibleTargets().length).sum();

        List<SmartPsiElementPointer<PsiClass>> classes = dataset.getClasses();
        List<Dataset.Method> methods = dataset.getMethods();

        List<MethodToMove> methodsToMove = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            Set<PsiClass> usedClasses = new HashSet<>();

            int methodId = 0;
            for (Dataset.Method method : dataset.getMethods()) {
                PsiMethod psiMethod = method.getPsiMethod().getElement();
                if (usedClasses.contains(psiMethod.getContainingClass())) {
                    ++methodId;
                    continue;
                }

                int targetId = -1;
                for (int possibleTargetId : method.getIdsOfPossibleTargets()) {
                    PsiClass targetClass = classes.get(possibleTargetId).getElement();
                    if (!usedClasses.contains(targetClass)) {
                        targetId = possibleTargetId;
                        break;
                    }
                }

                if (targetId != -1) {
                    usedClasses.add(psiMethod.getContainingClass());
                    usedClasses.add(classes.get(targetId).getElement());
                    methodsToMove.add(new MethodToMove(methodId, targetId));
                }

                ++methodId;
            }
        });

        MovedMethodList movedMethods = new MovedMethodList();

        for (MethodToMove methodToMove : methodsToMove) {
            Ref<Exception> exceptionRef = new Ref<>(null);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    rewriteMethod(methods.get(methodToMove.getMethodId()).getPsiMethod());
                } catch (Exception e) {
                    exceptionRef.set(e);
                }
            });

            if (!exceptionRef.isNull()) {
                throw exceptionRef.get();
            }

            DumbService.getInstance(project).runWhenSmart(
                () -> {
                    try {
                        int methodId = methodToMove.getMethodId();
                        int targetClassId = methodToMove.getTargetClassId();
                        SmartPsiElementPointer<PsiClass> targetClass = classes.get(targetClassId);
                        SmartPsiElementPointer<PsiMethod> psiMethod = methods.get(methodToMove.getMethodId()).getPsiMethod();
                        movedMethods.addMethod(moveMethod(project, psiMethod, targetClass), methodId, methods.get(methodId).getIdOfContainingClass(), targetClassId);
                    } catch (Exception e) {
                        exceptionRef.set(e);
                    }
                }
            );

            if (!exceptionRef.isNull()) {
                throw exceptionRef.get();
            }
        }

        MovedMethodSerializer.getInstance().serialize(movedMethods, csvFilesDir);

        log.info(potentialMoves + " potential moves found");
        log.info(movedMethods.getList().size() + " moves performed");
    }

    @Override
    protected @NotNull Path getOutputDir() {
        return csvFilesDir;
    }

    private void renameMethod(
        final @NotNull Project project,
        final @NotNull PsiMethod method,
        final @NotNull String newName
    ) {
        RenameProcessor renameProcessor = new RenameProcessor(project, method, newName, false, false);
        renameProcessor.run();
    }

    private @NotNull SmartPsiElementPointer<PsiMethod> moveMethod(
        final @NotNull Project project,
        final @NotNull SmartPsiElementPointer<PsiMethod> method,
        final @NotNull SmartPsiElementPointer<PsiClass> target
    ) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            throw new IllegalStateException("Failed to restore method from Smart Pointer: " + method);
        }

        PsiClass targetClass = target.getElement();
        if (targetClass == null) {
            throw new IllegalStateException("Failed to restore class from Smart Pointer: " + target);
        }

        if (hasMethodWithName(targetClass, psiMethod.getName())) {
            String newName = psiMethod.getName();
            while (hasMethodWithName(targetClass, newName)) {
                newName += "Other";
            }

            renameMethod(project, psiMethod, newName);
        }

        List<PsiVariable> possibleTargetVariables =
            Arrays.stream(psiMethod.getParameterList().getParameters())
                .filter(it -> {
                    PsiType type = it.getType();
                    return type instanceof PsiClassType && targetClass.equals(((PsiClassType) type).resolve());

                })
                .collect(Collectors.toList());

        if (possibleTargetVariables.isEmpty()) {
            throw new IllegalStateException("No appropriate variable to perform move method is found: " + fullyQualifiedName(psiMethod) + " is moving to " + targetClass.getQualifiedName());
        }

        PsiVariable targetVariable = possibleTargetVariables.get(0);
        Map<PsiClass, String> parameterNames = MoveInstanceMethodHandler.suggestParameterNames(psiMethod, targetVariable);

        MoveInstanceMethodProcessor moveMethodProcessor =
            new MoveInstanceMethodProcessor(
                project,
                psiMethod,
                targetVariable,
                PsiModifier.PUBLIC,
                parameterNames
            );

        moveMethodProcessor.run();

        List<PsiMethod> candidates = new ArrayList<>();
        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethod(final @NotNull PsiMethod aMethod) {
                super.visitMethod(aMethod);

                if (aMethod.getName().equals(psiMethod.getName())) {
                    candidates.add(aMethod);
                }
            }
        }.visitElement(targetClass);

        if (candidates.size() != 1) {
            throw new IllegalStateException("Failed to find moved method: " + psiMethod.getName() + "; Method was moved to " + targetClass.getQualifiedName());
        }

        return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(candidates.get(0));
    }

    // todo: qualifiers
    private void rewriteMethod(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            throw new IllegalStateException("Failed to restore method from Smart Pointer: " + method);
        }

        AccessorsMap accessorsMap = new AccessorsMap(
            Arrays.stream(psiMethod.getContainingClass().getAllMethods()).collect(Collectors.toList())
        );

        List<PsiReferenceExpression> allReferenceExpressions = new ArrayList<>();

        new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceExpression(
                final @NotNull PsiReferenceExpression expression
            ) {
                super.visitReferenceExpression(expression);
                allReferenceExpressions.add(expression);
            }
        }.visitElement(psiMethod);

        // this way the order is from leaves to AST root
        Collections.reverse(allReferenceExpressions);

        for (PsiReferenceExpression expression : allReferenceExpressions) {
            Optional<PsiField> optional = MethodUtils.referencedNonPublicField(expression);
            if (!optional.isPresent()) {
                continue;
            }

            PsiField field = optional.get();

            if (MethodUtils.isInLeftSideOfAssignment(expression)) {
                // setter

                PsiMethod setter = accessorsMap.getFieldToSetter().get(field);

                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression.getParent();
                PsiExpression setterCallExpression =
                        PsiElementFactoryImpl.SERVICE.getInstance(method.getProject())
                            .createExpressionFromText(setter.getName() + "(" + assignment.getRExpression().getText()  + ")", expression);

                assignment.replace(setterCallExpression);
            } else {
                // getter

                PsiMethod getter = accessorsMap.getFieldToGetter().get(field);
                PsiExpression getterCallExpression =
                        PsiElementFactoryImpl.SERVICE.getInstance(method.getProject())
                                .createExpressionFromText(getter.getName() + "()", expression);

                expression.replace(getterCallExpression);
            }
        }
    }
}
