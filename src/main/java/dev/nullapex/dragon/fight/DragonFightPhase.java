package dev.nullapex.dragon.fight;

public enum DragonFightPhase {
    OPENING("opening"),
    ESCALATION("escalation"),
    FINAL("final");

    private final String serializedName;

    DragonFightPhase(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public static DragonFightPhase fromSerializedName(String serializedName) {
        for (DragonFightPhase phase : values()) {
            if (phase.serializedName.equals(serializedName)) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown dragon fight phase: " + serializedName);
    }

    public boolean isLaterThan(DragonFightPhase other) {
        return this.ordinal() > other.ordinal();
    }
}
