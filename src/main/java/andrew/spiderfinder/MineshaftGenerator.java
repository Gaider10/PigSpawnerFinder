package andrew.spiderfinder;

import java.util.ArrayList;
import java.util.Random;

public class MineshaftGenerator {
    public static ArrayList<StructurePiece> generateForChunk(long worldSeed, int chunkX, int chunkZ, boolean mesa, ArrayList<Spawner> spawners) {
        ChunkRandom random = new ChunkRandom();
        long s = random.setCarverSeed(worldSeed, chunkX, chunkZ);

        if(random.nextDouble() < 0.004D) {
            random.setSeed(s);
            return generate(random, chunkX, chunkZ, mesa, spawners);
        }

        return new ArrayList<>();
    }

    public static ArrayList<StructurePiece> generate(Random random, int chunkX, int chunkZ, boolean mesa, ArrayList<Spawner> spawners) {
        ArrayList<StructurePiece> children = new ArrayList<>();
        
        MineshaftGenerator.MineshaftRoom mineshaftRoom = new MineshaftGenerator.MineshaftRoom(0, random, (chunkX << 4) + 2, (chunkZ << 4) + 2);
        children.add(mineshaftRoom);
        mineshaftRoom.placeJigsaw(mineshaftRoom, children, random);

        BlockBox boundingBox = BlockBox.empty();
        for(StructurePiece structurePiece : children){
            boundingBox.encompass(structurePiece.boundingBox);
        }

        int m;

        if(mesa) {
            m = 63 - boundingBox.maxY + boundingBox.getBlockCountY() / 2 + 5;
        } else {
            int l = boundingBox.getBlockCountY() + 1;
            if (l < 53) l += random.nextInt(53 - l);
            m = l - boundingBox.maxY;
        }

        boundingBox.offset(0, m, 0);

        for(StructurePiece structurePiece : children){
            structurePiece.translate(0, m, 0);
            if(structurePiece instanceof MineshaftCorridor && ((MineshaftCorridor)structurePiece).hasCobwebs){
                int y = structurePiece.applyYTransform(0);
                int x = structurePiece.applyXTransform(1, 2);
                int z = structurePiece.applyZTransform(1, 2);
                int length;
                if (structurePiece.facing.axis == Direction.Axis.Z) {
                    length = structurePiece.boundingBox.getBlockCountZ() / 5;
                } else {
                    length = structurePiece.boundingBox.getBlockCountX() / 5;
                }
                spawners.add(new Spawner(x, y, z, structurePiece.facing, length));
            }
        }

        return children;
    }

    public static StructurePiece getRandomJigsaw(ArrayList<StructurePiece> ArrayList, Random random, int i, int j, int k, Direction direction, int l) {
        int m = random.nextInt(100);
        BlockBox blockBox2;
        if (m >= 80) {
            blockBox2 = MineshaftGenerator.MineshaftCrossing.getBoundingBox(ArrayList, random, i, j, k, direction);
            if (blockBox2 != null) {
                return new MineshaftGenerator.MineshaftCrossing(l, blockBox2, direction);
            }
        } else if (m >= 70) {
            blockBox2 = MineshaftGenerator.MineshaftStairs.getBoundingBox(ArrayList, i, j, k, direction);
            if (blockBox2 != null) {
                return new MineshaftGenerator.MineshaftStairs(l, blockBox2, direction);
            }
        } else {
            blockBox2 = MineshaftGenerator.MineshaftCorridor.getBoundingBox(ArrayList, random, i, j, k, direction);
            if (blockBox2 != null) {
                return new MineshaftGenerator.MineshaftCorridor(l, random, blockBox2, direction);
            }
        }

        return null;
    }

    private static void tryGenerateJigsaw(StructurePiece structurePiece, ArrayList<StructurePiece> ArrayList, Random random, int i, int j, int k, Direction direction, int l) {
        if (l <= 8 && Math.abs(i - structurePiece.boundingBox.minX) <= 80 && Math.abs(k - structurePiece.boundingBox.minZ) <= 80) {
            StructurePiece mineshaftPart = getRandomJigsaw(ArrayList, random, i, j, k, direction, l + 1);
            if (mineshaftPart != null) {
                ArrayList.add(mineshaftPart);
                mineshaftPart.placeJigsaw(structurePiece, ArrayList, random);
            }
        }
    }

    public static class MineshaftStairs extends StructurePiece {
        public MineshaftStairs(int i, BlockBox blockBox, Direction direction) {
            super(i);
            facing = direction;
            boundingBox = blockBox;
        }

        public static BlockBox getBoundingBox(ArrayList<StructurePiece> ArrayList, int i, int j, int k, Direction direction) {
            BlockBox blockBox = new BlockBox(i, j - 5, k, i, j + 3 - 1, k);
            switch(direction) {
                case NORTH:
                default:
                    blockBox.maxX = i + 3 - 1;
                    blockBox.minZ = k - 8;
                    break;
                case SOUTH:
                    blockBox.maxX = i + 3 - 1;
                    blockBox.maxZ = k + 8;
                    break;
                case WEST:
                    blockBox.minX = i - 8;
                    blockBox.maxZ = k + 3 - 1;
                    break;
                case EAST:
                    blockBox.maxX = i + 8;
                    blockBox.maxZ = k + 3 - 1;
            }

            return StructurePiece.getOverlappingPiece(ArrayList, blockBox) != null ? null : blockBox;
        }

        public void placeJigsaw(StructurePiece structurePiece, ArrayList<StructurePiece> ArrayList, Random random) {
            if (facing != null) {
                switch(facing) {
                    case NORTH:
                    default:
                        MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY, boundingBox.minZ - 1, Direction.NORTH, length);
                        break;
                    case SOUTH:
                        MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY, boundingBox.maxZ + 1, Direction.SOUTH, length);
                        break;
                    case WEST:
                        MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY, boundingBox.minZ, Direction.WEST, length);
                        break;
                    case EAST:
                        MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY, boundingBox.minZ, Direction.EAST, length);
                }
            }

        }
    }

    public static class MineshaftCrossing extends StructurePiece {
        private final Direction direction;
        private final boolean twoFloors;

        public MineshaftCrossing(int i, BlockBox blockBox, Direction direction) {
            super(i);
            this.direction = direction;
            boundingBox = blockBox;
            twoFloors = blockBox.getBlockCountY() > 3;
        }

        public static BlockBox getBoundingBox(ArrayList<StructurePiece> ArrayList, Random random, int i, int j, int k, Direction facing) {
            BlockBox blockBox = new BlockBox(i, j, k, i, j + 3 - 1, k);
            if (random.nextInt(4) == 0) {
                blockBox.maxY += 4;
            }

            switch(facing) {
                case NORTH:
                default:
                    blockBox.minX = i - 1;
                    blockBox.maxX = i + 3;
                    blockBox.minZ = k - 4;
                    break;
                case SOUTH:
                    blockBox.minX = i - 1;
                    blockBox.maxX = i + 3;
                    blockBox.maxZ = k + 3 + 1;
                    break;
                case WEST:
                    blockBox.minX = i - 4;
                    blockBox.minZ = k - 1;
                    blockBox.maxZ = k + 3;
                    break;
                case EAST:
                    blockBox.maxX = i + 3 + 1;
                    blockBox.minZ = k - 1;
                    blockBox.maxZ = k + 3;
            }

            return StructurePiece.getOverlappingPiece(ArrayList, blockBox) != null ? null : blockBox;
        }

        public void placeJigsaw(StructurePiece structurePiece, ArrayList<StructurePiece> ArrayList, Random random) {
            switch(this.direction) {
                case NORTH:
                default:
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.minZ - 1, Direction.NORTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY, boundingBox.minZ + 1, Direction.WEST, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY, boundingBox.minZ + 1, Direction.EAST, length);
                    break;
                case SOUTH:
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.maxZ + 1, Direction.SOUTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY, boundingBox.minZ + 1, Direction.WEST, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY, boundingBox.minZ + 1, Direction.EAST, length);
                    break;
                case WEST:
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.minZ - 1, Direction.NORTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.maxZ + 1, Direction.SOUTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY, boundingBox.minZ + 1, Direction.WEST, length);
                    break;
                case EAST:
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.minZ - 1, Direction.NORTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY, boundingBox.maxZ + 1, Direction.SOUTH, length);
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY, boundingBox.minZ + 1, Direction.EAST, length);
            }

            if (twoFloors) {
                if (random.nextBoolean()) {
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY + 3 + 1, boundingBox.minZ - 1, Direction.NORTH, length);
                }

                if (random.nextBoolean()) {
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY + 3 + 1, boundingBox.minZ + 1, Direction.WEST, length);
                }

                if (random.nextBoolean()) {
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY + 3 + 1, boundingBox.minZ + 1, Direction.EAST, length);
                }

                if (random.nextBoolean()) {
                    MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + 1, boundingBox.minY + 3 + 1, boundingBox.maxZ + 1, Direction.SOUTH, length);
                }
            }

        }
    }

    public static class MineshaftCorridor extends StructurePiece {
        private final boolean hasCobwebs;

        public MineshaftCorridor(int i, Random random, BlockBox blockBox, Direction direction) {
            super(i);
            facing = direction;
            boundingBox = blockBox;
            hasCobwebs = random.nextInt(3) != 0 && random.nextInt(23) == 0;
        }

        public static BlockBox getBoundingBox(ArrayList<StructurePiece> ArrayList, Random random, int i, int j, int k, Direction direction) {
            BlockBox blockBox = new BlockBox(i, j, k, i, j + 3 - 1, k);
            int l;
            for(l = random.nextInt(3) + 2; l > 0; --l) {
                int m = l * 5;
                switch(direction) {
                    case NORTH:
                    default:
                        blockBox.maxX = i + 3 - 1;
                        blockBox.minZ = k - (m - 1);
                        break;
                    case SOUTH:
                        blockBox.maxX = i + 3 - 1;
                        blockBox.maxZ = k + m - 1;
                        break;
                    case WEST:
                        blockBox.minX = i - (m - 1);
                        blockBox.maxZ = k + 3 - 1;
                        break;
                    case EAST:
                        blockBox.maxX = i + m - 1;
                        blockBox.maxZ = k + 3 - 1;
                }

                if (StructurePiece.getOverlappingPiece(ArrayList, blockBox) == null) {
                    break;
                }
            }
            return l > 0 ? blockBox : null;
        }

        public void placeJigsaw(StructurePiece structurePiece, ArrayList<StructurePiece> ArrayList, Random random) {
            int j = random.nextInt(4);
            if (facing != null) {
                switch(facing) {
                    case NORTH:
                    default:
                        if (j <= 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ - 1, facing, length);
                        } else if (j == 2) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ, Direction.WEST, length);
                        } else {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ, Direction.EAST, length);
                        }
                        break;
                    case SOUTH:
                        if (j <= 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY - 1 + random.nextInt(3), boundingBox.maxZ + 1, facing, length);
                        } else if (j == 2) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.maxZ - 3, Direction.WEST, length);
                        } else {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.maxZ - 3, Direction.EAST, length);
                        }
                        break;
                    case WEST:
                        if (j <= 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ, facing, length);
                        } else if (j == 2) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ - 1, Direction.NORTH, length);
                        } else {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX, boundingBox.minY - 1 + random.nextInt(3), boundingBox.maxZ + 1, Direction.SOUTH, length);
                        }
                        break;
                    case EAST:
                        if (j <= 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ, facing, length);
                        } else if (j == 2) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX - 3, boundingBox.minY - 1 + random.nextInt(3), boundingBox.minZ - 1, Direction.NORTH, length);
                        } else {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX - 3, boundingBox.minY - 1 + random.nextInt(3), boundingBox.maxZ + 1, Direction.SOUTH, length);
                        }
                }
            }

            if (length < 8) {
                int k;
                int l;
                if (facing != Direction.NORTH && facing != Direction.SOUTH) {
                    for(k = boundingBox.minX + 3; k + 3 <= boundingBox.maxX; k += 5) {
                        l = random.nextInt(5);
                        if (l == 0) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, k, boundingBox.minY, boundingBox.minZ - 1, Direction.NORTH, length + 1);
                        } else if (l == 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, k, boundingBox.minY, boundingBox.maxZ + 1, Direction.SOUTH, length + 1);
                        }
                    }
                } else {
                    for(k = boundingBox.minZ + 3; k + 3 <= boundingBox.maxZ; k += 5) {
                        l = random.nextInt(5);
                        if (l == 0) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY, k, Direction.WEST, length + 1);
                        } else if (l == 1) {
                            MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY, k, Direction.EAST, length + 1);
                        }
                    }
                }
            }
        }
    }

    public static class MineshaftRoom extends StructurePiece {
        public MineshaftRoom(int i, Random random, int j, int k) {
            super(i);
            boundingBox = new BlockBox(j, 50, k, j + 7 + random.nextInt(6), 54 + random.nextInt(6), k + 7 + random.nextInt(6));
        }

        public void placeJigsaw(StructurePiece structurePiece, ArrayList<StructurePiece> ArrayList, Random random) {
            int j = boundingBox.getBlockCountY() - 3 - 1;
            if (j <= 0) {
                j = 1;
            }

            int k;
            for(k = 0; k < boundingBox.getBlockCountX(); k += 4) {
                k += random.nextInt(boundingBox.getBlockCountX());
                if (k + 3 > boundingBox.getBlockCountX()) {
                    break;
                }

                MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + k, boundingBox.minY + random.nextInt(j) + 1, boundingBox.minZ - 1, Direction.NORTH, length);
            }

            for(k = 0; k < boundingBox.getBlockCountX(); k += 4) {
                k += random.nextInt(boundingBox.getBlockCountX());
                if (k + 3 > boundingBox.getBlockCountX()) {
                    break;
                }

                MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX + k, boundingBox.minY + random.nextInt(j) + 1, boundingBox.maxZ + 1, Direction.SOUTH, length);
            }

            for(k = 0; k < boundingBox.getBlockCountZ(); k += 4) {
                k += random.nextInt(boundingBox.getBlockCountZ());
                if (k + 3 > boundingBox.getBlockCountZ()) {
                    break;
                }

                MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.minX - 1, boundingBox.minY + random.nextInt(j) + 1, boundingBox.minZ + k, Direction.WEST, length);
            }

            for(k = 0; k < boundingBox.getBlockCountZ(); k += 4) {
                k += random.nextInt(boundingBox.getBlockCountZ());
                if (k + 3 > boundingBox.getBlockCountZ()) {
                    break;
                }

                MineshaftGenerator.tryGenerateJigsaw(structurePiece, ArrayList, random, boundingBox.maxX + 1, boundingBox.minY + random.nextInt(j) + 1, boundingBox.minZ + k, Direction.EAST, length);
            }
        }
    }
}