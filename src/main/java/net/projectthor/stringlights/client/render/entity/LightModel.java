package net.projectthor.stringlights.client.render.entity;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.projectthor.stringlights.StringLights;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public record LightModel(float[] vertices, float[] uvs) {

    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    public void render(VertexConsumer buffer, MatrixStack matrices, int bLight0, int bLight1, int sLight0, int sLight1) {
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        int count = vertices.length / 3;
        for (int i = 0; i < count; i++) {
            float f = (i % (count / 2f)) / (count / 2f);
            int blockLight = (int) MathHelper.lerp(f, (float) bLight0, (float) bLight1);
            int skyLight = (int) MathHelper.lerp(f, (float) sLight0, (float) sLight1);
            int light = LightmapTextureManager.pack(blockLight, skyLight);
            buffer
                    .vertex(modelMatrix, vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2])
                    .color(255, 255, 255, 255)
                    .texture(uvs[i * 2], uvs[i * 2 + 1])
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(1, 0.35f, 0);
        }
    }

    public static class Builder {
        private final List<Float> vertices;
        private final List<Float> uvs;
        private int size;

        public Builder(int initialCapacity) {
            vertices = new ArrayList<>(initialCapacity * 3);
            uvs = new ArrayList<>(initialCapacity * 2);
        }

        public Builder vertex(Vector3f v) {
            vertices.add(v.x());
            vertices.add(v.y());
            vertices.add(v.z());
            return this;
        }

        public Builder uv(float u, float v) {
            uvs.add(u);
            uvs.add(v);
            return this;
        }

        public void next() {
            size++;
        }

        public LightModel build() {
            if (vertices.size() != size * 3) StringLights.LOGGER.error("Wrong count of vertices");
            if (uvs.size() != size * 2) StringLights.LOGGER.error("Wrong count of uvs");

            return new LightModel(toFloatArray(vertices), toFloatArray(uvs));
        }

        private float[] toFloatArray(List<Float> floats) {
            float[] array = new float[floats.size()];
            int i = 0;

            for (float f : floats) {
                array[i++] = f;
            }

            return array;
        }
    }
}
