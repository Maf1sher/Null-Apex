package dev.nullapex.dragon.movement;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DragonMovementControllerStorageTest {
    @Test
    void controllerIsCreatedLazilyAndReusedByItsEntity() {
        AtomicReference<Object> entitySlot = new AtomicReference<>();

        assertNull(DragonMovementControllerStorage.existing(entitySlot::get));

        Object controller = DragonMovementControllerStorage.getOrCreate(
            entitySlot::get,
            entitySlot::set,
            Object::new
        );

        assertSame(controller, DragonMovementControllerStorage.existing(entitySlot::get));
        assertSame(controller, DragonMovementControllerStorage.getOrCreate(
            entitySlot::get,
            entitySlot::set,
            Object::new
        ));
    }

    @Test
    void differentEntitiesReceiveDifferentControllers() {
        AtomicReference<Object> firstEntity = new AtomicReference<>();
        AtomicReference<Object> secondEntity = new AtomicReference<>();

        assertNotSame(
            DragonMovementControllerStorage.getOrCreate(firstEntity::get, firstEntity::set, Object::new),
            DragonMovementControllerStorage.getOrCreate(secondEntity::get, secondEntity::set, Object::new)
        );
    }
}
