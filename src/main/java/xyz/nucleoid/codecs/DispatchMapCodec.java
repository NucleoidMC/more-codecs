package xyz.nucleoid.codecs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;

import java.util.Map;
import java.util.function.Function;

final class DispatchMapCodec<K, V> implements Codec<Map<K, V>> {
    private final Codec<K> keyCodec;
    private final Function<K, Codec<V>> valueCodec;

    DispatchMapCodec(Codec<K> keyCodec, Function<K, Codec<V>> valueCodec) {
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Override
    public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).flatMap(mapInput -> {
            var read = ImmutableMap.<K, V>builder();
            var failed = ImmutableList.<Pair<T, T>>builder();

            var result = mapInput.entries().reduce(
                    DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
                    (r, pair) -> {
                        var keyResult = this.keyCodec.parse(ops, pair.getFirst());
                        keyResult.error().ifPresent(e -> failed.add(pair));

                        return keyResult.flatMap(key -> {
                            var entry = this.valueCodec.apply(key).parse(ops, pair.getSecond())
                                    .map(value -> Pair.of(key, value));
                            entry.error().ifPresent(e -> failed.add(pair));

                            return r.apply2stable((u, p) -> {
                                read.put(p.getFirst(), p.getSecond());
                                return u;
                            }, entry);
                        });
                    },
                    (r1, r2) -> r1.apply2stable((u1, u2) -> u1, r2)
            );

            Map<K, V> elements = read.build();
            T errors = ops.createMap(failed.build().stream());

            return result.map(unit -> Pair.of(elements, input))
                    .setPartial(Pair.of(elements, input))
                    .mapError(e -> e + " missed input: " + errors);
        });
    }

    @Override
    public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
        RecordBuilder<T> map = ops.mapBuilder();
        for (Map.Entry<K, V> entry : input.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            map.add(this.keyCodec.encodeStart(ops, key), this.valueCodec.apply(key).encodeStart(ops, value));
        }
        return map.build(prefix);
    }
}
