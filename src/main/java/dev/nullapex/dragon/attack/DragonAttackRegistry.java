package dev.nullapex.dragon.attack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Built-in dragon attack definitions, indexed by their stable IDs. */
public final class DragonAttackRegistry {
    private static final Map<String, DragonAttack> ATTACKS = Stream.of(SwoopAttack.INSTANCE)
        .collect(Collectors.toUnmodifiableMap(attack -> attack.definition().id(), attack -> attack));

    private DragonAttackRegistry() {
    }

    public static Optional<DragonAttack> find(String id) {
        return Optional.ofNullable(ATTACKS.get(Objects.requireNonNull(id, "id")));
    }

    public static Map<String, DragonAttack> all() {
        return ATTACKS;
    }
}
