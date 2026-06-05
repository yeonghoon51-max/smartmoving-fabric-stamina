package net.minecraft.network.codec;
import java.util.function.Function;
import net.minecraft.util.function.Function4;

public interface PacketCodec<B, V> {
    static <B, F1, V> PacketCodec<B, V> tuple(
            PacketCodec<? super B, F1> c1, Function<V, F1> g1,
            Function<F1, V> ctor) {
        return null;
    }

    static <B, F1, F2, F3, F4, V> PacketCodec<B, V> tuple(
            PacketCodec<? super B, F1> c1, Function<V, F1> g1,
            PacketCodec<? super B, F2> c2, Function<V, F2> g2,
            PacketCodec<? super B, F3> c3, Function<V, F3> g3,
            PacketCodec<? super B, F4> c4, Function<V, F4> g4,
            Function4<F1, F2, F3, F4, V> ctor) {
        return null;
    }
}
