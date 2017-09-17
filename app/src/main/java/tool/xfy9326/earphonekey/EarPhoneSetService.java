package tool.xfy9326.earphonekey;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.io.DataOutputStream;

public class EarPhoneSetService extends AccessibilityService {
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
    private boolean ScreenOn;

    @Override
    public void onCreate() {
        LongPressed = false;
        ScreenOn = true;
        mReceiver = new Receiver();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        LongPressHandle = new MsgHandler();
        super.onCreate();
    }

    @Override
    protected void onServiceConnected() {
        registerListener();
        runtime = Runtime.getRuntime();
        if (sp.getBoolean("AdvancedFunctionOn", false)) {
            process = Methods.getRootProcess(runtime);
        } else {
            process = null;
        }
        output = Methods.getStream(process);
        scancode_up = sp.getInt("KeyCode_UP", 0);
        scancode_down = sp.getInt("KeyCode_DOWN", 0);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent p1) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getBooleanExtra("ProcessChange", false)) {
                if (sp.getBoolean("AdvancedFunctionOn", false)) {
                    runtime = Runtime.getRuntime();
                    process = Methods.getRootProcess(runtime);
                } else {
                    process = null;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (Methods.isHeadSetUse(this) && CheckCalling()) {
            if (scancode_up != 0 && scancode_down != 0) {
                int keycode = event.getKeyCode();
                int scancode = event.getScanCode();
                int keyaction = event.getAction();
                boolean longpressmode = Methods.getLongPressGet(sp);
                if (keycode == KeyEvent.KEYCODE_VOLUME_UP && scancode == scancode_up) {
                    if (keyaction == KeyEvent.ACTION_DOWN) {
                        KeyUpAction(event, longpressmode, 1);
                    } else if (keyaction == KeyEvent.ACTION_UP && LongPressed) {
                        LongPressed = false;
                        if (Methods.getLongPressCustom(sp)) {
                            Methods.sendKeyCode(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS, process, output, Methods.getLongPressSend(sp));
                        } else {
                            Methods.sendKeyCode(this, KeyEvent.KEYCODE_VOLUME_UP, process, output, Methods.getLongPressSend(sp));
                        }
                    }
                    return true;
                } else if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && scancode == scancode_down) {
                    if (keyaction == KeyEvent.ACTION_DOWN) {
                        KeyUpAction(event, longpressmode, 2);
                    } else if (keyaction == KeyEvent.ACTION_UP && LongPressed) {
                        LongPressed = false;
                        if (Methods.getLongPressCustom(sp)) {
                            Methods.sendKeyCode(this, KeyEvent.KEYCODE_MEDIA_NEXT, process, output, Methods.getLongPressSend(sp));
                        } else {
                            Methods.sendKeyCode(this, KeyEvent.KEYCODE_VOLUME_DOWN, process, output, Methods.getLongPressSend(sp));
                        }
                    }
                    return true;

                }
            } else {
                Toast.makeText(this, R.string.correct_device, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    private void KeyUpAction(KeyEvent event, boolean longpressmode, int type) {
        if (longpressmode) {
            if (event.getRepeatCount() == 0) {
                LongPressed = true;
                setLongPressThread(type);
            }
        } else {
            if (Methods.getLongPressCustom(sp)) {
                if (event.getRepeatCount() == 0) {
                    LongPressed = true;
                    setLongPressThread(type);
                }
            } else {
                int code;
                if (type == 2) {
                    code = Methods.getKeyCodeDown(sp);
                } else if (type == 1) {
                    code = Methods.getKeyCodeUp(sp);
                } else {
                    code = 0;
                }
                Methods.sendKeyCode(this, code, process, output, Methods.getLongPressSend(sp));
            }
        }
    }

    private void registerListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(mReceiver, filter);
    }

    private void unregisterListener() {
        unregisterReceiver(mReceiver);
    }

    private void setLongPressThread(final int msg) {
        Thread longPressThread = new Thread() {
            public void run() {
                super.run();
                int i = 0;
                while (LongPressed) {
                    try {
                        sleep(100);
                        i++;
                        if (i == 8) {
                            LongPressHandle.sendEmptyMessage(msg);
                            LongPressed = false;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        longPressThread.start();
    }

    private boolean CheckCalling() {
        TelephonyManager telephonyService = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        return telephonyService == null || telephonyService.getCallState() == TelephonyManager.CALL_STATE_IDLE;
    }

    @Override
    public void onInterrupt() {
        unregisterListener();
        Methods.closeRuntime(process, output);
    }

    @SuppressLint("HandlerLeak")
    private class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Methods.sendKeyCode(EarPhoneSetService.this, Methods.getKeyCodeUp(sp), process, output, Methods.getLongPressSend(sp));
                    break;
                case 2:
                    Methods.sendKeyCode(EarPhoneSetService.this, Methods.getKeyCodeDown(sp), process, output, Methods.getLongPressSend(sp));
                    break;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                ScreenOn = true;
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                ScreenOn = false;
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            } else if (!ScreenOn && action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                if (Methods.isHeadSetUse(context) && CheckCalling()) {
                    if (currentVolume < mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                        Methods.sendKeyCode(EarPhoneSetService.this, Methods.getKeyCodeUp(sp), process, output, Methods.getLongPressSend(sp));
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    }
                    if (currentVolume > mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                        Methods.sendKeyCode(EarPhoneSetService.this, Methods.getKeyCodeDown(sp), process, output, Methods.getLongPressSend(sp));
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    }
                }
            }
        }
    }

}
