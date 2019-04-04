package org.jetbrains.research.groups.ml_methods.move_method_gen.filters;

import org.jetbrains.annotations.NotNull;

public class FilterWithCounter<T> implements Filter<T> {
    private final @NotNull Filter<T> filter;

    private int filteredOut = 0;

    public FilterWithCounter(final @NotNull Filter<T> filter) {
        this.filter = filter;
    }

    @Override
    public boolean test(T t) {
        boolean result = filter.test(t);
        if (!result) {
            ++filteredOut;
        }

        return result;
    }

    public int getFilteredOut() {
        return filteredOut;
    }

    public @NotNull String getDescription() {
        return filter.getClass().getSimpleName() + " filtered: " + filteredOut;
    }
}
