package dev.nullapex.dragon.attack;

/** Geometry helpers for detecting a target crossed between dragon movement updates. */
public final class SwoopHitDetection {
    private SwoopHitDetection() {
    }

    public static boolean intersectsSweptPart(Box previous, Box current, Box target) {
        Box sweptPart = previous == null ? current : previous.union(current);
        return sweptPart.inflate(0.15).intersects(target);
    }

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private Box union(Box other) {
            return new Box(
                Math.min(this.minX, other.minX),
                Math.min(this.minY, other.minY),
                Math.min(this.minZ, other.minZ),
                Math.max(this.maxX, other.maxX),
                Math.max(this.maxY, other.maxY),
                Math.max(this.maxZ, other.maxZ)
            );
        }

        private Box inflate(double amount) {
            return new Box(
                this.minX - amount,
                this.minY - amount,
                this.minZ - amount,
                this.maxX + amount,
                this.maxY + amount,
                this.maxZ + amount
            );
        }

        private boolean intersects(Box other) {
            return this.maxX > other.minX && this.minX < other.maxX
                && this.maxY > other.minY && this.minY < other.maxY
                && this.maxZ > other.minZ && this.minZ < other.maxZ;
        }
    }
}
