package replaymop.output.aspectj;


public class Aspect {
	public String name;
	public String body = "";
	
	
	
	public Aspect(String name) {
		this.name = name;
		this.body = ClassLoader.getSystemResourceAsStream("AspectJTemplate.aj").toString();
		setParameter("NAME", name);
	}
	
	public void setParameter(String parameter, String value){
		body.replace("%" + parameter + "%", value);
	}
	
	@Override
	public String toString() {
		return body;
	}
	
}
