package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodHandler;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.AccessorsMap;
import org.jetbrains.research.groups.ml_methods.move_method_gen.CsvSerializer;
import org.jetbrains.research.groups.ml_methods.move_method_gen.Dataset;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectAppStarter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

        log.addAppender(new ConsoleAppender(new PatternLayout("%d [%p] %m%n")));
    }

    @Override
    protected void run(@NotNull Project project) throws Exception {
        Dataset dataset = CsvSerializer.getInstance().deserialize(project, csvFilesDir);

        MovedMethodList movedMethods = new MovedMethodList();

        int methodId = 0;
        for (Dataset.Method method : dataset.getMethods()) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    rewriteMethod(method.getPsiMethod());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });

            final int capturedMethodId = methodId;
            DumbService.getInstance(project).runWhenSmart(
                () -> {
                    try {
                        int targetClassId = method.getIdsOfPossibleTargets()[0];
                        SmartPsiElementPointer<PsiClass> targetClass = dataset.getClasses().get(targetClassId);
                        movedMethods.addMethod(moveMethod(project, method.getPsiMethod(), targetClass), capturedMethodId, method.getIdOfContainingClass(), targetClassId);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            );

            ++methodId;
        }

        MovedMethodSerializer.getInstance().serialize(movedMethods, csvFilesDir);
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

        System.out.println("Moving " + psiMethod.getName() + " to " + targetClass.getQualifiedName());

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
            throw new IllegalStateException("Failed to find moved method: " + psiMethod.getName());
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
