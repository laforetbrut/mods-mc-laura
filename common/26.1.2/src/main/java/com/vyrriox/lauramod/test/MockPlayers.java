package com.vyrriox.lauramod.test;

import com.mojang.authlib.GameProfile;
import com.vyrriox.lauramod.world.LauraWorldData;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fake players for the self test, created like the vanilla GameTest mock players (a real
 * ServerPlayer in the player list, with a connection that goes nowhere).
 *
 * @author vyrriox
 */
public final class MockPlayers {
    /** What the server told each fake player in chat: Laura's lines arrive there. */
    private static final Map<UUID, List<String>> HEARD = new HashMap<>();

    private MockPlayers() {
    }

    public static ServerPlayer create(ServerLevel level, String name) {
        UUID id = UUID.nameUUIDFromBytes(("lauramod-test-" + name).getBytes(StandardCharsets.UTF_8));
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public void sendSystemMessage(Component message, boolean overlay) {
                HEARD.computeIfAbsent(id, key -> new ArrayList<>()).add(message.getString());
                super.sendSystemMessage(message, overlay);
            }
        };
        HEARD.remove(id);
        // The UUID is the same on every run: start from a clean slate, whatever an earlier
        // (possibly interrupted) run left in the test world.
        forgetCompanions(level.getServer(), id);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        com.vyrriox.lauramod.LauraMod.platform().configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** Removes the player's companions from the worlds and from the saved data. */
    public static void forgetCompanions(MinecraftServer server, UUID owner) {
        LauraWorldData data = LauraWorldData.get(server);
        for (LauraWorldData.Record r : data.byOwner(owner)) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(r.laura);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        data.forgetOwner(owner);
    }

    /** The chat messages the fake player received since it was created, oldest first. */
    public static List<String> heard(ServerPlayer player) {
        return HEARD.getOrDefault(player.getUUID(), List.of());
    }

    public static void setLanguage(ServerPlayer player, String language) {
        player.updateOptions(new ClientInformation(language, 8, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, false, ParticleStatus.ALL));
    }

    public static void remove(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        server.getPlayerList().remove(player);
        forgetCompanions(server, player.getUUID());
        HEARD.remove(player.getUUID());
    }
}
