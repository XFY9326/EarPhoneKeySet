package tool.xfy9326.earphonekey;

import android.accessibilityservice.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import java.io.*;
import android.content.*;
import android.preference.*;

public class EarPhoneSetService extends AccessibilityService
{
	private int scancode_up;
	private int scancode_down;

	@Override
	protected void onServiceConnected()
	{
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		scancode_up = sp.getInt("KeyCode_UP", 0);
		scancode_down = sp.getInt("KeyCode_DOWN", 0);
		super.onServiceConnected();
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent p1)
	{
	}

	@Override
	protected boolean onKeyEvent(KeyEvent event)
	{
		int keycode = event.getKeyCode();
		int scancode = event.getScanCode();
		int keyaction = event.getAction();
		if (keycode == KeyEvent.KEYCODE_VOLUME_UP && scancode == scancode_up)
		{
			if (keyaction == KeyEvent.ACTION_UP)
			{
				sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
			}
			return true;
		}
		if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && scancode == scancode_down)
		{
			if (keyaction == KeyEvent.ACTION_UP)
			{
				sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT);
			}
			return true;
		}
		return super.onKeyEvent(event);
	}

	private void sendKeyCode(final int keyCode)
	{
		Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					Runtime r = null;
					Process p = null;
					DataOutputStream o = null;
					String keyCommand = "input keyevent " + keyCode;
					try
					{
						r = Runtime.getRuntime();
						p = r.exec("su");
						o = new DataOutputStream(p.getOutputStream());
						o.writeBytes(keyCommand + "\n");
						o.writeBytes("exit\n");
						o.flush();
						p.waitFor();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						try
						{
							if (o != null)
							{
								o.close();
							}
							p.destroy();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			});
		t.start();
	}

	@Override
	public void onInterrupt()
	{
	}

}
