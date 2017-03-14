package pt.promatik.moss.utils;

import java.util.Timer;
import java.util.TimerTask;

public class Delegate
{
	private Timer timer = null;
	
	public Delegate(Runnable runnable, int delay)
	{
		this(runnable, delay, 0);
	}
	
	public Delegate(Runnable runnable, int delay, int period)
	{
		timer = null;
		try {
			timer = new Timer();
			TimerTask t = new TimerTask() {
				@Override
				public void run() {
					try {
						runnable.run();
					} catch (Exception e) {
						Utils.log("Delegate run exception", e);
					}
				}
			};
			if(period > 0) {
				timer.schedule(t, delay, period);
			} else {
				timer.schedule(t, delay);
			}
		} catch (Exception e) {
			Utils.log("Delegate timer run exception", e);
		}
	}
	
	public void cancel()
	{
		timer.cancel();
	}
	
	public static Delegate run(Runnable runnable, int delay)
	{
		return new Delegate(runnable, delay);
	}
	
	public static Delegate run(Runnable runnable, int delay, int period)
	{
		return new Delegate(runnable, delay, period);
	}
}
