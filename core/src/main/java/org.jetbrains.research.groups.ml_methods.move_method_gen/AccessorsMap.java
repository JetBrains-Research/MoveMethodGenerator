package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.whoseGetter;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.MethodUtils.whoseSetter;

public class AccessorsMap {
    private final @NotNull
    Map<PsiField, PsiMethod> fieldToGetter = new HashMap<>();

    private final @NotNull Map<PsiField, PsiMethod> fieldToSetter = new HashMap<>();

    public AccessorsMap(final @NotNull List<PsiMethod> methods) {
        methods.forEach(it -> {
            if (!it.hasModifierProperty(PsiModifier.PUBLIC)) {
                return;
            }

            whoseGetter(it).ifPresent(field -> fieldToGetter.put(field, it));
            whoseSetter(it).ifPresent(field -> fieldToSetter.put(field, it));
        });
    }

    @NotNull
    public Map<PsiField, PsiMethod> getFieldToGetter() {
        return fieldToGetter;
    }

    @NotNull
    public Map<PsiField, PsiMethod> getFieldToSetter() {
        return fieldToSetter;
    }
}
