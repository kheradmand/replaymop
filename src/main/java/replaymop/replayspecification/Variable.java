package replaymop.replayspecification;

public class Variable {


	public String type;
	public String name;

	public Variable() {
	}
	
	public Variable(String type, String name) {
		this.type = type;
		this.name = name;
	}

	
	@Override
	public String toString() {
		return ((type == null || type == "") ? "*" : type) + " " + name;
	}
}