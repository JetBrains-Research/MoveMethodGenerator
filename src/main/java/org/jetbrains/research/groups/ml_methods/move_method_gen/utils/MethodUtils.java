package org.jetbrains.research.groups.ml_methods.move_method_gen.utils;

import com.intellij.psi.*;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MethodUtils {
    private static final @NotNull Logger LOGGER = Logger.getLogger(MethodUtils.class);

    static {
        LOGGER.addAppender(new ConsoleAppender(new PatternLayout("%p [%c.%M] - %m%n")));
    }

    private MethodUtils() { }

    public static boolean isConstExpression(final @NotNull PsiExpression expression) {
        return expression instanceof PsiLiteralExpression ||
               expression instanceof PsiThisExpression ||
               expression instanceof PsiClassObjectAccessExpression ||
               expression instanceof PsiMethodReferenceExpression;
    }

    public static @NotNull Optional<PsiStatement> getSingleStatementOf(
        final @NotNull PsiMethod method
    ) {
        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Optional.empty();
        }

        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return Optional.empty();
        }

        return Optional.of(statements[0]);
    }

    public static @NotNull Optional<PsiField> whoseSetter(final @NotNull PsiMethod method) {
        if (method.isConstructor()) {
            return Optional.empty();
        }

        if (method.getParameterList().getParametersCount() != 1) {
            return Optional.empty();
        }

        PsiParameter parameter = method.getParameterList().getParameters()[0];
        PsiType parameterType = parameter.getType();
        // todo: need to check that parameter type is not just a subclass of actual field type

        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Optional.empty();
        }

        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return Optional.empty();
        }

        PsiStatement theStatement = statements[0];
        if (!(theStatement instanceof PsiExpressionStatement)) {
            return Optional.empty();
        }

        PsiExpression expression = ((PsiExpressionStatement) theStatement).getExpression();
        if (!(expression instanceof PsiAssignmentExpression)) {
            return Optional.empty();
        }

        PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) expression;
        PsiExpression leftExpression = assignmentExpression.getLExpression();
        PsiExpression rightExpression = assignmentExpression.getRExpression();

        if (!(leftExpression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiField field = getReferencedField((PsiReferenceExpression) leftExpression);
        if (field == null) {
            return Optional.empty();
        }

        if (!(rightExpression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) rightExpression;
        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        if (resolveResult.getElement() != parameter) {
            return Optional.empty();
        }

        return Optional.of(field);
    }

    public static @NotNull Optional<PsiField> whoseGetter(final @NotNull PsiMethod method) {
        if (method.getParameterList().getParametersCount() != 0) {
            return Optional.empty();
        }

        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return Optional.empty();
        }

        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return Optional.empty();
        }

        PsiStatement theStatement = statements[0];
        if (!(theStatement instanceof PsiReturnStatement)) {
            return Optional.empty();
        }

        PsiReturnStatement returnStatement = (PsiReturnStatement) theStatement;
        PsiExpression expression = returnStatement.getReturnValue();

        if (!(expression instanceof PsiReferenceExpression)) {
            return Optional.empty();
        }

        PsiField field = getReferencedField((PsiReferenceExpression) expression);
        if (field == null) {
            return Optional.empty();
        }

        PsiClass fieldClass = field.getContainingClass();
        if (fieldClass == null || !fieldClass.equals(method.getContainingClass())) { // what if it's just a super class?
            return Optional.empty();
        }

        if (!field.getType().equals(method.getReturnType())) {
            return Optional.empty();
        }

        return Optional.of(field);
    }

    public static @NotNull String fullyQualifiedName(final @NotNull PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();

        String className = "";
        if (containingClass != null) {
            className = containingClass.getQualifiedName();
            if (className == null) {
                className = containingClass.getName();
            }

            if (className == null) {
                className = "";
            }
        }

        return className + '.' + method.getName();
    }

    private static @Nullable PsiField getReferencedField(
        final @NotNull PsiReferenceExpression referenceExpression
    ) {
        if (referenceExpression.isQualified()) {
            PsiExpression qualifierExpression = referenceExpression.getQualifierExpression();
            if (!(qualifierExpression instanceof PsiThisExpression)) {
                return null;
            }
        }

        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        PsiElement referencedElement = resolveResult.getElement();
        if (referencedElement == null) {
            return null;
        }

        if (!(referencedElement instanceof PsiField)) {
            return null;
        }

        return (PsiField) referencedElement;
    }

    public static @NotNull Optional<PsiField> referencedNonPublicField(
        final @NotNull PsiReferenceExpression expression
    ) {
        JavaResolveResult resolveResult = expression.advancedResolve(false);
        PsiElement referencedElement = resolveResult.getElement();
        if (referencedElement == null) {
            return Optional.empty();
        }

        if (!(referencedElement instanceof PsiField)) {
            return Optional.empty();
        }

        PsiField field = (PsiField) referencedElement;

        if (field.hasModifierProperty(PsiModifier.PUBLIC)) {
            return Optional.empty();
        }

        return Optional.of(field);
    }

    public static boolean isInLeftSideOfAssignment(
        final @NotNull PsiReferenceExpression expression
    ) {
        PsiElement parent = expression.getParent();
        if (!(parent instanceof PsiAssignmentExpression)) {
            return false;
        }

        PsiAssignmentExpression assignment = (PsiAssignmentExpression) parent;
        return expression.equals(assignment.getLExpression());
    }
}
