package tool.xfy9326.earphonekey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class Methods {
    static void showAdvancedFunction(final Activity ctx) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View layout = inflater.inflate(R.layout.dialog_advance_keyset_layout, (ViewGroup) ctx.findViewById(R.id.layout_custom_keycode));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor sped = sp.edit();
        final EditText up = (EditText) layout.findViewById(R.id.edittext_up_key);
        final EditText down = (EditText) layout.findViewById(R.id.edittext_down_key);
        final CheckBox send = (CheckBox) layout.findViewById(R.id.checkbox_longpress_send);
        final CheckBox custom = (CheckBox) layout.findViewById(R.id.checkbox_longpress_custom);
        custom.setChecked(sp.getBoolean("LongPress_Custom", false));
        send.setChecked(sp.getBoolean("LongPress_Send", false));
        up.setText(String.valueOf(sp.getInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS)));
        down.setText(String.valueOf(sp.getInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT)));
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle(R.string.advanced_title);
        dialog.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
                if (up.getText().toString().trim().equals("") || down.getText().toString().trim().equals("")) {
                    Toast.makeText(ctx, R.string.advanced_wrong, Toast.LENGTH_SHORT).show();
                } else {
                    sped.putBoolean("LongPress_Custom", custom.isChecked());
                    sped.putBoolean("LongPress_Send", send.isChecked());
                    sped.putInt("CustomCode_UP", Integer.parseInt(up.getText().toString()));
                    sped.putInt("CustomCode_DOWN", Integer.parseInt(down.getText().toString()));
                    sped.apply();
                }
            }
        });
        dialog.setNegativeButton(R.string.reset, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
                sped.putInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                sped.putInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT);
                sped.commit();
            }
        });
        dialog.setNeutralButton(R.string.advanced_list, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) {
                AlertDialog.Builder list = new AlertDialog.Builder(ctx);
                list.setTitle(R.string.advanced_list);
                String str = readAssets(ctx);
                list.setMessage(str);
                list.setPositiveButton(R.string.cancel, null);
                list.show();
            }
        });
        dialog.setView(layout);
        dialog.show();
    }

    private static String readAssets(Context ctx) {
        String result = "";
        try {
            InputStreamReader inputReader = new InputStreamReader(ctx.getResources().getAssets().open("KeyCode.txt"));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            while ((line = bufReader.readLine()) != null) {
                result += line + "\n";
            }
        } catch (IOException e) {
            result = "No Found";
            e.printStackTrace();
        }
        return result;
    }

    static boolean isHeadSetUse(Context ctx) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        Intent iStatus = ctx.registerReceiver(null, iFilter);
        return (iStatus != null ? iStatus.getIntExtra("state", 0) : 0) == 1;
    }

    static void showAttention(Context ctx) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle(R.string.attention_title);
        dialog.setMessage(R.string.attention_msg);
        dialog.show();
    }

    static Process getRootProcess(Runtime r) {
        Process p;
        try {
            p = r.exec("su");
        } catch (IOException e1) {
            e1.printStackTrace();
            p = null;
        }
        return p;
    }

    static DataOutputStream getStream(Process p) {
        if (p != null) {
            return new DataOutputStream(p.getOutputStream());
        }
        return null;
    }

    static void closeRuntime(Process p, DataOutputStream o) {
        try {
            if (o != null) {
                o.writeBytes("exit\n");
                o.close();
            }
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sendKeyCode(Context ctx, final int keyCode, final Process p, final DataOutputStream o, final boolean longpress) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS && !longpress) {
            MediaButtonControl(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT && !longpress) {
            MediaButtonControl(ctx, KeyEvent.KEYCODE_MEDIA_NEXT);
        } else {
            if (p != null) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            String str;
                            if (longpress) {
                                str = "input keyevent --longpress ";
                            } else {
                                str = "input keyevent ";
                            }
                            o.writeBytes(str + keyCode + "\n");
                            o.flush();
                            p.waitFor();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.start();
            }
        }
    }

    private static void MediaButtonControl(Context ctx, int Keycode) {
        KeyEvent ky_down = new KeyEvent(KeyEvent.ACTION_DOWN, Keycode);
        KeyEvent ky_up = new KeyEvent(KeyEvent.ACTION_UP, Keycode);
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(ky_down);
        am.dispatchMediaKeyEvent(ky_up);
    }

    static boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }
        return false;
    }

    static boolean isRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int i = process.waitFor();
            if (i == 0) {
                Runtime.getRuntime().exec("su");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    static int getKeyCodeUp(SharedPreferences sp) {
        return sp.getInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    static int getKeyCodeDown(SharedPreferences sp) {
        return sp.getInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    static boolean getLongPressSend(SharedPreferences sp) {
        return sp.getBoolean("LongPress_Send", false);
    }

    static boolean getLongPressGet(SharedPreferences sp) {
        return sp.getBoolean("LongPress_Get", false);
    }

    static boolean getLongPressCustom(SharedPreferences sp) {
        return sp.getBoolean("LongPress_Custom", false);
    }
}
