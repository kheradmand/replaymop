package replaymop.output.aspectj;

public class Aspect {
	public String name;
	public String body = "";
	
	public Aspect(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return String.format("public aspect %s {\n %s \n}", name, body);
	}
	
}
