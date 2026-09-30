package dev.nullapex.dragon.fight;

/** Advances the fight through health thresholds without moving back to an earlier phase. */
public final class HealthThresholdPhasePolicy implements DragonFightPhasePolicy {
    public static final double ESCALATION_HEALTH_RATIO = 0.70;
    public static final double FINAL_HEALTH_RATIO = 0.35;

    private final double escalationHealthRatio;
    private final double finalHealthRatio;

    public HealthThresholdPhasePolicy() {
        this(ESCALATION_HEALTH_RATIO, FINAL_HEALTH_RATIO);
    }

    public HealthThresholdPhasePolicy(double escalationHealthRatio, double finalHealthRatio) {
        if (!Double.isFinite(escalationHealthRatio)
            || !Double.isFinite(finalHealthRatio)
            || escalationHealthRatio < 0.0
            || escalationHealthRatio > 1.0
            || finalHealthRatio < 0.0
            || finalHealthRatio >= escalationHealthRatio) {
            throw new IllegalArgumentException("Health thresholds must satisfy 0 <= final < escalation <= 1");
        }

        this.escalationHealthRatio = escalationHealthRatio;
        this.finalHealthRatio = finalHealthRatio;
    }

    @Override
    public DragonFightPhase nextPhase(
        DragonFightPhase currentPhase,
        DragonFightProgressionMetrics metrics
    ) {
        if (currentPhase == DragonFightPhase.FINAL) {
            return currentPhase;
        }

        double healthRatio = metrics.healthRatio();
        if (healthRatio <= this.finalHealthRatio) {
            return DragonFightPhase.FINAL;
        }
        if (currentPhase == DragonFightPhase.OPENING && healthRatio <= this.escalationHealthRatio) {
            return DragonFightPhase.ESCALATION;
        }
        return currentPhase;
    }
}
