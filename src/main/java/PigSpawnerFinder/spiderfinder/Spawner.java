package PigSpawnerFinder.spiderfinder;

public class Spawner {
    public int x, y, z;
    public Direction direction;
    public int length;

    public Spawner(int x, int y, int z, Direction direction, int length) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.direction = direction;
        this.length = length;
    }

    @Override
    public String toString() {
        return "Spawner{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", direction=" + direction +
                ", length=" + length +
                '}'
                + String.format("/tp @p %d %d %d", x,y,z);
    }
}
