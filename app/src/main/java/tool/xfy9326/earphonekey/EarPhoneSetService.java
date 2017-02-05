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

public class EarPhoneSetService extends AccessibilityService
{
	private int scancode_up;
	private int scancode_down;
	private int currentVolume;
	private AudioManager mAudioManager;
	private Receiver mReceiver;
	private Runtime runtime;
	private Process process;
	private DataOutputStream output;
	private SharedPreferences sp;
	private boolean LongPressed;
	private Handler LongPressHandle;
	private Thread LongPressThread;
	private boolean ScreenOn;

	@Override
	public void onCreate()
	{
		LongPressed = false;
		ScreenOn = true;
		mReceiver = new Receiver();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		setLongPressHandle();
		super.onCreate();
	}

	@Override
	protected void onServiceConnected()
	{
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		registerListener();
		runtime = Runtime.getRuntime();
		process = Methods.getRootProcess(runtime);
		output = Methods.getStream(process);
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
		if (Methods.isHeadSetUse(this))
		{
			int keycode = event.getKeyCode();
			int scancode = event.getScanCode();
			int keyaction = event.getAction();
			boolean longpressmode = Methods.getLongPressGet(sp);
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
						if (Methods.getLongPressCustom(sp))
						{
							if (event.getRepeatCount() == 0)
							{
								LongPressed = true;
								setLongPressThread(1);
							}
						}
						else
						{
							Methods.sendKeyCode(Methods.getKeyCodeUp(sp), process, output, Methods.getLongPressSend(sp));
						}
					}
				}
				else if (keyaction == KeyEvent.ACTION_UP && LongPressed)
				{
					LongPressed = false;
					if (Methods.getLongPressCustom(sp))
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS, process, output, Methods.getLongPressSend(sp));
					}
					else
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_VOLUME_UP, process, output, Methods.getLongPressSend(sp));
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
						if (Methods.getLongPressCustom(sp))
						{
							if (event.getRepeatCount() == 0)
							{
								LongPressed = true;
								setLongPressThread(2);
							}
						}
						else
						{
							Methods.sendKeyCode(Methods.getKeyCodeDown(sp), process, output, Methods.getLongPressSend(sp));
						}
					}
				}
				else if (keyaction == KeyEvent.ACTION_UP && LongPressed)
				{
					LongPressed = false;
					if (Methods.getLongPressCustom(sp))
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT, process, output, Methods.getLongPressSend(sp));
					}
					else
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN, process, output, Methods.getLongPressSend(sp));
					}
				}
				return true;
			}
		}
		return super.onKeyEvent(event);
	}

	private void registerListener()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction("android.media.VOLUME_CHANGED_ACTION");
		registerReceiver(mReceiver, filter);
	}

	private void unregisterListener() 
	{
		unregisterReceiver(mReceiver);
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
						Methods.sendKeyCode(Methods.getKeyCodeUp(sp), process, output, Methods.getLongPressSend(sp));
						break;
					case 2:
						Methods.sendKeyCode(Methods.getKeyCodeDown(sp), process, output, Methods.getLongPressSend(sp));
						break;
				}
			}
		};
	}

	@Override
	public void onInterrupt()
	{
		unregisterListener();
		Methods.closeRuntime(process, output);
	}

	private class Receiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_ON.equals(action))
			{
				ScreenOn = true;
			}
			else if (Intent.ACTION_SCREEN_OFF.equals(action))
			{
				ScreenOn = false;
				currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			}
			else if (!ScreenOn && action.equals("android.media.VOLUME_CHANGED_ACTION"))
			{
				if (Methods.isHeadSetUse(context))
				{
					if (currentVolume < mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
					{
						Methods.sendKeyCode(Methods.getKeyCodeUp(sp), process, output, Methods.getLongPressSend(sp));
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					}
					if (currentVolume > mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
					{
						Methods.sendKeyCode(Methods.getKeyCodeDown(sp), process, output, Methods.getLongPressSend(sp));
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					}
				}
			}
		}
    }

}
