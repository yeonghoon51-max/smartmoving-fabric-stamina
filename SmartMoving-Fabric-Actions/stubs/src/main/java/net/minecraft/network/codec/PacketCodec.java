package net.minecraft.network.codec;

import java.util.function.Function;
import java.util.function.Supplier;

public class PacketCodec<B, T> {
    public static <B, A, T> PacketCodec<B, T> tuple(PacketCodec<B, A> a, Function<T, A> getter, Function<A, T> factory) {
        return new PacketCodec<>();
    }
    public static <B, A, B2, C, D, E, F> PacketCodec<B, F> tuple(PacketCodec<B, A> a, java.util.function.Function<F, A> aGet,
                                                                PacketCodec<B, B2> b, java.util.function.Function<F, B2> bGet,
                                                                PacketCodec<B, C> c, java.util.function.Function<F, C> cGet,
                                                                PacketCodec<B, D> d, java.util.function.Function<F, D> dGet,
                                                                Function4<A,B2,C,D,F> factory) {
        return new PacketCodec<>();
    }
    public static class Unit<B, T> extends PacketCodec<B, T> {
        public Unit(Supplier<T> supplier) {}
    }

    @FunctionalInterface
    public interface Function4<A, B, C, D, R> { R apply(A a, B b, C c, D d); }
}
