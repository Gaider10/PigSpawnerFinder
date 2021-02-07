package randomreverser;

import andrew.reverser.CarverReverser;
import andrew.spiderfinder.*;
import com.google.common.collect.ImmutableSet;
import kaptainwutax.biomeutils.Biome;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.biomeutils.terrain.OverworldChunkGenerator;
import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;

import java.util.ArrayList;

public class PigSpawnerFinder {
    private static final ImmutableSet<Integer> BADLANDS = ImmutableSet.of(Biome.BADLANDS.getId(), Biome.BADLANDS_PLATEAU.getId(), Biome.ERODED_BADLANDS.getId(), Biome.MODIFIED_BADLANDS_PLATEAU.getId(), Biome.MODIFIED_WOODED_BADLANDS_PLATEAU.getId(), Biome.WOODED_BADLANDS_PLATEAU.getId());

    private static void processStructureSeed(long structureSeed, int centerChunkX, int centerChunkZ, int spawnerX, int spawnerY, int spawnerZ) {
        OverworldBiomeSource biomeSource;
        OverworldChunkGenerator chunkGenerator;

        int spawnerChunkX = spawnerX >> 4;
        int spawnerChunkZ = spawnerZ >> 4;

        for(long top = 0; top < (1 << 16); top++) {
            long worldSeed = structureSeed | (top << 48);

            //Check biomes
            biomeSource = new OverworldBiomeSource(MCVersion.v1_14, worldSeed);
            if(!BADLANDS.contains(biomeSource.getBiomeForNoiseGen((centerChunkX << 2) + 2, 0, (centerChunkZ << 2) + 2).getId())) continue;
            if(biomeSource.getBiomeForNoiseGen((spawnerChunkX << 2) + 2, 0, (spawnerChunkZ << 2) + 2) != Biome.BEACH) continue;
//            System.out.println("Good biomes: " + worldSeed);

            //Check depth above spawner
            chunkGenerator = new OverworldChunkGenerator(biomeSource);
            int height = chunkGenerator.getHeightInGround(spawnerX, spawnerZ);
            int depth = height - spawnerY;
//            System.out.println(depth);
            if(depth < 3 || depth > 6) continue;
//            System.out.println("Good central height: " + worldSeed);

            //Check depth nearby to avoid water
            boolean good = true;
            for(int ox = -1; ox <= 1 && good; ox++) {
                for(int oz = -1; oz <= 1; oz++) {
                    height = chunkGenerator.getHeightInGround(spawnerX + ox * 4, spawnerZ + oz * 4);
                    depth = height - spawnerY;
                    if(depth < 3 && height < 62) {
                        good = false;
                        break;
                    }
                }
            }
            if(!good) continue;

            System.out.println("Good nearby height: " + worldSeed + " " + spawnerX + " " + spawnerY + " " + spawnerZ);
        }
    }

    private static void processCarverSeed(long carverSeed, int spawnerCarverX, int spawnerY, int spawnerCarverZ, Direction direction, int length) {
        ChunkRand rand = new ChunkRand();
        ArrayList<Long> structureSeeds = new ArrayList<>();

        int radius = 32768 >> 4;
        int increment = 1 << 5;

        int m = length * 5;
        LCG skipCeiling = LCG.JAVA.combine(m * 3);
        LCG skipCobwebs = LCG.JAVA.combine(m * 3 * 2);
        LCG skip2 = LCG.JAVA.combine(2);
        LCG skip8 = LCG.JAVA.combine(8);
        LCG skip3 = LCG.JAVA.combine(3);


        int spawnerChunkCarverX = spawnerCarverX >> 4;
        int spawnerChunkCarverZ = spawnerCarverZ >> 4;

        int spawnerOffsetX = spawnerCarverX - (spawnerChunkCarverX << 4);
        int spawnerOffsetZ = spawnerCarverZ - (spawnerChunkCarverZ << 4);
        int spawnerOffset = direction.axis == Direction.Axis.X ? spawnerOffsetX : spawnerOffsetZ;

        for(int centerChunkX = -radius; centerChunkX <= radius; centerChunkX += increment) {
            for(int centerChunkZ = -radius; centerChunkZ <= radius; centerChunkZ += increment) {
                int spawnerChunkRealX = centerChunkX + spawnerChunkCarverX;
                int spawnerChunkRealZ = centerChunkZ + spawnerChunkCarverZ;

                structureSeeds.clear();
                CarverReverser.reverseCarverSeed(carverSeed, centerChunkX, centerChunkZ, structureSeeds);
                for(long structureSeed : structureSeeds) {
                    //Check for buried treasure
                    rand.setRegionSeed(structureSeed, spawnerChunkRealX, spawnerChunkRealZ, 10387320, MCVersion.v1_16);
                    if(rand.nextFloat() >= 0.01F) continue;

//                    System.out.println("Good treasure: " + structureSeed);

                    //Check for cobwebs and spawner position
                    //The spawner piece is the first corridor piece generated in this chunk so there are no random calls before it
                    rand.setDecoratorSeed(structureSeed, spawnerChunkRealX << 4, spawnerChunkRealZ << 4, 0, 3, MCVersion.v1_16);

                    //   skip ceiling air blocks
                    rand.advance(skipCeiling);
                    //   skip cobwebs
                    rand.advance(skipCobwebs);
                    //   skip supports
                    if(rand.nextInt(4) != 0) {
                        rand.advance(skip2);
                    }
                    //   skip additional cobwebs
                    rand.advance(skip8);
                    //   skip chests
                    for(int i = 0; i < 2; i++) {
                        if(rand.nextInt(100) == 0) {
                            rand.advance(skip3);
                        }
                    }

                    int spawnerShift = rand.nextInt(3) - 1;
                    int spawnerShiftReal = direction == Direction.NORTH || direction == Direction.WEST ? -spawnerShift : spawnerShift;
//                    System.out.println(spawnerOffset + spawnerShift);
                    if(spawnerOffset + spawnerShiftReal != 9) continue;

                    //Check for no cobwebs near the spawner
                    rand.setDecoratorSeed(structureSeed, spawnerChunkRealX << 4, spawnerChunkRealZ << 4, 0, 3, MCVersion.v1_16);
                    rand.advance(skipCeiling);

                    boolean hasCobwebsNearby = false;
                    for(int y = 0; y < 2 && !hasCobwebsNearby; y++) {
                        for(int x = 0; x < 3 && !hasCobwebsNearby; x++) {
                            for(int z = 0; z < m; z++) {
                                boolean hasCobweb = rand.nextFloat() < 0.6F;
                                if(hasCobweb) {
                                    if(
                                            (y == 1 && x == 1 && z == (2 + spawnerShift)) ||
                                            (y == 0 && x == 2 && z == (2 + spawnerShift)) ||
                                            (y == 0 && x == 0 && z == (2 + spawnerShift)) ||
                                            (y == 0 && x == 1 && z == (2 + spawnerShift + 1)) ||
                                            (y == 0 && x == 1 && z == (2 + spawnerShift - 1))
                                    ) {
                                        hasCobwebsNearby = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if(hasCobwebsNearby) continue;

                    int spawnerRealX = (spawnerChunkRealX << 4) + 9;
                    int spawnerRealZ = (spawnerChunkRealZ << 4) + 9;

                    System.out.println("Structure: " + structureSeed + " Center: " + centerChunkX + " " + centerChunkZ + " Spawner: " + spawnerRealX + " " + spawnerY + " " + spawnerRealZ);
                    processStructureSeed(structureSeed, centerChunkX, centerChunkZ, spawnerRealX, spawnerY, spawnerRealZ);
                }
            }
        }
    }

    private static void findCarvers() {
        ArrayList<Spawner> spawners = new ArrayList<>();

        for(long carverSeed = 1796831724L; carverSeed < (1L << 48); carverSeed++) {
            ArrayList<StructurePiece> pieces = MineshaftGenerator.generateForChunk(carverSeed, 0, 0, true, spawners);
            for(Spawner spawner : spawners) {
                //Check spawner height
                if(spawner.y < 58 || spawner.y > 59) continue;

                //Check if it can be at 9 9 but not near supports
                int offsetChunkX = spawner.x >> 4;
                int offsetChunkZ = spawner.z >> 4;
                int spawnerOffsetX = spawner.x - (offsetChunkX << 4);
                int spawnerOffsetZ = spawner.z - (offsetChunkZ << 4);
                if(!(spawner.direction.axis == Direction.Axis.X && spawnerOffsetZ == 9 && (spawnerOffsetX == 8 || spawnerOffsetX == 10)) && !(spawner.direction.axis == Direction.Axis.Z && spawnerOffsetX == 9 && (spawnerOffsetZ == 8 || spawnerOffsetZ == 10))) continue;

                //Check if it isn't too close to mesa
                if(Math.abs(offsetChunkX) < 2 && Math.abs(offsetChunkZ) < 2) continue;

                //Check if there are no other corridors in the same chunk generated before the one with the spawner (meaning no random calls before our corridor)
                int piecesBeforeSpawner = 0;
                BlockBox spawnerBox = new BlockBox(spawner.x, spawner.y, spawner.z, spawner.x, spawner.y, spawner.z);
                BlockBox chunk = new BlockBox(offsetChunkX << 4, 0, offsetChunkZ << 4, (offsetChunkX << 4) + 15, 255, (offsetChunkZ << 4) + 15);
                for(StructurePiece piece : pieces) {
                    if(piece.boundingBox.intersects(chunk)) {
                        if(piece.boundingBox.intersects(spawnerBox)) break;
                        else if (piece instanceof MineshaftGenerator.MineshaftCorridor) {
                            piecesBeforeSpawner = 1;
                            break;
                        }
                    }
                }
//                System.out.println(piecesBeforeSpawner);
                if(piecesBeforeSpawner != 0) continue;

                System.out.println("Carver: " + carverSeed + " Spawner: " + spawner.x + " " + spawner.y + " " + spawner.z + " " + spawner.direction + " " + spawner.length);
                processCarverSeed(carverSeed, spawner.x, spawner.y, spawner.z, spawner.direction, spawner.length);
            }
            spawners.clear();
        }
    }

    //Good nearby height: -5364344938332933183 2137 59 9   132 5
    //Good nearby height: -2211262249220164671 2137 59 9   132 5
    //Good nearby height: -288506683309673535 2137 59 9   132 5

    //Good nearby height: 8430151434447953355 2153 59 521   133 37
    //Good nearby height: -7959854984436834869 2153 59 521   133 37

    public static void main(String[] args) {
        findCarvers();
//        processCarverSeed(601266, -56, 59, 57, Direction.WEST, 3);
//        processStructureSeed(278134859145650L, 48640, 13056, 778185, 59, 208953);
    }

    //CLOSEST QUINTUPLE
    //Good nearby height: -2464334185163669257 6105 58 18969

    //CLOSE QUINTUPLE
    //Good nearby height: 5520728315301704325 -18471 59 -15335
    //Good nearby height: 3830679590341196603 -28663 58 -13255
    //Good nearby height: -8506231491360895631 29209 59 -25655
    //Good nearby height: 5080722595853041613 -21463 59 -11255
    //Good nearby height: 8558345933113196493 -21463 59 -11255
    //Good nearby height: 6432693858766937472 -16423 58 -10807
    //Good nearby height: -7777570340470530688 -16423 58 -10807
    //Good nearby height: -8964393524132048897 16409 58 12761

    //FAR QUINTUPLE:
    //Good nearby height: 6890415600180670898 5410761 59 3883065
    //Good nearby height: -2732087953649815118 5410761 59 3883065
    //Good nearby height: 8677973874565685682 6967241 59 2375737
    //Good nearby height: -3586538451014376014 7069641 59 5009465
    //Good nearby height: 5801462954023181234 7438281 59 4223033
    //Good nearby height: 2686338580661244338 8396745 59 1593401
    //Good nearby height: -3433007311031115598 9867209 59 7831609
    //Good nearby height: 13313117911499698 10883017 59 3985465
    //Good nearby height: -5281794143969360974 10883017 59 3985465
    //Good nearby height: -3230123038725389390 10883017 59 3985465
    //Good nearby height: -731751145441606734 10883017 59 3985465
    //Good nearby height: 2688172202216015794 11648969 59 6508601
    //Good nearby height: -1359156487906506830 11648969 59 6508601
}
