package tool.xfy9326.earphonekey;

import android.app.*;
import android.content.*;
import android.media.*;
import android.preference.*;
import android.provider.*;
import android.view.*;
import android.widget.*;
import java.io.*;

public class Methods
{
	public static void showAdvancedFuntion(final Context ctx)
	{
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View layout = inflater.inflate(R.layout.dialog_advance_keyset_layout, null);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		final SharedPreferences.Editor sped = sp.edit();
		final EditText up = (EditText) layout.findViewById(R.id.edittext_up_key);
		final EditText down = (EditText) layout.findViewById(R.id.edittext_down_key);
		final CheckBox send = (CheckBox) layout.findViewById(R.id.checkbox_longpress_send);
		final CheckBox get = (CheckBox) layout.findViewById(R.id.checkbox_longpress_get);
		final CheckBox custom = (CheckBox) layout.findViewById(R.id.checkbox_longpress_custom);
		custom.setChecked(sp.getBoolean("LongPress_Custom", false));
		send.setChecked(sp.getBoolean("LongPress_Send", false));
		get.setChecked(sp.getBoolean("LongPress_Get", false));
		up.setText(sp.getInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS) + "");
		down.setText(sp.getInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT) + "");
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
		dialog.setTitle(R.string.advanced_title);
		dialog.setPositiveButton(R.string.done, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface d, int i)
				{
					if (up.getText().toString().trim().equals("") || down.getText().toString().trim().equals(""))
					{
						Toast.makeText(ctx, R.string.advanced_wrong, Toast.LENGTH_SHORT).show();
					}
					else
					{
						sped.putBoolean("LongPress_Custom", custom.isChecked());
						sped.putBoolean("LongPress_Send", send.isChecked());
						sped.putBoolean("LongPress_Get", get.isChecked());
						sped.putInt("CustomCode_UP", Integer.parseInt(up.getText().toString()));
						sped.putInt("CustomCode_DOWN", Integer.parseInt(down.getText().toString()));
						sped.commit();
					}
				}
			});
		dialog.setNegativeButton(R.string.reset, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface d, int i)
				{
					sped.putInt("CustomCode_UP", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
					sped.putInt("CustomCode_DOWN", KeyEvent.KEYCODE_MEDIA_NEXT);
					sped.commit();
				}
			});
		dialog.setNeutralButton(R.string.advanced_list, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface d, int i)
				{
					AlertDialog.Builder list = new AlertDialog.Builder(ctx);
					list.setTitle(R.string.advanced_list);
					String str = readAssets(ctx, "KeyCode.txt");
					list.setMessage(str);
					list.setPositiveButton(R.string.cancel, null);
					list.show();
				}
			});
		dialog.setView(layout);
		dialog.show();
	}

	public static String readAssets(Context ctx, String path)
	{
		String result="";
        try
        {
            InputStreamReader inputReader = new InputStreamReader(ctx.getResources().getAssets().open(path));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line="";
            while ((line = bufReader.readLine()) != null)
            {
                result += line + "\n";
            }
        }
        catch (IOException e)
        {
            result = "No Found";
			e.printStackTrace();
        }
		return result;
	}

	public static boolean isHeadSetUse(Context ctx)
	{
		AudioManager localAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
		return localAudioManager.isWiredHeadsetOn();
	}

	public static void showAttention(Context ctx)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
		dialog.setTitle(R.string.attention_title);
		dialog.setMessage(R.string.attention_msg);
		dialog.show();
	}

	public static Process getRootProcess(Runtime r)
	{
		Process p;
		try
		{
			p = r.exec("su");
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			try
			{
				p = r.exec("");
			}
			catch (IOException e2)
			{
				e2.printStackTrace();
				p = null;
			}
		}
		return p;
	}

	public static DataOutputStream getStream(Process p)
	{
		DataOutputStream o = new DataOutputStream(p.getOutputStream());
		return o;
	}

	public static void closeRuntime(Process p, DataOutputStream o)
	{
		try
		{
			if (o != null)
			{
				o.writeBytes("exit\n");
				o.close();
			}
			p.destroy();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void sendKeyCode(final int keyCode, final Process p, final DataOutputStream o, final boolean longpress)
	{
		Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						String str = "";
						if (longpress)
						{
							str = "input keyevent --longpress ";
						}
						else
						{
							str = "input keyevent ";
						}
						o.writeBytes(str + keyCode + "\n");
						o.flush();
						p.waitFor();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		t.start();
	}

	public static boolean isAccessibilitySettingsOn(Context context)
	{
        int accessibilityEnabled = 0;
        try
		{
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        }
		catch (Settings.SettingNotFoundException e)
		{
			e.printStackTrace();
        }
        if (accessibilityEnabled == 1)
		{
            String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null)
			{
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }
        return false;
    }

	public static boolean isRoot()
	{
		Process process;
		try
		{
			process  = Runtime.getRuntime().exec("su");
			process.getOutputStream().write("exit\n".getBytes());
			process.getOutputStream().flush();
			int i = process.waitFor();
			if (0 == i)
			{
				process = Runtime.getRuntime().exec("su");
				return true;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}

	public static boolean haveRoot()
	{
		int i = execRootCmdSilent("echo test");
		if (i != -1)  return true;
		return false;
	}

	private static int execRootCmdSilent(String paramString)
	{
        try
		{
            Process localProcess = Runtime.getRuntime().exec("su");
            Object localObject = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream((OutputStream) localObject);
            String str = String.valueOf(paramString);
            localObject = str + "\n";
            localDataOutputStream.writeBytes((String) localObject);
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            localProcess.waitFor();
            int result = localProcess.exitValue();
            return (Integer) result;
        }
		catch (Exception localException)
		{
            localException.printStackTrace();
            return -1;
        }
    }
}
