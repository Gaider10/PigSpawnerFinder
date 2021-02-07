package andrew.reverser;

import java.util.ArrayList;

public class CarverReverser {
    private static final long m1 = 25214903917L;
    private static final long a1 = 11L;
    private static final long m2 = 205749139540585L;
    private static final long a2 = 277363943098L;
    private static final long m3 = 233752471717045L;
    private static final long a3 = 11718085204285L;
    private static final long m4 = 55986898099985L;
    private static final long a4 = 49720483695876L;

    private static final long m32 = ((1L << 32) - 1L) << 16;
    private static final long m48 = ((1L << 48) - 1L);

    public static long getPartialXORedCarverSeed(long XORedStructureSeed, int x, int z) {
        long a = ((XORedStructureSeed * m2 + a2) & m48) >>> 16;
        long b = ((XORedStructureSeed * m4 + a4) & m48) >>> 16;
        return x * a ^ z * b ^ XORedStructureSeed;
    }

    public static long getXORedCarverSeed(long XORedStructureSeed, int x, int z) {
        long a = (((XORedStructureSeed * m1 + a1) & m32) << 16) + (long)(int)(((XORedStructureSeed * m2 + a2) & m48) >>> 16);
        long b = (((XORedStructureSeed * m3 + a3) & m32) << 16) + (long)(int)(((XORedStructureSeed * m4 + a4) & m48) >>> 16);
        return (x * a ^ z * b ^ XORedStructureSeed) & m48;
    }

    private static void findBits(long XORedCarverSeed, int x, int z, long XORedSeedBits, int bit, int zeroBits, ArrayList<Long> structureSeeds) {
        if(bit == 32) {
            if(getXORedCarverSeed(XORedSeedBits, x, z) == XORedCarverSeed) {
                structureSeeds.add(XORedSeedBits ^ m1);
            }
        } else {
            long mask = 1L << bit + zeroBits;
            if(((getPartialXORedCarverSeed(XORedSeedBits, x, z) ^ XORedCarverSeed) & mask) == 0) {
                findBits(XORedCarverSeed, x, z, XORedSeedBits, bit + 1, zeroBits, structureSeeds);
            }
            XORedSeedBits |= 1L << bit + 16;
            if(((getPartialXORedCarverSeed(XORedSeedBits, x, z) ^ XORedCarverSeed) & mask) == 0) {
                findBits(XORedCarverSeed, x, z, XORedSeedBits, bit + 1, zeroBits, structureSeeds);
            }
        }
    }

    public static void reverseCarverSeed(long carverSeed, int x, int z, ArrayList<Long> structureSeeds) {
        int zeroBits = 0;
        for(; zeroBits < 16; zeroBits++) {
            if((x & (1 << zeroBits)) != 0 || (z & (1 << zeroBits)) != 0) break;
        }
        int increment = 1 << zeroBits;

        long XORedCarverSeed = carverSeed ^ m1;
        for(long lowest16 = XORedCarverSeed & ((1L << zeroBits) - 1L); lowest16 < (1 << 16); lowest16 += increment) {
            findBits(XORedCarverSeed, x, z, lowest16, 0, zeroBits, structureSeeds);
        }
    }
}
