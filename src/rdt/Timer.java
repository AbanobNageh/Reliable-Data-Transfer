package rdt;

public class Timer extends Thread {
	boolean timerFinished = false;
	short timerNumber = -1;
	int timerTime = 0;
	
	public Timer(int timerTime, short timerNumber) {
		this.timerTime = timerTime;
		this.timerNumber = timerNumber;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(this.timerTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.timerFinished = true;
	}

	public boolean isTimerFinished() {
		return timerFinished;
	}

	public short getTimerNumber() {
		return timerNumber;
	}
}
