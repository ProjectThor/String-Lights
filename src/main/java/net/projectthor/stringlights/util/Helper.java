package net.projectthor.stringlights.util;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.projectthor.stringlights.StringLights;
import org.joml.Vector3f;

public class Helper {

    public static Identifier identifier(String name) {
        return Identifier.of(StringLights.MOD_ID, name);
    }

    @Deprecated
    public static double drip(double x, double d) {
        double c = StringLights.runtimeConfig.getLightHangAmount();
        double b = -c / d;
        double a = c / (d * d);
        return (a * (x * x) + b * x);
    }

    public static double drip2(double x, double d, double h) {
        double a = StringLights.runtimeConfig.getLightHangAmount();
        a = a + (d * 0.3);
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        double p2 = -a * Math.cosh((2D * p1 - d) / (2D * a));
        return p2 + a * Math.cosh((((2D * x) + (2D * p1)) - d) / (2D * a));
    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    public static double drip2prime(double x, double d, double h) {
        double a = StringLights.runtimeConfig.getLightHangAmount();
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        return Math.sinh((2 * x + 2 * p1 - d) / (2 * a));
    }

    public static Vec3d middleOf(Vec3d a, Vec3d b) {
        double x = (a.getX() - b.getX()) / 2d + b.getX();
        double y = (a.getY() - b.getY()) / 2d + b.getY();
        double z = (a.getZ() - b.getZ()) / 2d + b.getZ();
        return new Vec3d(x, y, z);
    }

    public static Vec3d getLightOffset(Vec3d start, Vec3d end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.normalize(2 / 16f);
        return new Vec3d(offset);
    }
}
