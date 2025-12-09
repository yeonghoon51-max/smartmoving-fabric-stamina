package net.minecraft.entity;

public class LivingEntity extends Entity {
    public static class Velocity { public double x,y,z; public Velocity(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
    public boolean isSneaking() { return false; }
    public Velocity getVelocity() { return new Velocity(0,0,0); }
    public void setVelocity(double x,double y,double z) {}
    public void setVelocity(Velocity v) { setVelocity(v.x,v.y,v.z); }
    public boolean isOnGround() { return false; }
}
