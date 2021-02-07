package andrew.spiderfinder;

import java.util.Objects;

public class Vec3i {
	public int x, y, z;

	public Vec3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getDistSq(Vec3i pos){
		int dx = x - pos.x;
		int dy = y - pos.y;
		int dz = z - pos.z;
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vec3i vec3i = (Vec3i) o;
		return x == vec3i.x &&
				y == vec3i.y &&
				z == vec3i.z;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}
}
