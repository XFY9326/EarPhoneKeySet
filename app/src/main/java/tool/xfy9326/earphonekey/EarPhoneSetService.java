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
	private boolean mIsHeadSetPlugged;
	private VolumeChangeReceiver mVolumeRecevier;
	private HeadSetChangeReceiver mHeadSetReceiver;
    private ScreenChangeReceiver mScreenReceiver;
	private Runtime runtime;
	private Process process;
	private DataOutputStream output;

	@Override
	public void onCreate()
	{
		runtime = Runtime.getRuntime();
		process = Methods.getRootProcess(runtime);
		output = Methods.getStream(process);
		mScreenReceiver = new ScreenChangeReceiver();
		mVolumeRecevier = new VolumeChangeReceiver();
		mHeadSetReceiver = new HeadSetChangeReceiver();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		registerScreenListener();
		registerEarPhoneUse();
		super.onCreate();
	}

	@Override
	protected void onServiceConnected()
	{
		mIsHeadSetPlugged = Methods.isHeadSetUse(this);
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
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
					Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS, process, output);
				}
				return true;
			}
			if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && scancode == scancode_down)
			{
				if (keyaction == KeyEvent.ACTION_DOWN)
				{
					Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT, process, output);
				}
				return true;
			}
		}
		return super.onKeyEvent(event);
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
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS, process, output);
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					}
					if (currentVolume > mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
					{
						Methods.sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT, process, output);
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
