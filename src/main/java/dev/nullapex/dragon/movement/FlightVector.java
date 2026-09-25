package dev.nullapex.dragon.movement;

/** Small Minecraft-independent vector used by movement math and unit tests. */
public record FlightVector(double x, double y, double z) {
    public static final FlightVector ZERO = new FlightVector(0.0, 0.0, 0.0);

    public FlightVector add(FlightVector other) {
        return new FlightVector(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public FlightVector subtract(FlightVector other) {
        return new FlightVector(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public FlightVector scale(double scale) {
        return new FlightVector(this.x * scale, this.y * scale, this.z * scale);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }
}
