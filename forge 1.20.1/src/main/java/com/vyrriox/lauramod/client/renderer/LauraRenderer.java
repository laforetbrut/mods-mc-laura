package com.vyrriox.lauramod.client.renderer;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class LauraRenderer extends HumanoidMobRenderer<LauraEntity, PlayerModel<LauraEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LauraMod.MODID, "textures/entity/laura.png");

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(LauraMod.MODID,
            "textures/entity/laura.png");
    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();

    public LauraRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(LauraEntity entity) {
        String url = entity.getSkinUrl();
        if (url == null || url.isEmpty()) {
            return DEFAULT_TEXTURE;
        }

        return SKIN_CACHE.computeIfAbsent(url, this::registerSkin);
    }

    private ResourceLocation registerSkin(String url) {
        try {
            String hash = Integer.toHexString(url.hashCode());
            ResourceLocation location = new ResourceLocation(LauraMod.MODID, "skins/" + hash);
            net.minecraft.client.renderer.texture.TextureManager textureManager = net.minecraft.client.Minecraft
                    .getInstance().getTextureManager();

            if (textureManager.getTexture(location) == null) {
                java.io.File skinDir = new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory,
                        "cached_images/skins");
                skinDir.mkdirs();
                java.io.File skinFile = new java.io.File(skinDir, hash);
                textureManager.register(location, new net.minecraft.client.renderer.texture.HttpTexture(skinFile, url,
                        DEFAULT_TEXTURE, false, null));
            }
            return location;
        } catch (Exception e) {
            return DEFAULT_TEXTURE;
        }
    }
}
