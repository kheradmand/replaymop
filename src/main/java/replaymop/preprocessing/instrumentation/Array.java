package replaymop.preprocessing.instrumentation;

public class Array {
	// object arrays and multi-dimensional arrays
	public static Object get(Object array, int index) {
		System.out.println("object get");
		return ((Object[]) array)[index];
	}

	public static byte getbyte(Object array, int index) {
		System.out.println("byte get");
		return ((byte[]) array)[index];
	}

	public static char getchar(Object array, int index) {
		System.out.println("char get");
		return ((char[]) array)[index];
	}

	public static short getshort(Object array, int index) {
		System.out.println("short get");
		return ((short[]) array)[index];
	}

	public static int getint(Object array, int index) {
		System.out.println("int get");
		return ((int[]) array)[index];
	}

	public static long getlong(Object array, int index) {
		System.out.println("long get");
		return ((long[]) array)[index];
	}

	public static float getfloat(Object array, int index) {
		System.out.println("float get");
		return ((float[]) array)[index];
	}
	public static double getdouble(Object array, int index) {
		System.out.println("double get");
		return ((double[]) array)[index];
	}

	// object arrays and multi-dimensional arrays
	public static void set(Object array, int index, Object value) {
		System.out.println("object set");
		((Object[]) array)[index] = value;
	}

	public static void set(Object array, int index, byte value) {
		System.out.println("byte set");
		((byte[]) array)[index] = value;
	}

	public static void set(Object array, int index, char value) {
		System.out.println("char set");
		((char[]) array)[index] = value;
	}

	public static void set(Object array, int index, short value) {
		System.out.println("short set");
		((short[]) array)[index] = value;
	}

	public static void set(Object array, int index, int value) {
		System.out.println("int set");
		((int[]) array)[index] = value;
	}

	public static void set(Object array, int index, long value) {
		System.out.println("long set");
		((long[]) array)[index] = value;
	}

	public static void set(Object array, int index, float value) {
		System.out.println("float set");
		((float[]) array)[index] = value;
	}
	public static void set(Object array, int index, double value) {
		System.out.println("double set");
		((double[]) array)[index] = value;
	}

}
