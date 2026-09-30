package dev.nullapex.dragon.fight;

@FunctionalInterface
public interface DragonFightPhasePolicy {
    DragonFightPhase nextPhase(DragonFightPhase currentPhase, DragonFightProgressionMetrics metrics);
}
