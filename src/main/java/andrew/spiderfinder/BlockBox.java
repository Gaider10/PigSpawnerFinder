package andrew.spiderfinder;

public class BlockBox {
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public static BlockBox empty() {
        return new BlockBox(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean intersects(BlockBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ && this.maxY >= other.minY && this.minY <= other.maxY;
    }

    public void encompass(BlockBox region) {
        this.minX = Math.min(this.minX, region.minX);
        this.minY = Math.min(this.minY, region.minY);
        this.minZ = Math.min(this.minZ, region.minZ);
        this.maxX = Math.max(this.maxX, region.maxX);
        this.maxY = Math.max(this.maxY, region.maxY);
        this.maxZ = Math.max(this.maxZ, region.maxZ);
    }

    public void offset(int dx, int dy, int dz) {
        this.minX += dx;
        this.minY += dy;
        this.minZ += dz;
        this.maxX += dx;
        this.maxY += dy;
        this.maxZ += dz;
    }

    public int getBlockCountX() {
        return this.maxX - this.minX + 1;
    }

    public int getBlockCountY() {
        return this.maxY - this.minY + 1;
    }

    public int getBlockCountZ() {
        return this.maxZ - this.minZ + 1;
    }
}