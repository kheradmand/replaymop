package replaymop.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class AJAgentGenerator {
	private static final String tempDirName = "tempXf12";



	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.err.println("usage: AJAgentGenerator <aspect>");
			System.exit(1);
		}
		File ajFile = new File(args[0]);
		if (!ajFile.exists()) {
			System.err.println("file does not exist");
			System.exit(1);
		}
		generateAgent(ajFile);
	
	}
	
	
	
	public static void generateAgent(File ajFile) throws Exception{
		
		File root = ajFile.getParentFile();
		File tempDir = new File(root, tempDirName);
		File tempJar = new File(root, "temp.jar");
		File xmlFile = new File(tempDir, "META-INF" + File.separator + "aop-ajc.xml");
		String[] ajcArgs = new String[] { "-outjar", tempJar.getAbsolutePath(),
				"-outxml", "-Xjoinpoints:synchronization",
				ajFile.getAbsolutePath() };
		int errors = org.aspectj.tools.ajc.Main.bareMain(ajcArgs, false, null, null, null, null);
		if (errors > 0)
			throw new Exception("ajc compiler returned " + errors + " errors");
		FileUtils.forceMkdir(tempDir);
		(new ProcessBuilder("jar", "xf", "../temp.jar")).directory(tempDir).start().waitFor();
		
		String xmlFileContent = FileUtils.readFileToString(xmlFile);
		xmlFileContent = xmlFileContent.replaceFirst("</aspects>\\s+</aspectj>", "</aspects>\n<weaver options=\"-Xjoinpoints:synchronization\"/>\n</aspectj>");
		FileUtils.writeStringToFile(xmlFile, xmlFileContent);
		
		(new ProcessBuilder("jar", "cvf", "agent.jar", "-C", tempDirName, ".")).directory(root).start().waitFor();
		FileUtils.forceDelete(tempJar);
		FileUtils.forceDelete(tempDir);
	}
	
}
