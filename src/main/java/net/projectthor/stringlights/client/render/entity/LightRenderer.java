package net.projectthor.stringlights.client.render.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.projectthor.stringlights.StringLights;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static net.projectthor.stringlights.util.Helper.drip2;
import static net.projectthor.stringlights.util.Helper.drip2prime;

public class LightRenderer {

    private static final float LIGHT_SCALE = 1f;
    private static final int MAX_SEGMENTS = 2048;
    private final Object2ObjectOpenHashMap<BakeKey, LightModel> models = new Object2ObjectOpenHashMap<>(256);

    public void renderBaked(VertexConsumer buffer, MatrixStack matrices, BakeKey key, Vector3f lightVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        LightModel model;
        if (models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = buildModel(lightVec);
            models.put(key, model);
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && models.size() > 10000) {
            StringLights.LOGGER.error("Light model leak found");
        }
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    private LightModel buildModel(Vector3f lightVec) {
        float desiredSegmentLength = 1f / StringLights.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * lightVec.lengthSquared() / desiredSegmentLength);
        LightModel.Builder builder = LightModel.builder(initialCapacity);

        if (Float.isNaN(lightVec.x()) && Float.isNaN(lightVec.z())) {
            buildFaceVertical(builder, lightVec, 45, UVRect.DEFAULT_SIDE_A);
            buildFaceVertical(builder, lightVec, -45, UVRect.DEFAULT_SIDE_B);
        } else {
            buildFace(builder, lightVec, 45, UVRect.DEFAULT_SIDE_A);
            buildFace(builder, lightVec, -45, UVRect.DEFAULT_SIDE_B);
        }

        return builder.build();
    }

    private void buildFaceVertical(LightModel.Builder builder, Vector3f v, float angle, UVRect uv) {
        v.x = 0;
        v.z = 0;
        float actualSegmentLength = 1f / StringLights.runtimeConfig.getQuality();
        float lightWidth = (uv.x1() - uv.x0()) / 16 * LIGHT_SCALE;

        Vector3f normal = new Vector3f((float) Math.cos(Math.toRadians(angle)), 0, (float) Math.sin(Math.toRadians(angle)));
        normal.normalize(lightWidth);

        Vector3f vert00 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), vert01 = new Vector3f(vert00);

        Vector3f vert10 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), vert11 = new Vector3f(vert10);


        float uvv0 = 0, uvv1 = 0;
        boolean lastIter = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            if (vert00.y() + actualSegmentLength >= v.y()) {
                lastIter = true;
                actualSegmentLength = v.y() - vert00.y();
            }

            vert10.add(0, actualSegmentLength, 0);
            vert11.add(0, actualSegmentLength, 0);

            uvv1 += actualSegmentLength / LIGHT_SCALE;

            builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            uvv0 = uvv1;

            vert00.set(vert10);
            vert01.set(vert11);
        }
    }

    private void buildFace(LightModel.Builder builder, Vector3f v, float angle, UVRect uv) {
        float actualSegmentLength, desiredSegmentLength = 1f / StringLights.runtimeConfig.getQuality();
        float distance = v.length(), distanceXZ = (float) Math.sqrt(Math.fma(v.x(), v.x(), v.z() * v.z()));
        float wrongDistanceFactor = distance / distanceXZ;


        Vector3f vert00 = new Vector3f(), vert01 = new Vector3f(), vert11 = new Vector3f(), vert10 = new Vector3f();
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();

        float lightWidth = (uv.x1() - uv.x0()) / 16 * LIGHT_SCALE;

        float uvv0, uvv1 = 0, gradient, x, y;
        Vector3f point0 = new Vector3f(), point1 = new Vector3f();
        Quaternionf rotator = new Quaternionf();


        point0.set(0, (float) drip2(0, distance, v.y()), 0);
        gradient = (float) drip2prime(0, distance, v.y());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = estimateDeltaX(desiredSegmentLength, gradient);
        gradient = (float) drip2prime(x * wrongDistanceFactor, distance, v.y());
        y = (float) drip2(x * wrongDistanceFactor, distance, v.y());
        point1.set(x, y, 0);

        rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
        rotAxis.normalize();
        rotator.fromAxisAngleDeg(rotAxis, angle);


        normal.rotate(rotator);
        normal.normalize(lightWidth);
        vert10.set(point0.x() - normal.x() / 2, point0.y() - normal.y() / 2, point0.z() - normal.z() / 2);
        vert11.set(vert10);
        vert11.add(normal);


        actualSegmentLength = point0.distance(point1);


        boolean lastIter = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
            rotAxis.normalize();
            rotator = rotator.fromAxisAngleDeg(rotAxis, angle);

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotate(rotator);
            normal.normalize(lightWidth);

            vert00.set(vert10);
            vert01.set(vert11);

            vert10.set(point1.x() - normal.x() / 2, point1.y() - normal.y() / 2, point1.z() - normal.z() / 2);
            vert11.set(vert10);
            vert11.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / LIGHT_SCALE;

            builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            point0.set(point1);

            x += estimateDeltaX(desiredSegmentLength, gradient);
            if (x >= distanceXZ) {
                lastIter = true;
                x = distanceXZ;
            }

            gradient = (float) drip2prime(x * wrongDistanceFactor, distance, v.y());
            y = (float) drip2(x * wrongDistanceFactor, distance, v.y());
            point1.set(x, y, 0);

            actualSegmentLength = point0.distance(point1);
        }
    }

    private float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k * k));
    }

    public void render(VertexConsumer buffer, MatrixStack matrices, Vector3f lightVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        LightModel model = buildModel(lightVec);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void purge() {
        models.clear();
    }

    public static class BakeKey {
        private final int hash;

        public BakeKey(Vec3d srcPos, Vec3d dstPos) {
            float dY = (float) (srcPos.y - dstPos.y);
            float dXZ = new Vector3f((float) srcPos.x, 0, (float) srcPos.z)
                    .distance((float) dstPos.x, 0, (float) dstPos.z);
            int hash = Float.floatToIntBits(dY);
            hash = 31 * hash + Float.floatToIntBits(dXZ);
            this.hash = hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BakeKey bakeKey = (BakeKey) o;
            return hash == bakeKey.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
