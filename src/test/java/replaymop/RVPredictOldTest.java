package replaymop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RVPredictOldTest {
	private static String basePath = System.getProperty("REPLAYMOP_HOME") + File.separator + "examples" + File.separator + "rv-predicd-old";
	
	Tester tester;
	
	String folder;
	String entryPoint;
	String input;
	
	RVPredictOldTest(String folder, String entryPoint, String input){
		this.folder = folder;
		this.entryPoint = entryPoint;
		this.input = input;
		tester = new Tester(basePath + File.separator + folder);
	}
	
	@Test
	public void test() throws Exception{
		tester.runCommandInternally("mkdir bin");
		tester.runCommandInternally("mkdir javac *.java -d bin");
		tester.testOutputConsistency(entryPoint, 100, true, "java", "-cp bin", folder+"."+entryPoint, input);
		tester.runCommandInternally("rm -rf bin");
		tester.runCommandInternally("rm *.expected.out");
		tester.runCommandInternally("rm *.expected.err");
	}
	
	
	
	
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        
        return data;
	};
}
