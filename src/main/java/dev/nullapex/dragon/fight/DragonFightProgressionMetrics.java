package dev.nullapex.dragon.fight;

/** Inputs used to decide whether the dragon fight should advance to another phase. */
public record DragonFightProgressionMetrics(double healthRatio) {
    public DragonFightProgressionMetrics {
        if (!Double.isFinite(healthRatio)) {
            throw new IllegalArgumentException("Health ratio must be finite");
        }
        healthRatio = Math.max(0.0, Math.min(1.0, healthRatio));
    }
}
