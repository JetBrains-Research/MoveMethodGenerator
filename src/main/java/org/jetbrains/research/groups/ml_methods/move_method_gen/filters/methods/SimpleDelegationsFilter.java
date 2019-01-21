package org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.getSingleStatementOf;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.isConstExpression;

public class SimpleDelegationsFilter implements Predicate<PsiMethod> {
    @Override
    public boolean test(final @NotNull PsiMethod psiMethod) {
        Set<PsiParameter> parameters =
            Arrays.stream(psiMethod.getParameterList().getParameters())
                .collect(Collectors.toSet());

        Optional<PsiStatement> optionalStatement = getSingleStatementOf(psiMethod);
        if (!optionalStatement.isPresent()) {
            return true;
        }

        PsiStatement theStatement = optionalStatement.get();

        PsiExpression expression;
        if (theStatement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) theStatement;
            expression = returnStatement.getReturnValue();
        } else if (theStatement instanceof PsiExpressionStatement) {
            expression = ((PsiExpressionStatement) theStatement).getExpression();
        } else {
            return true;
        }

        if (expression instanceof PsiMethodCallExpression) {
            return testMethodCall((PsiMethodCallExpression) expression, parameters);
        } else if (expression instanceof PsiNewExpression) {
            return false;

            // Part of alternative version with more detailed check
            // PsiNewExpression newExpression = (PsiNewExpression) expression;
            // return testArguments(newExpression.getArgumentList().getExpressions(), parameters);
        }

        return true;
    }

    private boolean testMethodCall(
        final @NotNull PsiMethodCallExpression methodCall,
        final @NotNull Set<PsiParameter> parameters
    ) {
        PsiExpression qualifierExpression =
            methodCall.getMethodExpression().getQualifierExpression();

        return !isSimpleQualifier(qualifierExpression, parameters);
        // Part of alternative version with more detailed check
        /*if (!isSimpleQualifier(qualifierExpression, parameters)) {
            return true;
        }

        return testArguments(methodCall.getArgumentList().getExpressions(), parameters);
        */
    }

    private boolean testArguments(
        final @NotNull PsiExpression[] argumentExpressions,
        final @NotNull Set<PsiParameter> parameters
    ) {
        for (PsiExpression argument : argumentExpressions) {
            if (
                !isParameter(argument, parameters) &&
                !isConstExpression(argument) &&
                !isField(argument)
            ) {
                return true;
            }
        }

        return false;
    }

    private boolean isTrivialQualifier(final @Nullable PsiExpression qualifierExpression) {
        return qualifierExpression == null || qualifierExpression instanceof PsiThisExpression;
    }

    private boolean isSimpleQualifier(
        final @Nullable PsiExpression qualifierExpression,
        final @NotNull Set<PsiParameter> parameters
    ) {
        if (isTrivialQualifier(qualifierExpression)) {
            return true;
        }

        if (!(qualifierExpression instanceof PsiReferenceExpression)) {
            return false;
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) qualifierExpression;
        if (!isTrivialQualifier(referenceExpression.getQualifierExpression())) {
            return false;
        }

        return true;

        // Part of alternative version with more detailed check
        /*JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        PsiElement element = resolveResult.getElement();

        // todo: might be null. If class is String and it can't be resolved
        return element instanceof PsiClass ||
                parameters.contains(element) ||
                element instanceof PsiField;*/
    }

    private boolean isParameter(
        final @NotNull PsiExpression expression,
        final @NotNull Set<PsiParameter> parameters
    ) {
        if (!(expression instanceof PsiReferenceExpression)) {
            return false;
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) expression;
        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        return parameters.contains(resolveResult.getElement());
    }

    private boolean isField(final @NotNull PsiExpression expression) {
        if (!(expression instanceof PsiReferenceExpression)) {
            return false;
        }

        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) expression;
        if (!isTrivialQualifier(referenceExpression.getQualifierExpression())) {
            return false;
        }

        JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);

        return resolveResult.getElement() instanceof PsiField;
    }
}
