package com.vyrriox.lauramod.fabric.mixin;

import com.vyrriox.lauramod.fabric.PlayerLanguages;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Laura answers each player in the language of their game. On Minecraft 1.20.1 the server drops
 * the language of the client settings packet (Forge and NeoForge patch the player to keep it, later
 * Minecraft versions keep the whole packet) and Fabric API has no event for that packet. This is
 * the only mixin of the mod: it reads the language when the packet is applied and changes nothing.
 *
 * @author vyrriox
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void lauramod$rememberLanguage(ServerboundClientInformationPacket packet, CallbackInfo info) {
        PlayerLanguages.remember((ServerPlayer) (Object) this, packet.language());
    }
}
