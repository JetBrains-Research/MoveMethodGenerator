package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.AccessorsMap;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MethodRewriter {
    private static final @NotNull MethodRewriter INSTANCE = new MethodRewriter();

    private MethodRewriter() {
    }

    public static @NotNull MethodRewriter getInstance() {
        return INSTANCE;
    }

    public void rewriteMethod(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        encapsulateField(method);
        specifyThisMethods(method);
        addGetClassStatement(method);
    }

    public void postRewriteMethod(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        removeGetClassStatement(method);
    }

    private void encapsulateField(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
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

    private void addGetClassStatement(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            throw new IllegalStateException("Failed to restore method from Smart Pointer: " + method);
        }

        PsiElement firstElement = psiMethod.getBody().getFirstBodyElement();

        PsiStatement getClassStatement =
            PsiElementFactoryImpl.SERVICE.getInstance(method.getProject()).createStatementFromText("this.getClass();", firstElement);

        psiMethod.getBody().addBefore(getClassStatement, firstElement);
    }

    private void removeGetClassStatement(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            throw new IllegalStateException("Failed to restore method from Smart Pointer: " + method);
        }

        psiMethod.getBody().getStatements()[0].delete();
    }

    private void specifyThisMethods(final @NotNull SmartPsiElementPointer<PsiMethod> method) {
        PsiMethod psiMethod = method.getElement();
        if (psiMethod == null) {
            throw new IllegalStateException("Failed to restore method from Smart Pointer: " + method);
        }

        Set<PsiMethod> classMethods = Arrays.stream(psiMethod.getContainingClass().getAllMethods()).collect(Collectors.toSet());
        List<PsiReferenceExpression> methodCalls = new ArrayList<>();
        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(
                final @NotNull PsiMethodCallExpression expression
            ) {
                super.visitMethodCallExpression(expression);

                PsiMethod method = expression.resolveMethod();
                if (
                     method != null &&
                    !expression.getMethodExpression().isQualified() &&
                    classMethods.contains(method)
                ) {
                    methodCalls.add(expression.getMethodExpression());
                }
            }
        }.visitElement(psiMethod);

        // this way the order is from leaves to AST root
        Collections.reverse(methodCalls);

        for (PsiReferenceExpression expression : methodCalls) {
            expression.setQualifierExpression(
                PsiElementFactoryImpl.SERVICE.getInstance(method.getProject())
                        .createExpressionFromText("this", expression)
            );
        }
    }
}
