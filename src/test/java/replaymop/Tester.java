package replaymop;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;

public class Tester {

	String path;
	File pathFile;

	Tester(String path) {
		this.path = path;
		pathFile = new File(path);
	}

	// public void testSuccess(String prefix, boolean inspectStdErr,
	// String... command) throws Exception {
	// int ret = runCommand(prefix, command);
	// Assert.assertEquals(ret, 0);
	// if (inspectStdErr) {
	// long id = Thread.currentThread().getId();
	// File errorFile = new File(path + File.separator + prefix + id
	// + ".actual.err");
	// Assert.assertEquals(errorFile.length(), 0);
	// }
	// }

	public void testOutputConsistency(String prefix, int numOfRuns,
			final boolean inspectSuccess, final String... command)
			throws Exception {

		File srcInputFile = getInputFile(prefix);
		File targetInputFile = getInputFile("consistency-test");
		if (srcInputFile != null && srcInputFile.exists()
				&& srcInputFile.isFile())
			FileUtils.copyFile(srcInputFile, targetInputFile);

		prefix = "consistency-test";

		runCommand(prefix, command);

		File actualOutputFile = getActualOutputFile(prefix);
		File actualErrorFile = getActualErrorFile(prefix);
		File expectedOutputFile = getExpectedOutputFile(prefix);
		File expectedErrorFile = getExpectedErrorFile(prefix);

		expectedOutputFile.delete();
		expectedErrorFile.delete();

		FileUtils.moveFile(actualOutputFile, expectedOutputFile);
		FileUtils.moveFile(actualErrorFile, expectedErrorFile);

		ExecutorService pool = Executors.newFixedThreadPool(10);

		Future[] future = new Future[numOfRuns];

		for (int i = 0; i < numOfRuns; i++) {
			future[i] = pool.submit(new TestOuputTask("consistency-test", i,
					inspectSuccess, command));
		}
		pool.shutdown();
		for (int i = 0; i < numOfRuns; i++)
			future[i].get();

		System.out.print("\n");

	}

	class TestOuputTask implements Callable<Void> {

		String prefix;
		int uniqueId;
		boolean inspectSuccess;
		String[] command;

		public TestOuputTask(String prefix, int uniqueId,
				boolean inspectSuccess, String[] command) {
			super();
			this.prefix = prefix;
			this.uniqueId = uniqueId;
			this.inspectSuccess = inspectSuccess;
			this.command = command;
		}

		@Override
		public Void call() throws Exception {
			System.out.print(".");
			testOutput(String.format("consistency-test.%d", uniqueId),
					inspectSuccess, command);
			return null;
		}

	}

	private File getExpectedErrorFile(String prefix) {
		return new File(path + File.separator + getRawPrefix(prefix)
				+ ".expected.err");
	}

	private File getExpectedOutputFile(String prefix) {
		return new File(path + File.separator + getRawPrefix(prefix)
				+ ".expected.out");
	}

	public void testOutput(String prefix, boolean inspectSuccess,
			String... command) throws Exception {
		int ret = runCommand(prefix, command);
		if (inspectSuccess)
			Assert.assertEquals(ret, 0);

		File actualOutputFile = getActualOutputFile(prefix);
		File actualErrorFile = getActualErrorFile(prefix);
		File expectedOutputFile = getExpectedOutputFile(prefix);
		File expectedErrorFile = getExpectedErrorFile(prefix);

		Assert.assertTrue(FileUtils.contentEquals(actualOutputFile,
				expectedOutputFile));
		Assert.assertTrue(FileUtils.contentEquals(actualErrorFile,
				expectedErrorFile));

		actualOutputFile.delete();
		actualErrorFile.delete();

	}

	public int runCommand(String prefix, String... command) throws IOException,
			InterruptedException {

		File inputFile = getInputFile(prefix);
		File outputFile = getActualOutputFile(prefix);
		File errorFile = getActualErrorFile(prefix);

		return runCommand(inputFile, outputFile, errorFile, command);
	}

	private File getActualErrorFile(String prefix) {
		return new File(path + File.separator + prefix + ".actual.err");
	}

	private File getActualOutputFile(String prefix) {
		return new File(path + File.separator + prefix + ".actual.out");
	}

	private File getInputFile(String prefix) {
		return new File(path + File.separator + getRawPrefix(prefix) + ".in");
	}

	private String getRawPrefix(String uniquePrefix) {
		int dotIndex = uniquePrefix.lastIndexOf('.');
		if (dotIndex == -1)
			dotIndex = uniquePrefix.length();
		return uniquePrefix.substring(0, dotIndex);
	}

	private int runCommand(File inputFile, File outputFile, File errorFile,
			String... command) throws IOException, InterruptedException {
		ProcessBuilder processsBuilder = new ProcessBuilder(command);
		processsBuilder.directory(pathFile);
		processsBuilder.inheritIO();

		if (inputFile != null && inputFile.exists() && inputFile.isFile())
			processsBuilder.redirectInput(inputFile);

		if (outputFile != null)
			processsBuilder.redirectOutput(outputFile);

		if (errorFile != null)
			processsBuilder.redirectError(errorFile);

		Process process = processsBuilder.start();

		if (process.waitFor(10, TimeUnit.SECONDS))
			return process.waitFor();
		else {
			process.destroyForcibly();
			throw new InterruptedException(
					"Time limit expired, probabely due to deadlock");
		}

	}

	public int runCommandInternally(String... command) throws IOException,
			InterruptedException {
		return runCommand(null, null, null, command);
	}

	public int runCommandInternallyWithInput(String prefix, String... command)
			throws IOException, InterruptedException {
		return runCommand(getInputFile(prefix), null, null, command);
	}

}
