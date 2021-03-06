package replaymop.replayspecification;

public class ScheduleUnit {
	public Long thread;
	public int count;

	public ScheduleUnit() {
	}
	
	public ScheduleUnit(Long thread, int count) {
		this.thread = thread;
		this.count = count;
	}

	@Override
	public String toString() {
		return String.format("%d x %d", thread, count);
	}
}