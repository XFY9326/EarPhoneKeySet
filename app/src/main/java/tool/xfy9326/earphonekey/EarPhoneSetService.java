package tool.xfy9326.earphonekey;

import android.accessibilityservice.*;
import android.content.*;
import android.media.*;
import android.preference.*;
import android.view.*;
import android.view.accessibility.*;
import java.io.*;

public class EarPhoneSetService extends AccessibilityService
{
	private int scancode_up;
	private int scancode_down;
	private int currentVolume;
	private AudioManager mAudioManager;
	private int maxVolume;
	private boolean isDestroy;
	private Thread volumeChangeThread;
	private boolean mIsHeadSetPlugged;
	private BroadcastReceiver mHeadSetReceiver;
    private ScreenBroadcastReceiver mScreenReceiver;

	@Override
	public void onCreate()
	{
		mScreenReceiver = new ScreenBroadcastReceiver();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		registerListener();
		setEarPhoneListener();
		registerEarPhoneUse();
		super.onCreate();
	}

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
		if (mIsHeadSetPlugged)
		{
			if (keycode == KeyEvent.KEYCODE_VOLUME_UP && scancode == scancode_up)
			{
				if (keyaction == KeyEvent.ACTION_DOWN)
				{
					sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
				}
				return true;
			}
			if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && scancode == scancode_down)
			{
				if (keyaction == KeyEvent.ACTION_DOWN)
				{
					sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT);
				}
				return true;
			}
		}
		return super.onKeyEvent(event);
	}

	private boolean isHeadSetUse()
	{
		AudioManager localAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		return localAudioManager.isWiredHeadsetOn();
	}

	private void setAlarmListener()
	{
		isDestroy = false;
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volumeChangeThread = new Thread()
		{
			public void run()
			{
				while (!isDestroy)
				{
					try
					{
						Thread.sleep(20);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					if (mIsHeadSetPlugged)
					{
						if (currentVolume < mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
						{
							sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
							mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
						}
						if (currentVolume > mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
						{
							sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT);
							mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
						}
					}
				}
			};
		};
		volumeChangeThread.start();
	} 

	private void closeAlarmListener()
	{
		isDestroy = true;
	}

	private void registerListener()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenReceiver, filter);
	}

	private void unregisterListener() 
	{
		unregisterReceiver(mScreenReceiver);
	}

	private void setEarPhoneListener()
	{
		mIsHeadSetPlugged = isHeadSetUse();
		mHeadSetReceiver =  new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent)
			{
				if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
				{
					int state = intent.getIntExtra("state", -1);
					switch (state)
					{
						case 0:
							mIsHeadSetPlugged = false;
							break;
						case 1:
							mIsHeadSetPlugged = true;
							break;
					}
				}
			}
		};
	}

	private void registerEarPhoneUse()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(mHeadSetReceiver, filter);
	}

	private void unregisterEarPhoneUse()
	{
		unregisterReceiver(mHeadSetReceiver);
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

	@Override
	public void onDestroy()
	{
		isDestroy = true;
		unregisterListener();
		unregisterEarPhoneUse();
		super.onDestroy();
	}

	private class ScreenBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_ON.equals(action))
			{
				closeAlarmListener();
			}
			else if (Intent.ACTION_SCREEN_OFF.equals(action))
			{
				setAlarmListener();
			}
		}
    }

}
