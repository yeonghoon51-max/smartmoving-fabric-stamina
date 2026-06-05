package net.fabricmc.fabric.api.networking.v1;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
public final class PayloadTypeRegistry {
    public interface Registry {
        <P extends CustomPayload> void register(
                CustomPayload.Id<P> id, PacketCodec<? super RegistryByteBuf, P> codec);
    }
    public static Registry playC2S() { return null; }
    public static Registry playS2C() { return null; }
}
