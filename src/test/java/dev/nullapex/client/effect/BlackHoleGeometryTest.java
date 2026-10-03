package dev.nullapex.client.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlackHoleGeometryTest {
    @Test
    void cachesAUnitSphereForTheCoreAndScreenMask() {
        assertEquals(32 * 64 * 4, BlackHoleGeometry.sphereVertexCount());
        assertSame(BlackHoleGeometry.sphereVertex(0), BlackHoleGeometry.sphereVertex(0));

        for (int index = 0; index < BlackHoleGeometry.sphereVertexCount(); index++) {
            BlackHoleGeometry.SphereVertex vertex = BlackHoleGeometry.sphereVertex(index);
            assertTrue(Float.isFinite(vertex.x()));
            assertTrue(Float.isFinite(vertex.y()));
            assertTrue(Float.isFinite(vertex.z()));
            assertTrue(Float.isFinite(vertex.u()));
            assertTrue(Float.isFinite(vertex.v()));
            assertEquals(1.0, Math.sqrt(vertex.x() * vertex.x() + vertex.y() * vertex.y()
                + vertex.z() * vertex.z()), 0.00001);
        }
    }

    @Test
    void cachesBothWarpedDiskLayersWithValidVertexAttributes() {
        BlackHoleGeometry.DiskLayer primary = BlackHoleGeometry.primaryDisk();
        BlackHoleGeometry.DiskLayer secondary = BlackHoleGeometry.secondaryDisk();
        assertSame(primary, BlackHoleGeometry.primaryDisk());
        assertEquals(6 * 64 * 2 * 4 + 64 * 2 * 4, primary.vertexCount());
        assertEquals(primary.vertexCount(), secondary.vertexCount());

        for (BlackHoleGeometry.DiskLayer layer : new BlackHoleGeometry.DiskLayer[] {primary, secondary}) {
            for (int index = 0; index < layer.vertexCount(); index++) {
                BlackHoleGeometry.DiskVertex vertex = layer.vertex(index);
                assertTrue(Float.isFinite(vertex.x()));
                assertTrue(Float.isFinite(vertex.y()));
                assertTrue(Float.isFinite(vertex.z()));
                assertTrue(Float.isFinite(vertex.u()) && vertex.u() >= 0.0F && vertex.u() <= 1.0F);
                assertTrue(Float.isFinite(vertex.v()) && vertex.v() >= 0.0F && vertex.v() <= 1.0F);
                assertTrue(Float.isFinite(vertex.opacityScale())
                    && vertex.opacityScale() > 0.0F && vertex.opacityScale() <= 1.0F);
            }
        }
    }
}
