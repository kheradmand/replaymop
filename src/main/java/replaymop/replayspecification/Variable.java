package replaymop.replayspecification;

public class Variable {
	public String type;
	public String name;

	public Variable() {
	}
	
	@Override
	public String toString() {
		return ((type == null || type == "") ? "*" : type) + " " + name;
	}
}