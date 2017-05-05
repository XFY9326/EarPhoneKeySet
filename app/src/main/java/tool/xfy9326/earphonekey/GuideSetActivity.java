package tool.xfy9326.earphonekey;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View.OnClickListener;
import java.io.IOException;

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
			setEarPhoneDevice(false);
		}
		setView();
	}

	private void setEarPhoneDevice(boolean back_and_not_exit)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.guide_check);
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.dialog_guide_earphone_correct, null);
		final TextView notice = (TextView) layout.findViewById(R.id.textview_earphone_correct_info);
		notice.setText(R.string.guide_check_up);
		dialog.setView(layout);
		dialog.setCancelable(false);
		if (back_and_not_exit)
		{
			dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface d, int i)
					{
						check.dismiss();
					}
				});
		}
		else
		{
			dialog.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface d, int i)
					{
						GuideSetActivity.this.finish();
						System.exit(0);
					}
				});
		}
		dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
				public boolean onKey(DialogInterface d, int i, KeyEvent e)
				{
					int keyCode = e.getKeyCode();
					int action = e.getAction();
					if (checkmode && action == KeyEvent.ACTION_UP)
					{
						if (Methods.isHeadSetUse(GuideSetActivity.this))
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
									Toast.makeText(GuideSetActivity.this, getString(R.string.guide_check_error) + KeyEvent.keyCodeToString(keyCode) + "(" + keyCode + ")", Toast.LENGTH_SHORT).show();
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
									Toast.makeText(GuideSetActivity.this, getString(R.string.guide_check_error) + KeyEvent.keyCodeToString(keyCode) + "(" + keyCode + ")", Toast.LENGTH_SHORT).show();
								}
							}
						}
						else
						{
							Toast.makeText(GuideSetActivity.this, R.string.earphone_nofound, Toast.LENGTH_SHORT).show();
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
		Button attention = (Button) findViewById(R.id.button_attention);
		Button advanced = (Button) findViewById(R.id.button_advanced);
		TextView service_run = (TextView) findViewById(R.id.textview_service_run);
		CheckBox get = (CheckBox) findViewById(R.id.checkbox_longpress_get);
		get.setChecked(sp.getBoolean("LongPress_Get", false));
		if (!Methods.isAccessibilitySettingsOn(this))
		{
			service_run.setVisibility(View.GONE);
		}
		root.setOnClickListener(new OnClickListener(){
				public void onClick(View v)
				{
					if (Methods.isRoot())
					{
						startService(new Intent(GuideSetActivity.this, EarPhoneSetService.class).putExtra("ProcessChange", true));
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
		get.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton button, boolean b)
			{
				sped.putBoolean("LongPress_Get", b);
				sped.commit();
			}
		});
		recorrect.setOnClickListener(new OnClickListener(){
				public void onClick(View v)
				{
					if (Methods.isAccessibilitySettingsOn(GuideSetActivity.this))
					{
						Toast.makeText(GuideSetActivity.this, R.string.recorrect_error, Toast.LENGTH_SHORT).show();
					}
					else
					{
						checkmode = true;
						upcheck = false;
						downcheck = false;
						setEarPhoneDevice(true);
					}
				}
			});
		attention.setOnClickListener(new OnClickListener(){
				public void onClick(View v)
				{
					Methods.showAttention(GuideSetActivity.this);
				}
			});
		advanced.setOnClickListener(new OnClickListener(){
				public void onClick(View v)
				{
					if (Methods.isRoot())
					{
						Methods.showAdvancedFuntion(GuideSetActivity.this);
						if (!sp.getBoolean("AdvancedFunctionOn", false))
						{
							sped.putBoolean("AdvancedFunctionOn", true);
							sped.commit();
							startService(new Intent(GuideSetActivity.this, EarPhoneSetService.class).putExtra("ProcessChange", true));
						}
					}
					else
					{
						if (sp.getBoolean("AdvancedFunctionOn", false))
						{
							sped.putBoolean("AdvancedFunctionOn", false);
							sped.commit();
							startService(new Intent(GuideSetActivity.this, EarPhoneSetService.class).putExtra("ProcessChange", true));
						}
						Toast.makeText(GuideSetActivity.this, R.string.device_no_root, Toast.LENGTH_LONG).show();
					}
				}
			});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == AccessibilityService_Ask_Code)
		{
			TextView service_run = (TextView) findViewById(R.id.textview_service_run);
			if (Methods.isAccessibilitySettingsOn(this))
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
