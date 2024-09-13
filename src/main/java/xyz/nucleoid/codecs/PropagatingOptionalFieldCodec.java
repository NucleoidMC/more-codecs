package xyz.nucleoid.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.OptionalFieldCodec;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link OptionalFieldCodec} with {@code lenient} set to {@code false}, which additionally
 * compares values against the default value using {@link Objects#equals(Object, Object)}.
 */
@Deprecated
final class PropagatingOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
    private final String name;
    private final Codec<A> elementCodec;

    /**
     * @deprecated Use {@link Codec#optionalFieldOf(Codec, String)}
     */
    @Deprecated
    public PropagatingOptionalFieldCodec(final String name, final Codec<A> elementCodec) {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        T value = input.get(this.name);
        if (value != null) {
            return this.elementCodec.parse(ops, value).map(Optional::of);
        } else {
            return DataResult.success(Optional.empty());
        }
    }

    @Override
    public <T> RecordBuilder<T> encode(final Optional<A> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        if (input.isPresent()) {
            return prefix.add(this.name, this.elementCodec.encodeStart(ops, input.get()));
        }
        return prefix;
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return Stream.of(ops.createString(this.name));
    }
}
