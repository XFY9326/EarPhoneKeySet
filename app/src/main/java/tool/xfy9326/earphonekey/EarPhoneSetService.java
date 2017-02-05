package tool.xfy9326.earphonekey;

import android.accessibilityservice.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.view.accessibility.*;
import java.io.*;

import java.lang.Process;
import android.util.*;

public class EarPhoneSetService extends AccessibilityService
{
	private int scancode_up;
	private int scancode_down;
	private int currentVolume;
	private AudioManager mAudioManager;
	private boolean mIsHeadSetPlugged;
	private VolumeChangeReceiver mVolumeRecevier;
	private HeadSetChangeReceiver mHeadSetReceiver;
    private ScreenChangeReceiver mScreenReceiver;
	private Runtime runtime;
	private Process process;
	private DataOutputStream output;
	private SharedPreferences sp;
	private boolean LongPressed;
	private Handler LongPressHandle;
	private Thread LongPressThread;

	@Override
	public void onCreate()
	{
		runtime = Runtime.getRuntime();
		process = Methods.getRootProcess(runtime);
		output = Methods.getStream(process);
		LongPressed = false;
		mScreenReceiver = new ScreenChangeReceiver();
		mVolumeRecevier = new VolumeChangeReceiver();
		mHeadSetReceiver = new HeadSetChangeReceiver();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		registerScreenListener();
		registerEarPhoneUse();
		setLongPressHandle();
		super.onCreate();
	}

	@Override
	protected void onServiceConnected()
	{
		mIsHeadSetPlugged = Methods.isHeadSetUse(this);
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
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
		if (mIsHeadSetPlugged)
		{
			int keycode = event.getKeyCode();
			int scancode = event.getScanCode();
			int keyaction = event.getAction();
			boolean longpressmode = getLongPressGet();
			if (keycode == KeyEvent.KEYCODE_VOLUME_UP && scancode == scancode_up)
			{
				if (keyaction == KeyEvent.ACTION_DOWN)
				{
					if (longpressmode)
					{
						if (event.getRepeatCount() == 0)
						{
							LongPressed = true;
							setLongPressThread(1);
						}
					}
					else
					{
						if (getLongPressCustom())
						{
							if (event.getRepeatCount() == 0)
							{
								LongPressed = true;
								setLongPressThread(1);
							}
						}
						else
						{
							Methods.sendKeyCode(getKeyCodeUp(), process, output, getLongPressSend());
						}
					}
				}
				else if (keyaction == KeyEvent.ACTION_UP && LongPressed)
				{
					LongPressed = false;
					if (getLongPressCustom())
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS, process, output, getLongPressSend());
					}
					else
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_VOLUME_UP, process, output, getLongPressSend());
					}
				}
				return true;
			}
			else if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && scancode == scancode_down)
			{
				if (keyaction == KeyEvent.ACTION_DOWN)
				{
					if (longpressmode)
					{
						if (event.getRepeatCount() == 0)
						{
							LongPressed = true;
							setLongPressThread(2);
						}
					}
					else
					{
						if (getLongPressCustom())
						{
							if (event.getRepeatCount() == 0)
							{
								LongPressed = true;
								setLongPressThread(2);
							}
						}
						else
						{
							Methods.sendKeyCode(getKeyCodeDown(), process, output, getLongPressSend());
						}
					}
				}
				else if (keyaction == KeyEvent.ACTION_UP && LongPressed)
				{
					LongPressed = false;
					if (getLongPressCustom())
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT, process, output, getLongPressSend());
					}
					else
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN, process, output, getLongPressSend());
					}
				}
				return true;
			}
		}
		return super.onKeyEvent(event);
	}

	private int getKeyCodeUp()
	{
		return sp.getInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
	}

	private int getKeyCodeDown()
	{
		return sp.getInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT);
	}

	private boolean getLongPressSend()
	{
		return sp.getBoolean("LongPress_Send", false);
	}

	private boolean getLongPressGet()
	{
		return sp.getBoolean("LongPress_Get", false);
	}

	private boolean getLongPressCustom()
	{
		return sp.getBoolean("LongPress_Custom", false);
	}

	private void registerAlarmListener()
	{
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		IntentFilter fliter = new IntentFilter();
		fliter.addAction("android.media.VOLUME_CHANGED_ACTION");
		registerReceiver(mVolumeRecevier, fliter);
	} 

	private void unregisterAlarmListener()
	{
		unregisterReceiver(mVolumeRecevier);
	}

	private void registerScreenListener()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenReceiver, filter);
	}

	private void unregisterScreenListener() 
	{
		unregisterReceiver(mScreenReceiver);
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

	private void setLongPressThread(final int msg)
	{
		LongPressThread = new Thread(){
			public void run()
			{
				super.run();
				int i = 0;
				while (LongPressed)
				{
					try
					{
						sleep(100);
						i++;
						if (i == 8)
						{
							LongPressHandle.sendEmptyMessage(msg);
							LongPressed = false;
							break;
						}
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		};
		LongPressThread.start();
	}

	private void setLongPressHandle()
	{
		LongPressHandle = new Handler(){
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
					case 1:
						Methods.sendKeyCode(getKeyCodeUp(), process, output, getLongPressSend());
						break;
					case 2:
						Methods.sendKeyCode(getKeyCodeDown(), process, output, getLongPressSend());
						break;
				}
			}
		};
	}

	@Override
	public void onInterrupt()
	{
	}

	@Override
	public void onDestroy()
	{
		Methods.closeRuntime(process, output);
		unregisterScreenListener();
		unregisterEarPhoneUse();
		super.onDestroy();
	}

	private class HeadSetChangeReceiver extends BroadcastReceiver
	{
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
	}

	private class VolumeChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION"))
			{
				if (mIsHeadSetPlugged)
				{
					if (currentVolume < mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
					{
						Methods.sendKeyCode(getKeyCodeUp(), process, output, getLongPressSend());
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					}
					if (currentVolume > mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
					{
						Methods.sendKeyCode(getKeyCodeDown(), process, output, getLongPressSend());
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					}
				}
			}
		}
	}

	private class ScreenChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()))
			{
				unregisterAlarmListener();
			}
			else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))
			{
				registerAlarmListener();
			}
		}
    }

}
