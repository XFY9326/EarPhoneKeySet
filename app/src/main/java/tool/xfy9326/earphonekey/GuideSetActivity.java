package tool.xfy9326.earphonekey;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;

import java.lang.Process;
import android.preference.*;

public class GuideSetActivity extends Activity
{
	private static int AccessibilityService_Ask_Code = 1;
	private SharedPreferences sp;
	private SharedPreferences.Editor sped;
	private AlertDialog check;
	private boolean checkmode;
	private boolean upcheck;
	private boolean downcheck;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_guide_layout);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		sped = sp.edit();
		checkmode = sp.getBoolean("CheckEarPhone", false);
		if (!checkmode)
		{
			checkmode = true;
			upcheck = false;
			downcheck = false;
			setEarPhoneDevice();
		}
		setView();
	}

	private void setEarPhoneDevice()
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.guide_check);
		final TextView notice = new TextView(this);
		notice.setText(R.string.guide_check_up);
		notice.setPadding(30,90,30,90);
		notice.setTextSize(18.5f);
		notice.setGravity(Gravity.FILL_HORIZONTAL);
		dialog.setView(notice);
		dialog.setCancelable(false);
		dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
			public boolean onKey(DialogInterface d, int i, KeyEvent e)
			{
				int keyCode = e.getKeyCode();
				int action = e.getAction();
				if (checkmode && action == KeyEvent.ACTION_UP && keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_HOME && keyCode != KeyEvent.KEYCODE_MENU)
				{
					if (!upcheck)
					{
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
						{
							sped.putInt("KeyCode_UP", e.getScanCode());
							sped.commit();
							upcheck = true;
							notice.setText(R.string.guide_check_down);
						}
						else
						{
							Toast.makeText(GuideSetActivity.this, R.string.guide_check_error, Toast.LENGTH_SHORT).show();
						}
					}
					else if (upcheck && !downcheck)
					{
						if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
						{
							sped.putInt("KeyCode_DOWN", e.getScanCode());
							sped.putBoolean("CheckEarPhone", true);
							sped.commit();
							downcheck = true;
							check.dismiss();
						}
						else
						{
							Toast.makeText(GuideSetActivity.this, R.string.guide_check_error, Toast.LENGTH_SHORT).show();
						}
					}
				}
				return true;
			}
		});
		check = dialog.show();
	}

	private void setView()
	{
		Button root = (Button) findViewById(R.id.button_root);
		Button service = (Button) findViewById(R.id.button_service);
		Button recorrect = (Button) findViewById(R.id.button_recorrect);
		TextView root_get = (TextView) findViewById(R.id.textview_root_get);
		TextView service_run = (TextView) findViewById(R.id.textview_service_run);
		if (haveRoot())
		{
			if (!isRoot())
			{
				root_get.setVisibility(View.GONE);
			}
			if (!isAccessibilitySettingsOn(this))
			{
				service_run.setVisibility(View.GONE);
			}
			root.setOnClickListener(new OnClickListener(){
					public void onClick(View v)
					{
						try
						{
							Runtime.getRuntime().exec("su");
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				});
			service.setOnClickListener(new OnClickListener(){
					public void onClick(View v)
					{
						Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
						startActivityForResult(intent, AccessibilityService_Ask_Code);
					}
				});
			recorrect.setOnClickListener(new OnClickListener(){
					public void onClick(View v)
					{
						if (isAccessibilitySettingsOn(GuideSetActivity.this))
						{
							Toast.makeText(GuideSetActivity.this, R.string.recorrect_error, Toast.LENGTH_SHORT).show();
						}
						else
						{
							checkmode = true;
							upcheck = false;
							downcheck = false;
							setEarPhoneDevice();
						}
					}
				});
		}
		else
		{
			root.setEnabled(false);
			service.setEnabled(false);
			root_get.setVisibility(View.GONE);
			service_run.setVisibility(View.GONE);
			Toast.makeText(this, R.string.device_no_root, Toast.LENGTH_LONG).show();
		}
	}

	private static boolean isAccessibilitySettingsOn(Context context)
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

	private boolean isRoot()
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


	private static boolean haveRoot()
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == AccessibilityService_Ask_Code)
		{
			TextView service_run = (TextView) findViewById(R.id.textview_service_run);
			if (isAccessibilitySettingsOn(this))
			{
				service_run.setVisibility(View.VISIBLE);
			}
			else
			{
				service_run.setVisibility(View.GONE);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


}
