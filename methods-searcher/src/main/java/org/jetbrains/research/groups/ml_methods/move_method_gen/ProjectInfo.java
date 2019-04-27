package org.jetbrains.research.groups.ml_methods.move_method_gen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.Filter;
import org.jetbrains.research.groups.ml_methods.move_method_gen.filters.FilterWithCounter;
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

    private final @NotNull List<Filter<PsiClass>> classFilters =
            new ArrayList<Filter<PsiClass>>() {{
                add(new TypeParametersFilter());
                add(new InterfacesFilter());
                add(new AnnotationTypesFilter());
                add(new TestsFilter());
                add(new BuildersFilter());
                add(new EmptyClassesFilter());
                add(new AnonymousClassesFilter());
            }};

    private final @NotNull List<PsiMethod> methods;

    private final @NotNull List<FilterWithCounter<PsiMethod>> methodsFilters;

    private final @NotNull List<PsiMethod> methodsAfterFiltration;

    private final @NotNull AccessorsMap accessorsMap;

    public ProjectInfo(final @NotNull Project project) {
        this.project = project;

        allJavaFiles = ExtractingUtils.extractAllJavaFiles(project);
        sourceJavaFiles = ExtractingUtils.extractSourceJavaFiles(project);

        classes = ExtractingUtils.extractClasses(sourceJavaFiles)
                .stream()
                .filter(it -> {
                    for (Filter<PsiClass> filter : classFilters) {
                        if (!filter.test(it)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        methods = ExtractingUtils.extractMethods(classes);
        accessorsMap = new AccessorsMap(methods);

        methodsFilters =
            new ArrayList<FilterWithCounter<PsiMethod>>() {{
                add(new FilterWithCounter<>(new StaticMethodsFilter()));
                add(new FilterWithCounter<>(new ConstructorsFilter()));
                add(new FilterWithCounter<>(new AbstractMethodsFilter()));
                add(new FilterWithCounter<>(new GettersFilter()));
                add(new FilterWithCounter<>(new SettersFilter()));
                add(new FilterWithCounter<>(new EmptyMethodsFilter()));
                add(new FilterWithCounter<>(new ExceptionsThrowersFilter()));
                add(new FilterWithCounter<>(new SingleMethodFilter()));
                add(new FilterWithCounter<>(new SimpleDelegationsFilter()));
                add(new FilterWithCounter<>(new PrivateMethodsCallersFilter()));
                add(new FilterWithCounter<>(new PrivateFieldAccessorsFilter(ProjectInfo.this)));
                add(new FilterWithCounter<>(new OverridingMethodsFilter()));
                add(new FilterWithCounter<>(new OverriddenMethodsFilter()));
                add(new FilterWithCounter<>(new MethodCallWithSuperFilter()));
                add(new FilterWithCounter<>(new NoTargetsMethodsFilter(new RelevantClasses(classes))));
            }};

        methodsAfterFiltration =
            methods.stream()
                .filter(it -> {
                    for (Filter<PsiMethod> filter : methodsFilters) {
                        if (!filter.test(it)) {
                            return false;
                        }
                    }

                    return true;
                })
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
    public List<FilterWithCounter<PsiMethod>> getMethodsFilters() {
        return methodsFilters;
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
