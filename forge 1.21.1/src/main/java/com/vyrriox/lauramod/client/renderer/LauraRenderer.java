package com.vyrriox.lauramod.client.renderer;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LauraRenderer extends HumanoidMobRenderer<LauraEntity, PlayerModel<LauraEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(LauraMod.MODID,
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

    
    private ResourceLocation registerSkin(String urlStr) {
        try {
            String hash = Integer.toHexString(urlStr.hashCode());
            ResourceLocation location;
            try {
                // Forge 1.20
                location = (ResourceLocation) ResourceLocation.class.getConstructor(String.class, String.class).newInstance(LauraMod.MODID, "skins/" + hash);
            } catch(Exception e) {
                // Forge/NeoForge 1.21.1
                location = (ResourceLocation) ResourceLocation.class.getMethod("fromNamespaceAndPath", String.class, String.class).invoke(null, LauraMod.MODID, "skins/" + hash);
            }
            
            final ResourceLocation finalLocation = location;
            net.minecraft.client.renderer.texture.TextureManager tm = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            if (tm.getTexture(finalLocation) == null) {
                java.io.File skinDir = new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "cached_images/skins");
                skinDir.mkdirs();
                java.io.File skinFile = new java.io.File(skinDir, hash + ".png");

                if (!skinFile.exists()) {
                    new Thread(() -> {
                        try {
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                            conn.connect();
                            try (java.io.InputStream in = conn.getInputStream(); java.io.FileOutputStream out = new java.io.FileOutputStream(skinFile)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                            }
                            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                                tm.register(finalLocation, new net.minecraft.client.renderer.texture.HttpTexture(skinFile, urlStr, DEFAULT_TEXTURE, false, null));
                            });
                        } catch (Exception e) {}
                    }).start();
                } else {
                    tm.register(finalLocation, new net.minecraft.client.renderer.texture.HttpTexture(skinFile, urlStr, DEFAULT_TEXTURE, false, null));
                }
            }
            return finalLocation;
        } catch (Exception e) {
            return DEFAULT_TEXTURE;
        }
    }
}
