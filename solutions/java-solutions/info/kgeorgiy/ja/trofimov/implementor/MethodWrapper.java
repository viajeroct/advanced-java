package info.kgeorgiy.ja.trofimov.implementor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * A wrapper over {@link Method} for comparing methods on equality, compares names and
 * parameters types.
 *
 * @param method which we wrap
 */
public record MethodWrapper(Method method) {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodWrapper other) {
            return method.getName().equals(other.method.getName()) &&
                    Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
    }
}
