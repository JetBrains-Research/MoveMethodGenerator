package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.classes.*;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.methods.*;
import org.jetbrains.research.groups.ml_methods.move_method_gen.utils.ExtractingUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectInfo {
    private final @NotNull Project project;

    private final @NotNull List<PsiJavaFile> allJavaFiles;

    private final @NotNull List<PsiJavaFile> sourceJavaFiles;

    private final @NotNull List<PsiClass> classes;

    private final @NotNull List<PsiMethod> methods;

    private final @NotNull List<PsiMethod> methodsAfterFiltration;

    private final @NotNull AccessorsMap accessorsMap;

    public ProjectInfo(final @NotNull Project project) {
        this.project = project;

        allJavaFiles = ExtractingUtils.extractAllJavaFiles(project);
        sourceJavaFiles = ExtractingUtils.extractSourceJavaFiles(project);

        classes = ExtractingUtils.extractClasses(sourceJavaFiles)
                .stream()
                .filter(new TypeParametersFilter())
                .filter(new InterfacesFilter())
                .filter(new AnnotationTypesFilter())
                .filter(new TestsFilter())
                .filter(new BuildersFilter())
                .filter(new EmptyClassesFilter())
                .collect(Collectors.toList());

        methods = ExtractingUtils.extractMethods(classes);

        accessorsMap = new AccessorsMap(methods);

        methodsAfterFiltration =
            methods.stream()
                .filter(new ConstructorsFilter())
                .filter(new AbstractMethodsFilter())
                .filter(new StaticMethodsFilter())
                .filter(new GettersFilter())
                .filter(new SettersFilter())
                .filter(new NoTargetsMethodsFilter(new RelevantClasses(classes)))
                .filter(new OverridingMethodsFilter())
                .filter(new OverriddenMethodsFilter())
                .filter(new PrivateMethodsCallersFilter())
                .filter(new PrivateFieldAccessorsFilter(this))
                .filter(new EmptyMethodsFilter())
                .filter(new ExceptionsThrowersFilter())
                .filter(new SimpleDelegationsFilter())
                .filter(new SingleMethodFilter())
                .collect(Collectors.toList());
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public List<PsiJavaFile> getAllJavaFiles() {
        return allJavaFiles;
    }

    @NotNull
    public List<PsiJavaFile> getSourceJavaFiles() {
        return sourceJavaFiles;
    }

    @NotNull
    public List<PsiClass> getClasses() {
        return classes;
    }

    @NotNull
    public List<PsiMethod> getMethods() {
        return methods;
    }

    @NotNull
    public List<PsiMethod> getMethodsAfterFiltration() {
        return methodsAfterFiltration;
    }

    @NotNull
    public AccessorsMap getAccessorsMap() {
        return accessorsMap;
    }
}
