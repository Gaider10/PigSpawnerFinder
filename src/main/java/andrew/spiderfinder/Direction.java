package andrew.spiderfinder;

public enum Direction {
    NORTH(Axis.Z),
    SOUTH(Axis.Z),
    WEST(Axis.X),
    EAST(Axis.X);

    public Axis axis;

    Direction(Axis axis) {
        this.axis = axis;
    }

    public enum Axis {
        X,
        Z
    }
}