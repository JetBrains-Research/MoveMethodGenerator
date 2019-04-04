package org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions;

import org.jetbrains.annotations.NotNull;

public class InvalidMethodException extends InvalidCsvInputException {
    public InvalidMethodException(final @NotNull String methodName, final @NotNull String fileLocation, final int classOffset) {
        super("Failed to find method '" + methodName + "' at " + fileLocation + ":" + classOffset);
    }
}
