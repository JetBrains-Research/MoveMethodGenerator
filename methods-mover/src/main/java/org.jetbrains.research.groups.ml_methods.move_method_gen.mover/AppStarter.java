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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.AccessorsMap;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectAppStarter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.ExtractingUtils.*;

public class AppStarter extends ProjectAppStarter {
    @Override
    public String getCommandName() {
        return "methods-mover";
    }

    @Override
    public void premain(String[] args) {
        super.premain(args);

        if (args == null || args.length != 2) {
            System.err.println("Invalid number of arguments!");
            System.exit(1);
            return;
        }
    }

    @Override
    protected void run(@NotNull Project project) throws Exception {
        final Ref<SmartPsiElementPointer<PsiMethod>> methodRef = new Ref<>(null);
        final Ref<SmartPsiElementPointer<PsiClass>> classRef = new Ref<>(null);

        ApplicationManager.getApplication().runReadAction(
            () -> {
                List<PsiJavaFile> javaFiles = extractSourceJavaFiles(project);
                List<PsiClass> javaClasses = extractClasses(javaFiles);
                List<PsiMethod> javaMethods = extractMethods(javaClasses);

                PsiMethod method = javaMethods.stream().filter(it -> it.getName().equals("foo")).findFirst().get();
                PsiClass clazz = javaClasses.stream().filter(it -> it.getName().equals("B")).findFirst().get();

                methodRef.set(SmartPointerManager.getInstance(project).createSmartPsiElementPointer(method));
                classRef.set(SmartPointerManager.getInstance(project).createSmartPsiElementPointer(clazz));
            }
        );

        SmartPsiElementPointer<PsiMethod> method = methodRef.get();
        SmartPsiElementPointer<PsiClass> clazz = classRef.get();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                rewriteMethod(method);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        DumbService.getInstance(project).runWhenSmart(
            () -> {
                try {
                    moveMethod(project, method, clazz);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        );
    }

    private void moveMethod(
        final @NotNull Project project,
        final @NotNull SmartPsiElementPointer<PsiMethod> method,
        final @NotNull SmartPsiElementPointer<PsiClass> target
    ) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            // todo: throw
            return;
        }

        PsiClass targetClass = target.getElement();
        if (targetClass == null) {
            // todo: throw
            return;
        }

        System.out.println("Moving " + psiMethod.getName() + " to " + targetClass.getQualifiedName());

        List<PsiVariable> possibleTargetVariables =
            Arrays.stream(psiMethod.getParameterList().getParameters())
                .filter(it -> {
                    PsiType type = it.getType();
                    if (!(type instanceof PsiClassType)) {
                        return false;
                    }

                    return targetClass.equals(((PsiClassType) type).resolve());
                })
                .collect(Collectors.toList());

        if (possibleTargetVariables.isEmpty()) {
            // todo: throw
            return;
        }

        PsiVariable targetVariable = possibleTargetVariables.get(0);
        Map<PsiClass, String> parameterNames = MoveInstanceMethodHandler.suggestParameterNames(psiMethod, targetVariable);

        for (Map.Entry<PsiClass, String> entry : parameterNames.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

        MoveInstanceMethodProcessor moveMethodProcessor =
            new MoveInstanceMethodProcessor(
                project,
                psiMethod,
                targetVariable,
                PsiModifier.PUBLIC,
                parameterNames
            );

        moveMethodProcessor.run();
    }

    // todo: qualifiers, setters, (make static ??)
    private void rewriteMethod(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            // todo: throw
            return;
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

        for (PsiReferenceExpression expression : allReferenceExpressions) {
            Optional<PsiField> optional = MethodUtils.referencedNonPublicField(expression);
            if (!optional.isPresent()) {
                continue;
            }

            PsiField field = optional.get();

            if (MethodUtils.isInLeftSideOfAssignment(expression)) {
                // setter
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
