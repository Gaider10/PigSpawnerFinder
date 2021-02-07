package andrew.spiderfinder;

import java.util.Random;

public class ChunkRandom extends Random {
    protected int sampleCount;

    public ChunkRandom() {
    }

    public ChunkRandom(long seed) {
        super(seed);
    }

    /**
     * Skips the provided number of calls to the randomizer.
     *
     * <p>The skips give the effect of "scrambling" the randomizer but the output is still
     * linearly dependent. Note that since multiple calls to a linear congruential generator is
     * equivalent to another linear congruence, this method could be optimized to combine
     * the calls into one.</p>
     */
    public void consume(int count) {
        for (int j = 0; j < count; ++j) {
            this.next(1);
        }
    }

    @Override
    protected int next(int bound) {
        ++this.sampleCount;
        return super.next(bound);
    }

    /**
     * Seeds the randomizer to generate the surface terrain blocks (such as grass, sand, etc.)
     * and the bedrock patterns.
     *
     * <p>Note that the terrain seed does not depend on the world seed and only gets affected by
     * chunk coordinates.</p>
     */
    public long setTerrainSeed(int chunkX, int chunkZ) {
        long long4 = chunkX * 341873128712L + chunkZ * 132897987541L;
        this.setSeed(long4);
        return long4;
    }

    /**
     * Seeds the randomizer to create population features such as decorators and animals.
     *
     * <p>This method takes in the world seed and the negative-most block coordinates of the
     * chunk. The coordinate pair provided is equivalent to (chunkX * 16, chunkZ * 16). The
     * three values are mixed together through some layers of hashing to produce the
     * population seed.</p>
     *
     * <p>This function has been proved to be reversible through some exploitation of the underlying
     * nextLong() weaknesses. It is also important to remember that since setSeed()
     * truncates the 16 upper bits of world seed, only the 48 lowest bits affect the population
     * seed output.</p>
     */
    public long setPopulationSeed(long worldSeed, int blockX, int blockZ) {
        this.setSeed(worldSeed);
        long long6 = this.nextLong() | 0x1L;
        long long8 = this.nextLong() | 0x1L;
        long long10 = blockX * long6 + blockZ * long8 ^ worldSeed;
        this.setSeed(long10);
        return long10;
    }

    /**
     * Seeds the randomizer to generate a given feature.
     *
     * The salt, in the form of {@code index + 10000 * step} assures that each feature is seeded
     * differently, making the decoration feel more random. Even though it does a good job
     * at doing so, many entropy issues arise from the salt being so small and result in
     * weird alignments between features that have an index close apart.
     * @param populationSeed The population seed computed in setPopulationSeed().
     * @param index The index of the feature in the feature list.
     * @param step The generation step's ordinal for this feature.
     */
    public long setDecoratorSeed(long populationSeed, int index, int step) {
        long long6 = populationSeed + index + 10000 * step;
        this.setSeed(long6);
        return long6;
    }

    /**
     * Seeds the randomizer to generate larger features such as caves, ravines, mineshafts
     * and strongholds. It is also used to initiate structure start behaviour such as rotation.
     *
     * <p>Similar to the population seed, only the 48 lowest bits of the world seed affect the
     * output since it the upper 16 bits are truncated in the setSeed() call.</p>
     */
    public long setCarverSeed(long worldSeed, int chunkX, int chunkZ) {
        this.setSeed(worldSeed);
        long long6 = this.nextLong();
        long long8 = this.nextLong();
        long long10 = chunkX * long6 ^ chunkZ * long8 ^ worldSeed;
        this.setSeed(long10);
        return long10;
    }

    /**
     * Seeds the randomizer to determine the start position of structure features such as
     * temples, monuments and buried treasures within a region.
     *
     * <p>The region coordinates pair corresponds to the coordinates of the region the seeded
     * chunk lies in. For example, a swamp hut region is 32 by 32 chunks meaning that all
     * chunks that lie within that region get seeded the same way.</p>
     *
     * <p>Similarly, the upper 16 bits of world seed also do not affect the region seed because
     * they get truncated in the setSeed() call.</p>
     */
    public long setRegionSeed(long worldSeed, int regionX, int regionZ, int salt) {
        long long7 = regionX * 341873128712L + regionZ * 132897987541L + worldSeed + salt;
        this.setSeed(long7);
        return long7;
    }

    public static Random getSlimeRandom(int chunkX, int chunkZ, long worldSeed, long scrambler) {
        return new Random(worldSeed + chunkX * chunkX * 4987142 + chunkX * 5947611 + chunkZ * chunkZ * 4392871L + chunkZ * 389711 ^ scrambler);
    }
}