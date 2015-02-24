package replaymop.replayspecification;

public class ScheduleUnit {
	public Long thread;
	public int count;

	public ScheduleUnit() {
	}
	
	@Override
	public String toString() {
		return String.format("%d x %d", thread, count);
	}
}