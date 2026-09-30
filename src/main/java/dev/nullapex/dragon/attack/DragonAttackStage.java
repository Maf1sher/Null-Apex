package dev.nullapex.dragon.attack;

public enum DragonAttackStage {
    IDLE("idle"),
    WINDUP("windup"),
    ACTIVE("active"),
    RECOVERY("recovery");

    private final String displayName;

    DragonAttackStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
