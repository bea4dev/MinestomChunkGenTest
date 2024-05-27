package com.github.bea4dev;

import de.articdive.jnoise.generators.noisegen.opensimplex.FastSimplexNoiseGenerator;
import de.articdive.jnoise.pipeline.JNoise;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TestGenerator implements Generator {
    private final long seed = 0;
    private final List<Block> layers;

    public TestGenerator() {
        var layers = new ArrayList<Block>();
        for (var i = 1; i <= 8; i++) {
            layers.add(Block.SNOW.withProperty("layers", String.valueOf(i)));
        }
        this.layers = layers;
    }

    private final ThreadLocal<JNoise> baseNoise = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed).build())
                .scale(0.005)
                .build()
    );
    private final ThreadLocal<JNoise> shapeNoise1 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 100).build())
                .scale(0.01)
                .build()
    );
    private final ThreadLocal<JNoise> shapeNoise2 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 200).build())
                .scale(0.002)
                .build()
    );
    private final ThreadLocal<JNoise> detailNoise1 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 300).build())
                .scale(0.003)
                .build()
    );
    private final ThreadLocal<JNoise> detailNoise2 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 400).build())
                .scale(0.02)
                .build()
    );
    private final ThreadLocal<JNoise> caveNoise1 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 500).build())
                .scale(0.01)
                .build()
    );
    private final ThreadLocal<JNoise> caveNoise2 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 600).build())
                .scale(0.02)
                .build()
    );
    private final ThreadLocal<JNoise> caveNoise3 = ThreadLocal.withInitial(() ->
        JNoise.newBuilder()
                .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(seed + 700).build())
                .scale(0.05)
                .build()
    );

    @Override
    public void generate(@NotNull GenerationUnit unit) {
        var baseNoise = this.baseNoise.get();
        var shapeNoise1 = this.shapeNoise1.get();
        var shapeNoise2 = this.shapeNoise2.get();
        var detailNoise1 = this.detailNoise1.get();
        var detailNoise2 = this.detailNoise2.get();
        var caveNoise1 = this.caveNoise1.get();
        var caveNoise2 = this.caveNoise2.get();
        var caveNoise3 = this.caveNoise3.get();

        var start = unit.absoluteStart();
        for (var x = 0; x < unit.size().blockX(); x++) {
            for (var z = 0; z < unit.size().blockZ(); z++) {
                var bottom = start.add(x, 0.0, z);

                var landHeight = baseNoise.evaluateNoise(bottom.x(), bottom.z()) * 16 + 64;

                var shapeValue1 = shapeNoise1.evaluateNoise(bottom.x(), bottom.z()) * 8 + 8;
                if (shapeValue1 > 8) {
                    landHeight += shapeValue1;
                }

                var shapeValue2 = shapeNoise2.evaluateNoise(bottom.x(), bottom.z()) * 4;
                if (shapeValue2 < -2 && shapeValue1 <= 8) {
                    landHeight += shapeValue2;
                }

                var detailValue1 = detailNoise1.evaluateNoise(bottom.x(), bottom.z()) * 1.5;
                if (shapeValue1 > 8) {
                    landHeight += detailValue1;
                }

                var detailValue = detailNoise2.evaluateNoise(bottom.x(), bottom.z()) * 1.5;
                landHeight += detailValue;

                for (var y = 0; y < unit.size().blockY(); y++) {
                    var position = start.add(x, y, z);

                    var caveValue = caveNoise1.evaluateNoise(position.x(), position.y(), position.z()) +
                            caveNoise2.evaluateNoise(position.x(), position.y(), position.z()) +
                            caveNoise3.evaluateNoise(position.x(), position.y(), position.z()) / 2;

                    boolean isCave;
                    if (position.y() < landHeight) {
                        isCave = caveValue > 1.2;
                    } else {
                        isCave = caveValue > 1.5;
                    }

                    if (position.y() < ((int) landHeight) - 1) {
                        if (!isCave) {
                            unit.modifier().setBlock(position, Block.STONE);
                        }
                    }

                    if (position.blockY() == ((int) landHeight) - 1) {
                        if (isCave) {
                            unit.modifier().setBlock(position, Block.POWDER_SNOW);
                        } else {
                            unit.modifier().setBlock(position, Block.SNOW_BLOCK);
                        }
                    }
                }

                var layerIndex = (int) ((landHeight - Math.floor(landHeight)) * 8);
                var layer = layers.get(layerIndex);
                unit.modifier().setBlock(bottom.withY(landHeight), layer);
            }
        }
    }
}
