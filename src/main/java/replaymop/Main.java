package replaymop;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
	public static Parameters parameters;
	
	private static void parserArguments(String[] args){
		parameters = new Parameters();
		JCommander parameterParser;
		try{
			parameterParser = new JCommander(parameters, args);
		}catch (ParameterException pe){
			System.out.println(pe.getMessage());
			return;
		}
		parameterParser.setProgramName("replaymop");
		if (parameters.inputFile == null){
			parameterParser.usage();
			System.exit(1);
		}
	}
	public static void main(String[] args){
		parserArguments(args);
		
		
	}
}
