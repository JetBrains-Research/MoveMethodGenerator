package org.jetbrains.research.groups.ml_methods.move_method_gen.mover;

public class MethodToMove {
    private final int methodId;

    private final int targetClassId;

    public MethodToMove(final int methodId, final int targetClassId) {
        this.methodId = methodId;
        this.targetClassId = targetClassId;
    }

    public int getMethodId() {
        return methodId;
    }

    public int getTargetClassId() {
        return targetClassId;
    }
}
