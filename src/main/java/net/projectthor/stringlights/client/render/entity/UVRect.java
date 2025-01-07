package net.projectthor.stringlights.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record UVRect(float x0, float x1) {
    public static final UVRect DEFAULT_SIDE_A = new UVRect(0, 3);
    public static final UVRect DEFAULT_SIDE_B = new UVRect(3, 6);
}
