package replaymop.output.aspectj;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class Aspect {
	public String name;
	public String body = "";

	public Aspect(String name) {
		this.name = name;

		try {
			InputStream temp = ClassLoader
					.getSystemResourceAsStream("AspectJTemplate.aj");
			this.body = IOUtils.toString(temp);
		} catch (IOException e) {
			e.printStackTrace();
		}

		setParameter("NAME", name);
	}

	public void setParameter(String parameter, String value) {
		body = body.replace("%" + parameter + "%", value);
	}

	@Override
	public String toString() {
		return body;
	}

}
