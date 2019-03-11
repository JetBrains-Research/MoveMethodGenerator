package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.groups.ml_methods.move_method_gen.ProjectAppStarter;

public class AppStarter extends ProjectAppStarter {
    @Override
    public String getCommandName() {
        return "change-project";
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
    }
}
