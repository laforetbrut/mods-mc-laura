package com.vyrriox.lauramod.skin;

import com.vyrriox.lauramod.world.LauraAdvancements;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.model.ModelParser;
import com.vyrriox.lauramod.network.LauraNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Server-side validation and application of skin and model changes.
 *
 * @author vyrriox
 */
public final class SkinService {
    private SkinService() {
    }

    private static long lastRescan;

    /** Rescans the asset folders at most every 5 seconds, so unknown names cannot be used to spam disk scans. */
    private static void rescanThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastRescan > 5000) {
            lastRescan = now;
            ServerAssetStore.rescan();
        }
    }

    public static boolean canChangeLook(ServerPlayer player, LauraEntity laura) {
        return laura.isOwnedBy(player) || LauraConfig.othersCanChangeSkin.get() || player.hasPermissions(2);
    }

    /** Applies a skin reference sent by a client or typed in a command. Returns a feedback message. */
    public static void requestSkin(ServerPlayer player, LauraEntity laura, String rawRef, boolean slim) {
        if (!canChangeLook(player, laura)) {
            player.sendSystemMessage(Component.translatable("lauramod.skin.not_allowed"));
            return;
        }
        String raw = rawRef == null ? "" : rawRef.trim();
        if (raw.equalsIgnoreCase("reset") || raw.isEmpty()) {
            laura.setSkin(SkinRef.parse(LauraConfig.defaultSkin.get()), true);
            player.sendSystemMessage(Component.translatable("lauramod.skin.reset"));
            return;
        }
        SkinRef ref = SkinRef.parse(raw.startsWith("http") ? "url:" + raw : raw);
        switch (ref.type()) {
            case BUILTIN -> {
                laura.setSkin(ref, slim);
                player.sendSystemMessage(Component.translatable("lauramod.skin.changed", Component.translatable("lauramod.skin.builtin." + ref.value())));
                LauraAdvancements.award(player, "new_look");
            }
            case URL -> {
                if (!LauraConfig.allowUrlSkins.get()) {
                    player.sendSystemMessage(Component.translatable("lauramod.skin.url_disabled"));
                    return;
                }
                String url = SkinRef.normalizeUrl(ref.value());
                if (url.length() > 1024 || !SkinRef.isAllowedUrl(url, LauraConfig.urlDomainWhitelist.get())) {
                    player.sendSystemMessage(Component.translatable("lauramod.skin.url_refused", String.join(", ", LauraConfig.urlDomainWhitelist.get())));
                    return;
                }
                laura.setSkin(new SkinRef(SkinRef.Type.URL, url, ""), slim);
                player.sendSystemMessage(Component.translatable("lauramod.skin.changed", Component.literal(url)));
                LauraAdvancements.award(player, "new_look");
            }
            case SERVER -> {
                ServerAssetStore.Entry entry = ServerAssetStore.get(AssetKind.SKIN, ref.value());
                if (entry == null) {
                    // A file added to the folder since the last scan.
                    rescanThrottled();
                    entry = ServerAssetStore.get(AssetKind.SKIN, ref.value());
                }
                if (entry == null) {
                    player.sendSystemMessage(Component.translatable("lauramod.skin.file_missing", ref.value()));
                    return;
                }
                laura.setSkin(new SkinRef(SkinRef.Type.SERVER, entry.name(), entry.sha1()), slim);
                player.sendSystemMessage(Component.translatable("lauramod.skin.changed", Component.literal(entry.name())));
                LauraAdvancements.award(player, "new_look");
            }
            case PLAYER -> {
                if (!LauraConfig.allowPlayerNameSkins.get()) {
                    player.sendSystemMessage(Component.translatable("lauramod.skin.player_disabled"));
                    return;
                }
                if (!MojangSkinResolver.isValidName(ref.value())) {
                    player.sendSystemMessage(Component.translatable("lauramod.skin.player_invalid", ref.value()));
                    return;
                }
                player.sendSystemMessage(Component.translatable("lauramod.skin.player_searching", ref.value()));
                MinecraftServer server = player.getServer();
                MojangSkinResolver.resolve(ref.value()).thenAccept(result -> {
                    if (server == null) {
                        return;
                    }
                    server.execute(() -> {
                        if (!laura.isAlive()) {
                            return;
                        }
                        if (result.isEmpty()) {
                            player.sendSystemMessage(Component.translatable("lauramod.skin.player_not_found", ref.value()));
                            return;
                        }
                        MojangSkinResolver.Result r = result.get();
                        laura.setSkin(new SkinRef(SkinRef.Type.URL, r.url(), ""), r.slim());
                        laura.setSkinLabel("player:" + r.name());
                        player.sendSystemMessage(Component.translatable("lauramod.skin.changed", Component.literal(r.name())));
                        LauraAdvancements.award(player, "new_look");
                    });
                });
            }
        }
    }

    public static void requestModel(ServerPlayer player, LauraEntity laura, String rawName) {
        if (!canChangeLook(player, laura)) {
            player.sendSystemMessage(Component.translatable("lauramod.skin.not_allowed"));
            return;
        }
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.equalsIgnoreCase("reset") || name.equalsIgnoreCase("default")) {
            laura.setModel("");
            player.sendSystemMessage(Component.translatable("lauramod.model.reset"));
            return;
        }
        if (!LauraConfig.allowCustomModels.get()) {
            player.sendSystemMessage(Component.translatable("lauramod.model.disabled"));
            return;
        }
        if (name.startsWith("pack:")) {
            String packName = name.substring(5);
            if (!ServerAssetStore.isSafeName(packName)) {
                return;
            }
            laura.setModel("pack:" + packName);
            player.sendSystemMessage(Component.translatable("lauramod.model.changed", packName));
            LauraAdvancements.award(player, "makeover");
            return;
        }
        String serverName = name.startsWith("server:") ? name.substring(7) : name;
        int hash = serverName.indexOf('#');
        if (hash > 0) {
            serverName = serverName.substring(0, hash);
        }
        ServerAssetStore.Entry entry = ServerAssetStore.get(AssetKind.MODEL, serverName);
        if (entry == null && ServerAssetStore.isSafeName(serverName)) {
            // A file added to the folder since the last scan.
            rescanThrottled();
            entry = ServerAssetStore.get(AssetKind.MODEL, serverName);
        }
        if (entry == null) {
            // Not on the server: maybe a resource pack model the clients have.
            if (ServerAssetStore.isSafeName(serverName)) {
                laura.setModel("pack:" + serverName);
                player.sendSystemMessage(Component.translatable("lauramod.model.changed_pack", serverName));
                LauraAdvancements.award(player, "makeover");
            }
            return;
        }
        laura.setModel("server:" + entry.name() + "#" + entry.sha1());
        player.sendSystemMessage(Component.translatable("lauramod.model.changed", entry.name()));
        LauraAdvancements.award(player, "makeover");
    }

    /** Called when a complete upload arrived. */
    public static void finishUpload(ServerPlayer player, LauraEntity laura, AssetKind kind, String name, byte[] data, boolean slim) {
        String stem = sanitize(player.getGameProfile().getName()) + "_" + sanitize(name);
        if (kind == AssetKind.SKIN) {
            if (!ServerAssetStore.isValidSkinPng(data)) {
                LauraNetwork.uploadResult(player, false, Component.translatable("lauramod.upload.invalid_skin"));
                return;
            }
        } else {
            try {
                ModelParser.parse(name.endsWith(".geo.json") ? name : name + ".bbmodel", new String(data, StandardCharsets.UTF_8), null);
            } catch (RuntimeException e) {
                LauraNetwork.uploadResult(player, false, Component.translatable("lauramod.upload.invalid_model", e.getMessage()));
                return;
            }
        }
        try {
            ServerAssetStore.Entry entry = ServerAssetStore.storeUpload(kind, stem, data);
            if (entry == null) {
                LauraNetwork.uploadResult(player, false, Component.translatable("lauramod.upload.failed"));
                return;
            }
            LauraMod.LOGGER.info("{} uploaded {} {}", player.getGameProfile().getName(), kind.name().toLowerCase(Locale.ROOT), entry.name());
            LauraNetwork.uploadResult(player, true, Component.translatable("lauramod.upload.done", entry.name()));
            if (laura != null && canChangeLook(player, laura)) {
                if (kind == AssetKind.SKIN) {
                    laura.setSkin(new SkinRef(SkinRef.Type.SERVER, entry.name(), entry.sha1()), slim);
                } else {
                    laura.setModel("server:" + entry.name() + "#" + entry.sha1());
                }
            }
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Upload from {} failed: {}", player.getGameProfile().getName(), e.getMessage());
            LauraNetwork.uploadResult(player, false, Component.translatable("lauramod.upload.failed"));
        }
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        String stem = s.replaceAll("(?i)\\.(png|bbmodel|geo\\.json)$", "");
        for (char c : stem.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
                sb.append(c);
            }
            if (sb.length() >= 32) {
                break;
            }
        }
        return sb.isEmpty() ? "file" : sb.toString();
    }
}
