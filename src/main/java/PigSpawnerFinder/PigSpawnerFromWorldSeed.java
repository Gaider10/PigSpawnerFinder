package PigSpawnerFinder;

import PigSpawnerFinder.spiderfinder.*;
import kaptainwutax.biomeutils.biome.Biomes;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.rand.seed.WorldSeed;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.terrainutils.terrain.OverworldChunkGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import static PigSpawnerFinder.PigSpawnerFinder.processCarverSeed;


public class PigSpawnerFromWorldSeed {
	// this is non optimized
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter worldseed : ");
		String ws = scanner.nextLine();
		long worldSeed;
		try {
			worldSeed = Long.parseLong(ws);
		} catch (NumberFormatException e) {
			worldSeed = ws.hashCode();
			System.err.println("You inputted a wrong world seed, we converted it to a string one " + worldSeed);
		}
		System.out.println("Using worldseed : " + worldSeed);
		System.out.println("Enter half size to search for (in chunks) : ");
		String sz = scanner.nextLine();
		int size;
		try {
			size = Integer.parseInt(sz);
		} catch (NumberFormatException e) {
			System.out.println("Sorry you inputed a wrong size (too large or something)");
			e.printStackTrace();
			return;
		}
		System.out.printf("Searching an area of %dx%d chunks\n",size*2,size*2);
		for (int chunkX = -size; chunkX < size; chunkX++) {
			int finalChunkX = chunkX;
			long finalWorldSeed = worldSeed;
//			IntStream.range(-size, size).parallel().forEach(
//					chunkZ -> processForChunk(finalWorldSeed, finalChunkX, chunkZ)
//			);

			for (int chunkZ = -size; chunkZ < size; chunkZ++) {
				processForChunk(-2464334185163669257L, finalChunkX, chunkZ);
			}
		}
		System.out.println("We are done, if you didn't see any STEP X (from 1 to 3, 3 being the final true one)\n" +
				"Then you have a non possible worldseed or a too small of an area, remember those are rare.");
	}


	public static void processForChunk(long worldSeed, int chunkX, int chunkZ) {
		System.out.println(chunkX+ " "+chunkZ);
		ArrayList<Spawner> spawners = new ArrayList<>();
		// get the spawner from the mineshaft
		ArrayList<StructurePiece> pieces = MineshaftGenerator.generateForChunk(
				WorldSeed.toStructureSeed(worldSeed),
				chunkX, chunkZ, true, spawners
		);
		for (Spawner spawner : spawners) {

			//Check spawner height
			if (spawner.y < 58 || spawner.y > 59) continue;

			//Check if it can be at 9 9 but not near supports
			int spawnerChunkX = spawner.x >> 4;
			int spawnerChunkZ = spawner.z >> 4;
			int spawnerOffsetX = spawner.x & 15;
			int spawnerOffsetZ = spawner.z & 15;
			if (spawner.direction.axis == Direction.Axis.X && spawnerOffsetZ == 9 && (spawnerOffsetX == 8 || spawnerOffsetX == 10) ||
					spawner.direction.axis == Direction.Axis.Z && spawnerOffsetX == 9 && (spawnerOffsetZ == 8 || spawnerOffsetZ == 10)) {

				//Check if it isn't too close to mesa
				if (Math.abs(spawnerChunkX) < 2 && Math.abs(spawnerChunkZ) < 2) continue;

				//Check if there are no other corridors in the same chunk generated before the one with the spawner (meaning no random calls before our corridor)
				int piecesBeforeSpawner = 0;
				BlockBox spawnerBox = new BlockBox(spawner.x, spawner.y, spawner.z, spawner.x, spawner.y, spawner.z);
				BlockBox chunk = new BlockBox(spawnerChunkX << 4, 0, spawnerChunkZ << 4, (spawnerChunkX << 4) + 15, 255, (spawnerChunkZ << 4) + 15);
				for (StructurePiece piece : pieces) {
					if (piece.boundingBox.intersects(chunk)) {
						if (piece.boundingBox.intersects(spawnerBox)) break;
						else if (piece instanceof MineshaftGenerator.MineshaftCorridor) {
							piecesBeforeSpawner = 1;
							break;
						}
					}
				}
				if (piecesBeforeSpawner != 0) continue;

				System.out.println("STEP 1: Spawner could work for : " + spawner);
				findOutIfCorrect(worldSeed, spawner);
			}
		}
	}

	public static void findOutIfCorrect(long worldseed, Spawner spawner) {
		ChunkRand rand = new ChunkRand();
		// everything here can be handled with the structure seed only (the lower 48 bits)
		long structureSeed = WorldSeed.toStructureSeed(worldseed);

		int m = spawner.length * 5;
		LCG skipCeiling = LCG.JAVA.combine(m * 3L);
		LCG skipCobwebs = LCG.JAVA.combine(m * 3L * 2L);
		LCG skip2 = LCG.JAVA.combine(2);
		LCG skip8 = LCG.JAVA.combine(8);
		LCG skip3 = LCG.JAVA.combine(3);


		int spawnerChunkX = spawner.x >> 4;
		int spawnerChunkZ = spawner.z >> 4;

		int spawnerOffsetX = spawner.x & 15;
		int spawnerOffsetZ = spawner.z & 15;
		int spawnerOffset = spawner.direction.axis == Direction.Axis.X ? spawnerOffsetX : spawnerOffsetZ;


		//Check for buried treasure
		rand.setRegionSeed(structureSeed, spawnerChunkX, spawnerChunkZ, 10387320, MCVersion.v1_16);
		if (rand.nextFloat() >= 0.01F) return;


		//Check for cobwebs and spawner position
		//The spawner piece is the first corridor piece generated in this chunk so there are no random calls before it
		// the index and step are super specific to 1.16 (please document yourself)
		rand.setDecoratorSeed(structureSeed, spawnerChunkX << 4, spawnerChunkZ << 4, 0, 3, MCVersion.v1_16);

		//   skip ceiling air blocks
		rand.advance(skipCeiling);
		//   skip cobwebs
		rand.advance(skipCobwebs);
		//   skip supports
		if (rand.nextInt(4) != 0) {
			rand.advance(skip2);
		}
		//   skip additional cobwebs
		rand.advance(skip8);
		//   skip chests
		for (int i = 0; i < 2; i++) {
			if (rand.nextInt(100) == 0) {
				rand.advance(skip3);
			}
		}

		int spawnerShift = rand.nextInt(3) - 1;
		int spawnerShiftReal = spawner.direction == Direction.NORTH || spawner.direction == Direction.WEST ? -spawnerShift : spawnerShift;
		if (spawnerOffset + spawnerShiftReal != 9) return;

		//Check for no cobwebs near the spawner
		// the index and step are super specific to 1.16 (please document yourself)
		rand.setDecoratorSeed(structureSeed, spawnerChunkX << 4, spawnerChunkZ << 4, 0, 3, MCVersion.v1_16);
		rand.advance(skipCeiling);

		boolean hasCobwebsNearby = false;
		for (int y = 0; y < 2 && !hasCobwebsNearby; y++) {
			for (int x = 0; x < 3 && !hasCobwebsNearby; x++) {
				for (int z = 0; z < m; z++) {
					boolean hasCobweb = rand.nextFloat() < 0.6F;
					if (hasCobweb) {
						if (
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
		if (hasCobwebsNearby) return;

		BPos spawnerPos = new BPos((spawnerChunkX << 4) + 9, spawner.y, (spawnerChunkZ << 4) + 9);
		System.out.println("STEP 2: Spawner passed the buried treasure test : " + spawner);
		processWorldSeed(worldseed, spawnerPos);

	}

	private static final Set<Integer> BADLANDS = new HashSet<Integer>() {{
		add(Biomes.BADLANDS.getId());
		add(Biomes.BADLANDS_PLATEAU.getId());
		add(Biomes.ERODED_BADLANDS.getId());
		add(Biomes.MODIFIED_BADLANDS_PLATEAU.getId());
		add(Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU.getId());
		add(Biomes.WOODED_BADLANDS_PLATEAU.getId());
	}};


	public static void processWorldSeed(long worldSeed, BPos spawnerPos) {
		OverworldBiomeSource biomeSource;
		OverworldChunkGenerator chunkGenerator;
		CPos spawnerChunkPos = spawnerPos.toChunkPos();
		//Check biomes
		biomeSource = new OverworldBiomeSource(MCVersion.v1_16, worldSeed);
		// those two checks are super intensive, that's why we do it at last
		if (!BADLANDS.contains(biomeSource.getBiomeForNoiseGen((spawnerChunkPos.getX() << 2) + 2, 0, (spawnerChunkPos.getX() << 2) + 2).getId())) return;
		if (biomeSource.getBiomeForNoiseGen((spawnerChunkPos.getX() << 2) + 2, 0, (spawnerChunkPos.getZ() << 2) + 2) != Biomes.BEACH) return;
//            System.out.println("Good biomes: " + worldSeed);

		//Check depth above spawner
		chunkGenerator = new OverworldChunkGenerator(biomeSource);
		int height = chunkGenerator.getHeightInGround(spawnerPos.getX(), spawnerPos.getZ());
		int depth = height - spawnerPos.getY();
		if (depth < 3 || depth > 6) return;

		//Check depth nearby to avoid water
		boolean good = true;
		for (int ox = -1; ox <= 1 && good; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				height = chunkGenerator.getHeightInGround(spawnerPos.getX() + ox * 4, spawnerPos.getZ() + oz * 4);
				depth = height - spawnerPos.getY();
				if (depth < 3 && height < 62) {
					good = false;
					break;
				}
			}
		}
		if (!good) return;

		System.out.printf("STEP 3 (FINAL) : Found spawner : /tp @p %d %d %d for worldseed %d\n",
				spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ(), worldSeed);
	}
}
