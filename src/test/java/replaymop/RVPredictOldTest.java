package replaymop;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RVPredictOldTest {
	private static String basePath = System.getProperty("user.dir")
			+ File.separator + "examples" + File.separator + "rv-predict-old";

	Tester tester;

	String folder;
	String entryPoint;
	String input;
	String workindDir;

	public RVPredictOldTest(String folder, String entryPoint, String input) {
		this.folder = folder;
		this.entryPoint = entryPoint;
		this.input = input;
		this.workindDir = basePath + File.separator + folder;
		tester = new Tester(workindDir);
	}

	@Test
	public void test() throws Exception {
		System.out.println("\ntesting " + folder + "." + entryPoint);
		
		//create bin
		File bin = new File(workindDir + File.separator + "bin");
		FileUtils.forceMkdir(bin);
		
		//compile *.java to bin
		List<String> compile = new ArrayList<>();
		compile.add("javac");
		compile.add("-d");
		compile.add("bin");
		FileUtils.listFiles(new File(workindDir), new String[] { "java" },
				false).forEach(f -> compile.add(f.getName()));
		tester.runCommandInternally(compile.toArray(new String[compile.size()]));
		
		//test command
		//TODO: implement better input (maybe)
		tester.testOutputConsistency(entryPoint, 100, true, "java", "-cp",
				"bin", folder + "." + entryPoint, input);
		//tester.runCommandInternally("java", "-cp", "bin", folder + "." + entryPoint, input);
		
		//remove bin + .out + .err 
		FileUtils.forceDelete(bin);
		FileUtils.listFiles(new File(workindDir),
				new String[] { "out", "err" }, false).forEach(
				f -> f.delete());

	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		ArrayList<Object[]> data = new ArrayList<Object[]>();
		data.add(new Object[] { "account", "Main", "/dev/null" });
		data.add(new Object[] { "cdep", "Main", "" });
		data.add(new Object[] { "constructor", "Simple", "" });
		data.add(new Object[] { "elevator", "Elevator", "" });
		data.add(new Object[] { "emptyfor", "Main", "" });
		data.add(new Object[] { "ex", "Foo", "" });
		data.add(new Object[] { "file", "File", "" });
		data.add(new Object[] { "finalvar", "Main", "" });
		data.add(new Object[] { "finalvar2", "Main", "" });
		data.add(new Object[] { "finalvar3", "Main", "" });
		data.add(new Object[] { "finalvar4", "Main", "" });
		data.add(new Object[] { "huge", "Main", "NumberOfEvents" });
		data.add(new Object[] { "huge", "Main", "NumberOfEvents2" });
		data.add(new Object[] { "huge", "Loop", "" });
		data.add(new Object[] { "impure", "Simple", "" });
		data.add(new Object[] { "innerclass", "Simple", "" });
		data.add(new Object[] { "joinsimple", "Simple", "" });
		data.add(new Object[] { "mixedlockshuge", "Main", "" });
		data.add(new Object[] { "pseudosafecdep", "Main", "" });
		data.add(new Object[] { "safesimple", "Simple", "" });
		data.add(new Object[] { "safewait", "Simple", "" });
		data.add(new Object[] { "simple", "Simple", "" });
		data.add(new Object[] { "singleton", "Main", "" });
		data.add(new Object[] { "subtle", "MyThread", "" });
		data.add(new Object[] { "tsp", "Tsp", basePath + File.separator + "tsp" + File.separator + "tspfiles" + File.separator + "map4", 2 });
		data.add(new Object[] { "unsafejoin", "Simple", "" });
		data.add(new Object[] { "wait", "Simple", "" });
		return data;
	};
}
