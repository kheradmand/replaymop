package replaymop;

import java.io.File;
import java.io.PrintWriter;

import replaymop.output.aspectj.Aspect;
import replaymop.output.aspectj.AspectJProcessor;
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
			File inputFile = new File(parameters.inputFile);
			ReplaySpecification spec = RSParser.parse(inputFile);
			Aspect aspect = AspectJProcessor.process(spec);
			File outputFile = new File(inputFile.getParent() + File.separator + aspect.name + ".aj");
			PrintWriter writer = new PrintWriter(outputFile);
			writer.println(aspect.toString());
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
