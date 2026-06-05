package net.minecraft.util;
public final class Identifier {
    private final String namespace, path;
    private Identifier(String namespace, String path) { this.namespace = namespace; this.path = path; }
    public static Identifier of(String namespace, String path) { return new Identifier(namespace, path); }
    public static Identifier of(String id) { return new Identifier("minecraft", id); }
}
