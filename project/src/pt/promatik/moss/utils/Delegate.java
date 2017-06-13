package pt.promatik.moss.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Delegate
{
	private Timer timer = null;
	private long startTime, cancelTime = 0;
	
	public Delegate(Runnable runnable, int delay)
	{
		this(runnable, delay, 0);
	}
	
	public Delegate(Runnable runnable, Date delay)
	{
		this(runnable, delay, 0);
	}
	
	public Delegate(Runnable runnable, Calendar delay)
	{
		this(runnable, delay.getTime(), 0);
	}
	
	
	public Delegate(Runnable runnable, int delay, int period)
	{
		_delegate(runnable, delay, null, period);
	}
	
	public Delegate(Runnable runnable, Date delay, int period)
	{
		_delegate(runnable, 0, delay, period);
	}
	
	public Delegate(Runnable runnable, Calendar delay, int period)
	{
		_delegate(runnable, 0, delay.getTime(), period);
	}
	
	
	private void _delegate(Runnable runnable, int delayInt, Date delayDate, int period)
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
			
			// Save start time
			startTime = System.nanoTime();
			
			if(period > 0) {
				if(delayDate != null)
					timer.schedule(t, delayDate, period);
				else
					timer.schedule(t, delayInt, period);
			} else {
				if(delayDate != null)
					timer.schedule(t, delayDate);
				else
					timer.schedule(t, delayInt);
			}
		} catch (Exception e) {
			Utils.log("Delegate timer run exception", e);
		}
	}
	
	public void cancel()
	{
		timer.cancel();
		cancelTime = System.nanoTime();
	}
	
	public int elapsedTime() {
		return Math.round(((cancelTime == 0 ? System.nanoTime() : cancelTime) - startTime) / 1000000f);
	}
	
	public static Delegate run(Runnable runnable, int delay)
	{
		return new Delegate(runnable, delay);
	}
	
	public static Delegate run(Runnable runnable, int delay, int period)
	{
		return new Delegate(runnable, delay, period);
	}

	public static Delegate run(Runnable runnable, Date time)
	{
		return new Delegate(runnable, time);
	}

	public static Delegate run(Runnable runnable, Date time, int period)
	{
		return new Delegate(runnable, time, period);
	}

	public static Delegate run(Runnable runnable, Calendar time)
	{
		return new Delegate(runnable, time.getTime());
	}

	public static Delegate run(Runnable runnable, Calendar time, int period)
	{
		return new Delegate(runnable, time.getTime(), period);
	}
}
