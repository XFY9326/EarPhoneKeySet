package tool.xfy9326.earphonekey;

import android.app.*;
import android.content.*;
import android.media.*;
import android.provider.*;
import java.io.*;

public class Methods
{
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
	
	public static Process getRootProcess (Runtime r)
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
	
	public static DataOutputStream getStream (Process p)
	{
		DataOutputStream o = new DataOutputStream(p.getOutputStream());
		return o;
	}
	
	public static void closeRuntime (Process p, DataOutputStream o)
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

	public static void sendKeyCode(final int keyCode, final Process p, final DataOutputStream o)
	{
		Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						o.writeBytes("input keyevent " + keyCode + "\n");
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
