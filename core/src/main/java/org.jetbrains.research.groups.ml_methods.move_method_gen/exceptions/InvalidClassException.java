package org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions;

import org.jetbrains.annotations.NotNull;

public class InvalidClassException extends InvalidCsvInputException {
    public InvalidClassException(final @NotNull String className, final @NotNull String fileLocation, final int classOffset) {
        super("Failed to find class '" + className + "' at " + fileLocation + ":" + classOffset);
    }
}
