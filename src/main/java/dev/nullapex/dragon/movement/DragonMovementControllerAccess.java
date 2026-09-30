package dev.nullapex.dragon.movement;

/** Internal mixin interface for storing a movement controller on its dragon entity. */
public interface DragonMovementControllerAccess {
    DragonMovementController nullApex$getMovementController();

    void nullApex$setMovementController(DragonMovementController controller);
}
