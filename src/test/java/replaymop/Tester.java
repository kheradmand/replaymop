package replaymop;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

public class Tester {

	String path;
	File pathFile;

	Tester(String path) {
		this.path = path;
		pathFile = new File(path);
	}

	public void testSuccess(String prefix, boolean inspectStdErr,
			String... command) throws Exception {
		int ret = runCommand(prefix, command);
		Assert.assertEquals(ret, 0);
		if (inspectStdErr) {
			long id = Thread.currentThread().getId();
			File errorFile = new File(path + File.pathSeparator + prefix + id
					+ "actual.err");
			Assert.assertEquals(errorFile.length(), 0);
		}
	}

	public void testOutput(String prefix, boolean inspectSuccess,
			String... command) throws Exception {
		int ret = runCommand(prefix, command);
		if (inspectSuccess)
			Assert.assertEquals(ret, 0);
		
		long id = Thread.currentThread().getId();
		File actualOutputFile = new File(path + File.pathSeparator + prefix
				+ id + "actual.out");
		File actualErrorFile = new File(path + File.pathSeparator + prefix + id
				+ "actual.err");
		File expectedOutputFile = new File(path + File.pathSeparator + prefix
				 + "expected.out");
		File expectedErrorFile = new File(path + File.pathSeparator + prefix
				 + "expected.err");
		
		Assert.assertTrue(FileUtils.contentEquals(actualOutputFile,
				expectedOutputFile));
		Assert.assertTrue(FileUtils.contentEquals(actualErrorFile,
				expectedErrorFile));

	}
	
	public void testOutputConsistency(String prefix, int numOfRuns, final boolean inspectSuccess, final String... command) throws Exception{
		
		File srcInputFile = new File(path + File.pathSeparator + prefix + ".in");
		File targetInputFile = new File(path + File.pathSeparator + "consistency-test" + ".in");		
		FileUtils.copyFile(srcInputFile, targetInputFile);
		
		prefix = "consistency-test";
		
		runCommand(prefix, command);
		
		long id = Thread.currentThread().getId();
		File actualOutputFile = new File(path + File.pathSeparator + prefix
				+ id + "actual.out");
		File actualErrorFile = new File(path + File.pathSeparator + prefix + id
				+ "actual.err");
		File expectedOutputFile = new File(path + File.pathSeparator + prefix
				 + "expected.out");
		File expectedErrorFile = new File(path + File.pathSeparator + prefix
				 + "expected.err");
		
		FileUtils.moveFile(actualOutputFile, expectedOutputFile);
		FileUtils.moveFile(actualErrorFile, expectedErrorFile);
		
		ExecutorService pool = Executors.newFixedThreadPool(10);
		
		Future[] future = new Future[numOfRuns];
		
        for (int i = 0; i < numOfRuns; i++) {
            future[i] = pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                       testOutput("consistency-test", inspectSuccess, command);
                       return null;
                }
            });
        }
        pool.shutdown();
        for (int i = 0; i < numOfRuns; i++) 
        	future[i].get();
		
		
		
	}

	public int runCommand(String prefix, String... command) throws IOException,
			InterruptedException {
		long id = Thread.currentThread().getId();

		File inputFile = new File(path + File.pathSeparator + prefix + ".in");
		File outputFile = new File(path + File.pathSeparator + prefix + id
				+ "actual.out");
		File errorFile = new File(path + File.pathSeparator + prefix + id
				+ "actual.err");

		return runCommand(inputFile, outputFile, errorFile, command);
	}

	private int runCommand(File inputFile, File outputFile, File errorFile,
			String... command) throws IOException, InterruptedException {
		ProcessBuilder processsBuilder = new ProcessBuilder(command);
		processsBuilder.directory(pathFile);

		if (inputFile!= null && inputFile.exists() && inputFile.isFile())
			processsBuilder.redirectInput(inputFile);
		else
			processsBuilder.redirectInput(Redirect.INHERIT);

		processsBuilder.redirectOutput(outputFile);
		processsBuilder.redirectError(errorFile);

		Process process = processsBuilder.start();

		int ret = process.waitFor();

		return ret;
	}
	
	public int runCommandInternally(String... command) throws IOException, InterruptedException {
		return runCommand(null, Redirect.INHERIT.file(), Redirect.INHERIT.file());
	}

}
