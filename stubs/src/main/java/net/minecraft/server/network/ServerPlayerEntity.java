package net.minecraft.server.network;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
public abstract class ServerPlayerEntity extends PlayerEntity {
    public MinecraftServer getServer() { return null; }
    public void writeCustomDataToNbt(NbtCompound nbt) {}
    public void readCustomDataFromNbt(NbtCompound nbt) {}
}
