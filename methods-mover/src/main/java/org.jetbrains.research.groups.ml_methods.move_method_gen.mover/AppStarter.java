package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectAppStarter;

import java.util.List;

import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.ExtractingUtils.extractClasses;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.ExtractingUtils.extractMethods;
import static org.jetbrains.research.groups.ml_methods.move_method_gen.utils.ExtractingUtils.extractSourceJavaFiles;

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
        List<PsiJavaFile> javaFiles = extractSourceJavaFiles(project);
        List<PsiClass> javaClasses = extractClasses(javaFiles);
        List<PsiMethod> javaMethods = extractMethods(javaClasses);

        PsiMethod method = javaMethods.stream().filter(it -> it.getName().equals("foo")).findFirst().get();
        PsiClass clazz = javaClasses.stream().filter(it -> it.getName().equals("B")).findFirst().get();

        System.out.println(method.getName());
        System.out.println(clazz.getQualifiedName());
    }
}
