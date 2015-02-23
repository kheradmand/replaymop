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
public class BasicTest {
	private static String basePath = System.getProperty("user.dir")
			+ File.separator + "examples" + File.separator + "basic";

	Tester tester;

	String folder;
	String entryPoint;
	String input;
	String workindDir;

	public BasicTest(String folder, String entryPoint, String input) {
		this.folder = folder;
		this.entryPoint = entryPoint;
		this.input = input;
		this.workindDir = basePath + File.separator + folder;
		tester = new Tester(workindDir);
	}

	@Test
	public void test() throws Exception {
		System.out.println("\ntesting " + entryPoint);
		
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
				"bin",  entryPoint, input);
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
		data.add(new Object[] { "Example0", "Example0", "" });
		data.add(new Object[] { "Example1", "Example1", "" });
		return data;
	};
}
