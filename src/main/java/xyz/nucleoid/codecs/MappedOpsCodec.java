package xyz.nucleoid.codecs;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.Function;

public final class MappedOpsCodec<A, S> implements Codec<A> {
    private final DynamicOps<S> sourceOps;
    private final Function<A, S> encode;
    private final Function<S, DataResult<A>> decode;

    MappedOpsCodec(DynamicOps<S> sourceOps, Function<A, S> encode, Function<S, DataResult<A>> decode) {
        this.sourceOps = sourceOps;
        this.encode = encode;
        this.decode = decode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        S sourceData = this.encode.apply(input);
        T targetData = ops == this.sourceOps ? (T) sourceData : this.sourceOps.convertTo(ops, sourceData);
        return ops.getMap(targetData).flatMap(map -> {
            return ops.mergeToMap(prefix, map);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        S sourceData = ops == this.sourceOps ? (S) input : ops.convertTo(this.sourceOps, input);
        return this.decode.apply(sourceData).map(output -> Pair.of(output, input));
    }
}
