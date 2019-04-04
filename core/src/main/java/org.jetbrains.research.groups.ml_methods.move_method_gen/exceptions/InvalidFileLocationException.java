package org.jetbrains.research.groups.ml_methods.move_method_gen.exceptions;

import org.jetbrains.annotations.NotNull;

public class InvalidFileLocationException extends InvalidCsvInputException {
    public InvalidFileLocationException(final @NotNull String fileLocation) {
        super("Failed to find file '" + fileLocation + "'");
    }
}
