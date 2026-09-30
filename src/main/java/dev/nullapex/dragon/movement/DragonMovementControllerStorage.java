package dev.nullapex.dragon.movement;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lazily creates controllers in the per-entity storage supplied by the dragon mixin. */
final class DragonMovementControllerStorage {
    private DragonMovementControllerStorage() {
    }

    static <T> T existing(Supplier<? extends T> getter) {
        return Objects.requireNonNull(getter, "getter").get();
    }

    static <T> T getOrCreate(
        Supplier<? extends T> getter,
        Consumer<? super T> setter,
        Supplier<? extends T> factory
    ) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        Objects.requireNonNull(factory, "factory");

        T value = getter.get();
        if (value == null) {
            value = Objects.requireNonNull(factory.get(), "factory result");
            setter.accept(value);
        }
        return value;
    }
}
