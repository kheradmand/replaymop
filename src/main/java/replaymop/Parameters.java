package replaymop;

import java.util.List;

import com.beust.jcommander.*;

public class Parameters {
	@Parameter(description = "Input file")
	public List<String> inputFile;
	
	@Parameter(names = "-debug-runtime", description = "Causes the replayed program to print debug info")
	public boolean debug_runtime = true;
	

}
