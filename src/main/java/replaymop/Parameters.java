package replaymop;

import java.util.List;

import com.beust.jcommander.*;

public class Parameters {
	@Parameter(description = "Input file/folder")
	public List<String> inputFile;
	
	@Parameter(names = "-debug-runtime", description = "Causes the replayed program to print debug info")
	public boolean debug_runtime = true;
	
	@Parameter(names = "-rv-trace", description = "Generate replay specification from RV-Predict's trace")
	public boolean rv_trace = false;
	
	@Parameter(names = "-debug", description = "Generate debug information", arity = 1) //TODO:temp
	public boolean debug = true;
	
}
