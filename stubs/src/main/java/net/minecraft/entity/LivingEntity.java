package net.minecraft.entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
public abstract class LivingEntity {
    public Vec3d getVelocity() { return null; }
    public void setVelocity(double x, double y, double z) {}
    public void setVelocity(Vec3d velocity) {}
    public World getWorld() { return null; }
    public boolean isOnGround() { return false; }
    public boolean isSneaking() { return false; }
    public boolean isSprinting() { return false; }
    public boolean isClimbing() { return false; }
    public boolean isTouchingWater() { return false; }
    protected void jump() {}
    public void travel(Vec3d movementInput) {}
}
