package rdt;
/**
 * this class represents a timer, it is used to send a packet again
 * if the timer runs out.
 */
public class Timer extends Thread {
	boolean timerFinished = false;
	short timerNumber = -1;
	int timerTime = 0;
	
	/**
	 * creates a new timer.
	 * @param timerTime the time of the timer.
	 * @param timerNumber the ID of the timer.
	 */
	public Timer(int timerTime, short timerNumber) {
		this.timerTime = timerTime;
		this.timerNumber = timerNumber;
	}
	
	/**
	 * starts the timer, once the timer is finished it marks the timer as done. 
	 */
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
