package replaymop.preprocessing.instrumentation;

public class Array {
	// object arrays and multi-dimensional arrays
	public static Object get(Object array, int index) {
		return ((Object[]) array)[index];
	}

	public static byte get(byte[] array, int index) {
		return array[index];
	}

	public static char get(char[] array, int index) {
		return array[index];
	}

	public static short get(short[] array, int index) {
		return array[index];
	}

	public static int get(int[] array, int index) {
		return array[index];
	}

	public static long get(long[] array, int index) {
		return array[index];
	}

	public static float get(float[] array, int index) {
		return array[index];
	}
	public static double get(double[] array, int index) {
		return array[index];
	}

	// object arrays and multi-dimensional arrays
	public static void set(Object array, int index, Object value) {
		((Object[]) array)[index] = value;
	}

	public static void set(byte[] array, int index, byte value) {
		array[index] = value;
	}

	public static void set(char[] array, int index, char value) {
		array[index] = value;
	}

	public static void set(short[] array, int index, short value) {
		array[index] = value;
	}

	public static void set(int[] array, int index, int value) {
		array[index] = value;
	}

	public static void set(long[] array, int index, long value) {
		array[index] = value;
	}

	public static void set(float[] array, int index, float value) {
		array[index] = value;
	}
	public static void set(double[] array, int index, double value) {
		array[index] = value;
	}

}
