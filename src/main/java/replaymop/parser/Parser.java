package replaymop.parser;

import java.io.File;

import replaymop.ReplayMOPException;
import replaymop.replayspecification.ReplaySpecification;

public abstract class Parser {
	protected ReplaySpecification spec;
	protected boolean alreadyParsed = false;
	protected abstract void startParsing() throws ReplayMOPException;
	
	public ReplaySpecification parse() {
		try {
			if (!alreadyParsed)
				startParsing();
		} catch (ReplayMOPException re) {
			re.printStackTrace();
			return null;
		}
		return spec;
	}
}
