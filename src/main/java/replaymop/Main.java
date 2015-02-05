package replaymop;

import java.io.File;

import replaymop.parser.RSParser;
import replaymop.parser.rs.ReplaySpecification;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
	public static Parameters parameters;
	
	private static void parseArguments(String[] args){
		parameters = new Parameters();
		JCommander parameterParser;
		try{
			parameterParser = new JCommander(parameters, args);
		}catch (ParameterException pe){
			System.err.println(pe.getMessage());
			return;
		}
		parameterParser.setProgramName("replaymop");
		if (parameters.inputFile == null){
			parameterParser.usage();
			System.exit(1);
		}
	}
	
	public static void main(String[] args){
		try{
			parseArguments(args);
			ReplaySpecification spec = RSParser.parse(new File(parameters.inputFile));
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
