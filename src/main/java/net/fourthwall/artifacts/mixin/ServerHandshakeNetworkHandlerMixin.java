package net.fourthwall.artifacts.mixin;

import eu.pb4.polymer.autohost.impl.ConnectionExt;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.net.AutoHostAddressSanitizer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ServerHandshakeNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onHandshake", at = @At("TAIL"))
    private void artifacts$sanitizeAutoHostAddress(HandshakeC2SPacket packet, CallbackInfo ci) {
        if (!(this.connection instanceof ConnectionExt autoHostConnection)) {
            return;
        }

        String originalAddress = packet.address();
        String sanitizedAddress = AutoHostAddressSanitizer.sanitize(originalAddress);
        if (sanitizedAddress.equals(originalAddress)) {
            return;
        }

        autoHostConnection.polymerAutoHost$setAddress(sanitizedAddress, packet.port());
        FourthWallArtifacts.LOGGER.info(
                "Sanitized Polymer AutoHost handshake address from '{}' to '{}'.",
                originalAddress,
                sanitizedAddress
        );
    }
}
