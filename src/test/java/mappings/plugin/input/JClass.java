package mappings.plugin.input;

import java.util.Collection;

// Bridge method testing
public class JClass<T extends Collection<T>> extends IClass<T> {
    @Override
    public T get() {
        return null;
    }

    @Override
    public void set(T value) {
    }
}
