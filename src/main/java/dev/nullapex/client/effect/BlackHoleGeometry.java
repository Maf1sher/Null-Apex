package dev.nullapex.client.effect;

import java.util.ArrayList;
import java.util.List;

/** Immutable local-space mesh templates reused by the black-hole renderer. */
final class BlackHoleGeometry {
    private static final int SPHERE_LATITUDE_SEGMENTS = 32;
    private static final int SPHERE_LONGITUDE_SEGMENTS = 64;
    private static final int DISK_ANGULAR_SEGMENTS = 64;
    private static final int DISK_RADIAL_SEGMENTS = 6;
    private static final float DISK_RADIUS = 1.25F;
    private static final float PRIMARY_DISK_INNER_RADIUS = 0.42F;

    private static final List<SphereVertex> SPHERE_VERTICES = createSphere();
    private static final DiskLayer PRIMARY_DISK = createDiskLayer(
        PRIMARY_DISK_INNER_RADIUS, DISK_RADIUS, DISK_RADIUS,
        DISK_RADIUS * 0.035F, DISK_RADIUS * 0.035F, 1.0F);
    private static final DiskLayer SECONDARY_DISK = createDiskLayer(
        DISK_RADIUS * 0.54F, DISK_RADIUS * 0.94F, DISK_RADIUS,
        DISK_RADIUS * 0.06F, DISK_RADIUS * 0.055F, 0.44F);

    private BlackHoleGeometry() {
    }

    static int sphereVertexCount() {
        return SPHERE_VERTICES.size();
    }

    static SphereVertex sphereVertex(int index) {
        return SPHERE_VERTICES.get(index);
    }

    static DiskLayer primaryDisk() {
        return PRIMARY_DISK;
    }

    static DiskLayer secondaryDisk() {
        return SECONDARY_DISK;
    }

    private static List<SphereVertex> createSphere() {
        List<SphereVertex> vertices = new ArrayList<>(
            SPHERE_LATITUDE_SEGMENTS * SPHERE_LONGITUDE_SEGMENTS * 4);
        for (int latitude = 0; latitude < SPHERE_LATITUDE_SEGMENTS; latitude++) {
            float top = (float)latitude / SPHERE_LATITUDE_SEGMENTS;
            float bottom = (float)(latitude + 1) / SPHERE_LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < SPHERE_LONGITUDE_SEGMENTS; longitude++) {
                float left = (float)longitude / SPHERE_LONGITUDE_SEGMENTS;
                float right = (float)(longitude + 1) / SPHERE_LONGITUDE_SEGMENTS;
                vertices.add(sphereVertex(left, top));
                vertices.add(sphereVertex(left, bottom));
                vertices.add(sphereVertex(right, bottom));
                vertices.add(sphereVertex(right, top));
            }
        }
        return List.copyOf(vertices);
    }

    private static SphereVertex sphereVertex(float longitude, float latitude) {
        double theta = Math.PI * latitude;
        double phi = Math.PI * 2.0 * longitude;
        float sinTheta = (float)Math.sin(theta);
        float x = sinTheta * (float)Math.cos(phi);
        float y = (float)Math.cos(theta);
        float z = sinTheta * (float)Math.sin(phi);
        return new SphereVertex(x, y, z, longitude, latitude);
    }

    private static DiskLayer createDiskLayer(float innerRadius, float outerRadius, float normalizationScale,
        float halfThickness, float warpAmplitude, float opacityScale) {
        List<DiskVertex> vertices = new ArrayList<>(
            (DISK_RADIAL_SEGMENTS * DISK_ANGULAR_SEGMENTS * 2 + DISK_ANGULAR_SEGMENTS * 2) * 4);
        for (int radial = 0; radial < DISK_RADIAL_SEGMENTS; radial++) {
            float radialStart = (float)radial / DISK_RADIAL_SEGMENTS;
            float radialEnd = (float)(radial + 1) / DISK_RADIAL_SEGMENTS;
            float radius0 = innerRadius + (outerRadius - innerRadius) * radialStart;
            float radius1 = innerRadius + (outerRadius - innerRadius) * radialEnd;
            for (int segment = 0; segment < DISK_ANGULAR_SEGMENTS; segment++) {
                double angle0 = Math.PI * 2.0 * segment / DISK_ANGULAR_SEGMENTS;
                double angle1 = Math.PI * 2.0 * (segment + 1) / DISK_ANGULAR_SEGMENTS;
                float warp00 = diskWarp(radius0, angle0, normalizationScale, warpAmplitude);
                float warp10 = diskWarp(radius1, angle0, normalizationScale, warpAmplitude);
                float warp11 = diskWarp(radius1, angle1, normalizationScale, warpAmplitude);
                float warp01 = diskWarp(radius0, angle1, normalizationScale, warpAmplitude);
                float thickness0 = halfThickness * (0.35F + 0.65F * radius0 / normalizationScale);
                float thickness1 = halfThickness * (0.35F + 0.65F * radius1 / normalizationScale);

                addQuad(vertices,
                    radius0, angle0, warp00 + thickness0,
                    radius1, angle0, warp10 + thickness1,
                    radius1, angle1, warp11 + thickness1,
                    radius0, angle1, warp01 + thickness0,
                    opacityScale);
                addQuad(vertices,
                    radius0, angle1, warp01 - thickness0,
                    radius1, angle1, warp11 - thickness1,
                    radius1, angle0, warp10 - thickness1,
                    radius0, angle0, warp00 - thickness0,
                    opacityScale * 0.72F);
                if (radial == DISK_RADIAL_SEGMENTS - 1) {
                    addQuad(vertices,
                        radius1, angle0, warp10 + thickness1,
                        radius1, angle0, warp10 - thickness1,
                        radius1, angle1, warp11 - thickness1,
                        radius1, angle1, warp11 + thickness1,
                        opacityScale * 0.85F);
                }
                if (radial == 0) {
                    addQuad(vertices,
                        radius0, angle1, warp01 + thickness0,
                        radius0, angle1, warp01 - thickness0,
                        radius0, angle0, warp00 - thickness0,
                        radius0, angle0, warp00 + thickness0,
                        opacityScale * 0.72F);
                }
            }
        }
        return new DiskLayer(List.copyOf(vertices));
    }

    private static void addQuad(List<DiskVertex> vertices,
        float radius0, double angle0, float z0,
        float radius1, double angle1, float z1,
        float radius2, double angle2, float z2,
        float radius3, double angle3, float z3,
        float opacityScale) {
        vertices.add(diskVertex(radius0, angle0, z0, opacityScale));
        vertices.add(diskVertex(radius1, angle1, z1, opacityScale));
        vertices.add(diskVertex(radius2, angle2, z2, opacityScale));
        vertices.add(diskVertex(radius3, angle3, z3, opacityScale));
    }

    private static DiskVertex diskVertex(float radius, double angle, float z, float opacityScale) {
        float x = radius * (float)Math.cos(angle);
        float y = radius * (float)Math.sin(angle);
        return new DiskVertex(x, y, z,
            0.5F + x / (2.0F * DISK_RADIUS), 0.5F + y / (2.0F * DISK_RADIUS), opacityScale);
    }

    private static float diskWarp(float radius, double angle, float scale, float amplitude) {
        float normalizedRadius = radius / scale;
        float angularWave = (float)Math.sin(angle * 2.0 + normalizedRadius * 3.5);
        float corrugation = (float)Math.sin(angle * 6.0 - normalizedRadius * 18.0);
        return amplitude * (angularWave * (0.3F + normalizedRadius * 0.7F) + corrugation * 0.22F);
    }

    record SphereVertex(float x, float y, float z, float u, float v) {
    }

    record DiskVertex(float x, float y, float z, float u, float v, float opacityScale) {
    }

    static final class DiskLayer {
        private final List<DiskVertex> vertices;

        private DiskLayer(List<DiskVertex> vertices) {
            this.vertices = vertices;
        }

        int vertexCount() {
            return this.vertices.size();
        }

        DiskVertex vertex(int index) {
            return this.vertices.get(index);
        }
    }
}
