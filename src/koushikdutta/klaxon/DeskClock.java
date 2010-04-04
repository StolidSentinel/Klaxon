/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package koushikdutta.klaxon;

import static android.os.BatteryManager.BATTERY_STATUS_CHARGING;
import static android.os.BatteryManager.BATTERY_STATUS_FULL;
import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {
    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "DeskClock";

    // Package ID of the music player.
    private static final String MUSIC_PACKAGE_ID = "com.android.music";

    // Alarm action for midnight (so we can update the date display).
    private static final String ACTION_MIDNIGHT = "koushikdutta.klaxon.MIDNIGHT";

    // Interval between polls of the weather widget. Its refresh period is
    // likely to be much longer (~3h), but we want to pick up any changes
    // within 5 minutes.
    private final long QUERY_WEATHER_DELAY = 5 * 60 * 1000; // 5 min

    // Delay before engaging the burn-in protection mode (green-on-black).
    private final long SCREEN_SAVER_TIMEOUT = 5* 60 * 1000; // 10 min

    // Repositioning delay in screen saver.
    private final long SCREEN_SAVER_MOVE_DELAY = 60 * 1000; // 1 min

    // Color to use for text & graphics in screen saver mode.
    private final int SCREEN_SAVER_COLOR = 0xFF308030;
    private final int SCREEN_SAVER_COLOR_DIM = 0xFF183018;

    // Opacity of black layer between clock display and wallpaper.
    private final float DIM_BEHIND_AMOUNT_NORMAL = 0.4f;
    private final float DIM_BEHIND_AMOUNT_DIMMED = 0.7f; // higher contrast when display dimmed

    // Internal message IDs.
    private final int QUERY_WEATHER_DATA_MSG     = 0x1000;
    private final int UPDATE_WEATHER_DISPLAY_MSG = 0x1001;
    private final int SCREEN_SAVER_TIMEOUT_MSG   = 0x2000;
    private final int SCREEN_SAVER_MOVE_MSG      = 0x2001;

    // Weather widget query information.
    private static final String GENIE_PACKAGE_ID = "com.google.android.apps.genie.geniewidget";
    private static final String WEATHER_CONTENT_AUTHORITY = GENIE_PACKAGE_ID + ".weather";
    private static final String WEATHER_CONTENT_PATH = "/weather/current";
    private static final String[] WEATHER_CONTENT_COLUMNS = new String[] {
            "location",
            "timestamp",
            "temperature",
            "highTemperature",
            "lowTemperature",
            "iconUrl",
            "iconResId",
            "description",
        };

    private static final String ACTION_GENIE_REFRESH = "com.google.android.apps.genie.REFRESH";

    // State variables follow.
    private DigitalClock mTime;
    private TextView mDate;

    private TextView mNextAlarm = null;
    private TextView mBatteryDisplay;

    private TextView mWeatherCurrentTemperature;
    private TextView mWeatherHighTemperature;
    private TextView mWeatherLowTemperature;
    private TextView mWeatherLocation;
    private ImageView mWeatherIcon;

    private String mWeatherCurrentTemperatureString;
    private String mWeatherHighTemperatureString;
    private String mWeatherLowTemperatureString;
    private String mWeatherLocationString;
    private Drawable mWeatherIconDrawable;

    private Resources mGenieResources = null;

    private boolean mDimmed = false;
    private boolean mScreenSaverMode = false;

    private String mDateFormat;

    private int mBatteryLevel = -1;
    private boolean mPluggedIn = false;

    private boolean mLaunchedFromDock = false;

    private int mIdleTimeoutEpoch = 0;

    private Random mRNG;

    private PendingIntent mMidnightIntent;
    
    private KlaxonSettings mKlaxonSettings;
    private boolean mFixWeather = false;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_DATE_CHANGED.equals(action)) {
                refreshDate();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                handleBatteryUpdate(
                    intent.getIntExtra("status", BATTERY_STATUS_UNKNOWN),
                    intent.getIntExtra("level", 0));
            } else if ("android.intent.action.DOCK_EVENT".equals(action)) {
                int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", -1);
                if (DEBUG) Log.d("ACTION_DOCK_EVENT, state=" + state);
                if (state == /* Intent.EXTRA_DOCK_STATE_UNDOCKED */ 0x00000000) {
                    if (mLaunchedFromDock) {
                        // moveTaskToBack(false);
                        finish();
                    }
                    mLaunchedFromDock = false;
                }
            }
        }
        
    };

    private final Handler mHandy = new Handler() {
        @Override
        public void handleMessage(Message m) {
            if (m.what == QUERY_WEATHER_DATA_MSG) {
                new Thread() { public void run() { queryWeatherData(); } }.start();
                scheduleWeatherQueryDelayed(QUERY_WEATHER_DELAY);
            } else if (m.what == UPDATE_WEATHER_DISPLAY_MSG) {
                updateWeatherDisplay();
            } else if (m.what == SCREEN_SAVER_TIMEOUT_MSG) {
                if (m.arg1 == mIdleTimeoutEpoch) {
                    saveScreen();
                }
            } else if (m.what == SCREEN_SAVER_MOVE_MSG) {
                moveScreenSaver();
            }
        }
    };


    private void moveScreenSaver() {
        moveScreenSaverTo(-1,-1);
    }
    private void moveScreenSaverTo(int x, int y) {
        if (!mScreenSaverMode) return;

        final View saver_view = findViewById(R.id.saver_view);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (x < 0 || y < 0) {
            int myWidth = saver_view.getMeasuredWidth();
            int myHeight = saver_view.getMeasuredHeight();
            x = (int)(mRNG.nextFloat()*(metrics.widthPixels - myWidth));
            y = (int)(mRNG.nextFloat()*(metrics.heightPixels - myHeight));
        }

        if (DEBUG) Log.d(String.format("screen saver: %d: jumping to (%d,%d)",
                System.currentTimeMillis(), x, y));

        saver_view.setLayoutParams(new AbsoluteLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            x,
            y));

        // Synchronize our jumping so that it happens exactly on the second.
        mHandy.sendEmptyMessageDelayed(SCREEN_SAVER_MOVE_MSG,
            SCREEN_SAVER_MOVE_DELAY +
            (1000 - (System.currentTimeMillis() % 1000)));
    }

    private void setWakeLock(boolean hold) {
        if (DEBUG) Log.d((hold ? "hold" : " releas") + "ing wake lock");
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= 0x00400000; //WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        winParams.flags |= 0x00080000; //WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        winParams.flags |= 0x00200000; //WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        if (hold)
            winParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        else
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.setAttributes(winParams);
    }

    private void restoreScreen() {
        if (!mScreenSaverMode) return;
        if (DEBUG) Log.d("restoreScreen");
        mScreenSaverMode = false;
        initViews();
        doDim(false); // restores previous dim mode
        // policy: update weather info when returning from screen saver
        if (mPluggedIn) requestWeatherDataFetch();
        refreshAll();
    }

    // Special screen-saver mode for OLED displays that burn in quickly
    private void saveScreen() {
        if (mScreenSaverMode) return;
        if (DEBUG) Log.d("saveScreen");

        // quickly stash away the x/y of the current date
        final View oldTimeDate = findViewById(R.id.time_date);
        int oldLoc[] = new int[2];
        oldTimeDate.getLocationOnScreen(oldLoc);

        mScreenSaverMode = true;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // give up any internal focus before we switch layouts
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        setContentView(R.layout.desk_clock_saver);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);

        final int color = mDimmed ? SCREEN_SAVER_COLOR_DIM : SCREEN_SAVER_COLOR;

        ((TextView)findViewById(R.id.timeDisplay)).setTextColor(color);
        ((TextView)findViewById(R.id.am_pm)).setTextColor(color);
        mDate.setTextColor(color);
        mNextAlarm.setTextColor(color);
        mNextAlarm.setCompoundDrawablesWithIntrinsicBounds(
            getResources().getDrawable(mDimmed
                ? R.drawable.ic_lock_idle_alarm_saver_dim
                : R.drawable.ic_lock_idle_alarm_saver),
            null, null, null);

        mBatteryDisplay =
        mWeatherCurrentTemperature =
        mWeatherHighTemperature =
        mWeatherLowTemperature =
        mWeatherLocation = null;
        mWeatherIcon = null;

        refreshDate();
        refreshAlarm();

        moveScreenSaverTo(oldLoc[0], oldLoc[1]);
    }

    @Override
    public void onUserInteraction() {
        if (mScreenSaverMode)
            restoreScreen();
    }

    // Tell the Genie widget to load new data from the network.
    private void requestWeatherDataFetch() {
        if (DEBUG) Log.d("forcing the Genie widget to update weather now...");
        sendBroadcast(new Intent(ACTION_GENIE_REFRESH).putExtra("requestWeather", true));
        // update the display with any new data
        scheduleWeatherQueryDelayed(5000);
    }

    private boolean supportsWeather() {
        return (mGenieResources != null);
    }

    private void scheduleWeatherQueryDelayed(long delay) {
        // cancel any existing scheduled queries
        unscheduleWeatherQuery();

        if (DEBUG) Log.d("scheduling weather fetch message for " + delay + "ms from now");

        mHandy.sendEmptyMessageDelayed(QUERY_WEATHER_DATA_MSG, delay);
    }

    private void unscheduleWeatherQuery() {
        mHandy.removeMessages(QUERY_WEATHER_DATA_MSG);
    }

    private void queryWeatherData() {
        // if we couldn't load the weather widget's resources, we simply
        // assume it's not present on the device.
        if (mGenieResources == null) return;

        Uri queryUri = new Uri.Builder()
            .scheme(android.content.ContentResolver.SCHEME_CONTENT)
            .authority(WEATHER_CONTENT_AUTHORITY)
            .path(WEATHER_CONTENT_PATH)
            .appendPath(new Long(System.currentTimeMillis()).toString())
            .build();

        if (DEBUG) Log.d("querying genie: " + queryUri);

        Cursor cur;
        try {
            cur = managedQuery(
                queryUri,
                WEATHER_CONTENT_COLUMNS,
                null,
                null,
                null);
        } catch (RuntimeException e) {
            Log.e("Weather query failed", e);
            cur = null;
        }

        if (cur != null && cur.moveToFirst()) {
            if (DEBUG) {
                java.lang.StringBuilder sb =
                    new java.lang.StringBuilder("Weather query result: {");
                for(int i=0; i<cur.getColumnCount(); i++) {
                    if (i>0) sb.append(", ");
                    sb.append(cur.getColumnName(i))
                        .append("=")
                        .append(cur.getString(i));
                }
                sb.append("}");
                Log.d(sb.toString());
            }

            mWeatherIconDrawable = mGenieResources.getDrawable(cur.getInt(
                cur.getColumnIndexOrThrow("iconResId")));

            mWeatherLocationString = cur.getString(
                cur.getColumnIndexOrThrow("location"));

            // any of these may be NULL
            final int colTemp = cur.getColumnIndexOrThrow("temperature");
            final int colHigh = cur.getColumnIndexOrThrow("highTemperature");
            final int colLow = cur.getColumnIndexOrThrow("lowTemperature");
            
            mWeatherCurrentTemperatureString =
                cur.isNull(colTemp)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colTemp));
            mWeatherHighTemperatureString =
                cur.isNull(colHigh)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colHigh));
            mWeatherLowTemperatureString =
                cur.isNull(colLow)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colLow));

            if (mFixWeather)
            {
                mWeatherCurrentTemperatureString =
                    cur.isNull(colTemp)
                        ? "\u2014"
                        : String.format("%d\u00b0", (int)((double)cur.getInt(colTemp) * 1.8d + 32d));
                mWeatherHighTemperatureString =
                    cur.isNull(colHigh)
                        ? "\u2014"
                        : String.format("%d\u00b0", (int)((double)cur.getInt(colHigh) * 1.8d + 32d));
                mWeatherLowTemperatureString =
                    cur.isNull(colLow)
                        ? "\u2014"
                        : String.format("%d\u00b0", (int)((double)cur.getInt(colLow) * 1.8d + 32d));
            }
        } else {
            Log.w("No weather information available (cur="
                + cur +")");
            mWeatherIconDrawable = null;
            mWeatherLocationString = getString(R.string.weather_fetch_failure);
            mWeatherCurrentTemperatureString =
                mWeatherHighTemperatureString =
                mWeatherLowTemperatureString = "";
        }

        mHandy.sendEmptyMessage(UPDATE_WEATHER_DISPLAY_MSG);
    }

    private void refreshWeather() {
        if (supportsWeather())
            scheduleWeatherQueryDelayed(0);
        updateWeatherDisplay(); // in case we have it cached
    }

    private void updateWeatherDisplay() {
        if (mWeatherCurrentTemperature == null) return;

        mWeatherCurrentTemperature.setText(mWeatherCurrentTemperatureString);
        mWeatherHighTemperature.setText(mWeatherHighTemperatureString);
        mWeatherLowTemperature.setText(mWeatherLowTemperatureString);
        mWeatherLocation.setText(mWeatherLocationString);
        mWeatherIcon.setImageDrawable(mWeatherIconDrawable);
    }

    // Adapted from KeyguardUpdateMonitor.java
    private void handleBatteryUpdate(int plugStatus, int batteryLevel) {
        final boolean pluggedIn = (plugStatus == BATTERY_STATUS_CHARGING || plugStatus == BATTERY_STATUS_FULL);
        if (pluggedIn != mPluggedIn) {
            setWakeLock(pluggedIn);

            if (pluggedIn) {
                // policy: update weather info when attaching to power
                requestWeatherDataFetch();
            }
        }
        if (pluggedIn != mPluggedIn || batteryLevel != mBatteryLevel) {
            mBatteryLevel = batteryLevel;
            mPluggedIn = pluggedIn;
            refreshBattery();
        }
    }

    private void refreshBattery() {
        if (mBatteryDisplay == null) return;

        if (mPluggedIn /* || mBatteryLevel < LOW_BATTERY_THRESHOLD */) {
            mBatteryDisplay.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, android.R.drawable.ic_lock_idle_charging, 0);
            mBatteryDisplay.setText(
                getString(R.string.battery_charging_level, mBatteryLevel));
            mBatteryDisplay.setVisibility(View.VISIBLE);
        } else {
            mBatteryDisplay.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshDate() {
        final Date now = new Date();
        if (DEBUG) Log.d("refreshing date..." + now);
        mDate.setText(DateFormat.format(mDateFormat, now));
    }

    protected void refreshAlarm() {
        if (mNextAlarm == null) return;

        String nextAlarm = Settings.System.getString(getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            mNextAlarm.setText(nextAlarm);
            //mNextAlarm.setCompoundDrawablesWithIntrinsicBounds(
            //    android.R.drawable.ic_lock_idle_alarm, 0, 0, 0);
            mNextAlarm.setVisibility(View.VISIBLE);
        } else {
            mNextAlarm.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshAll() {
        refreshDate();
        refreshAlarm();
        refreshBattery();
        refreshWeather();
    }

    private void doDim(boolean fade) {
        View tintView = findViewById(R.id.window_tint);
        if (tintView == null) return;

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();

        winParams.flags |= (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        winParams.flags |= (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // dim the wallpaper somewhat (how much is determined below)
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        if (mDimmed) {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            winParams.dimAmount = DIM_BEHIND_AMOUNT_DIMMED;

            // show the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this,
                fade ? R.anim.dim
                     : R.anim.dim_instant));
        } else {
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            winParams.dimAmount = DIM_BEHIND_AMOUNT_NORMAL;

            // hide the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this,
                fade ? R.anim.undim
                     : R.anim.undim_instant));
        }

        win.setAttributes(winParams);
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d("onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event 
        setIntent(newIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d("onResume with intent: " + getIntent());

        // reload the date format in case the user has changed settings
        // recently
        try
        {
            // mDateFormat = getString(com.android.internal.R.string.full_wday_month_day_no_year);
        	Class clazz = Class.forName("com.android.internal.R$string");
        	java.lang.reflect.Field field = clazz.getField("full_wday_month_day_no_year");
        	mDateFormat = getString(field.getInt(null));        	
        }
        catch(Exception ex)
        {
        	mDateFormat = "EEEE, d MMMM";
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("android.intent.action.DOCK_EVENT");//Intent.ACTION_DOCK_EVENT);
        filter.addAction(ACTION_MIDNIGHT);
        registerReceiver(mIntentReceiver, filter);

        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, 1);
        mMidnightIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_MIDNIGHT), 0);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC, today.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mMidnightIntent);

        // un-dim when resuming
        mDimmed = false;
        doDim(false);

        restoreScreen(); // disable screen saver
        refreshAll(); // will schedule periodic weather fetch

        setWakeLock(mPluggedIn);

        mIdleTimeoutEpoch++;
        mHandy.sendMessageDelayed(
            Message.obtain(mHandy, SCREEN_SAVER_TIMEOUT_MSG, mIdleTimeoutEpoch, 0),
            SCREEN_SAVER_TIMEOUT);

        final boolean launchedFromDock
            = getIntent().hasCategory("android.intent.category.DESK_DOCK");//Intent.CATEGORY_DESK_DOCK);

        if (supportsWeather() && launchedFromDock && !mLaunchedFromDock) {
            // policy: fetch weather if launched via dock connection
            if (DEBUG) Log.d("Device now docked; forcing weather to refresh right now");
            requestWeatherDataFetch();
        }

        mLaunchedFromDock = launchedFromDock;
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.d("onPause");

        // Turn off the screen saver. (But don't un-dim.)
        restoreScreen();

        // Other things we don't want to be doing in the background.
        unregisterReceiver(mIntentReceiver);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(mMidnightIntent);
        unscheduleWeatherQuery();

        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.d("onStop");

        // Avoid situations where the user launches Alarm Clock and is
        // surprised to find it in dim mode (because it was last used in dim
        // mode, but that last use is long in the past).
        mDimmed = false;

        super.onStop();
    }

    protected void initViews() {
        // give up any internal focus before we switch layouts
        final View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();

        setContentView(R.layout.desk_clock);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mBatteryDisplay = (TextView) findViewById(R.id.battery);

        mTime.getRootView().requestFocus();

        mWeatherCurrentTemperature = (TextView) findViewById(R.id.weather_temperature);
        mWeatherHighTemperature = (TextView) findViewById(R.id.weather_high_temperature);
        mWeatherLowTemperature = (TextView) findViewById(R.id.weather_low_temperature);
        mWeatherLocation = (TextView) findViewById(R.id.weather_location);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);

        final View.OnClickListener alarmClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(DeskClock.this, AlarmClock.class));
            }
        };

        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);
        mNextAlarm.setOnClickListener(alarmClickListener);

        final ImageButton alarmButton = (ImageButton) findViewById(R.id.alarm_button);
        alarmButton.setOnClickListener(alarmClickListener);

        final ImageButton galleryButton = (ImageButton) findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            .putExtra("slideshow", true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e("Couldn't launch image browser", e);
                }
            }
        });

        final ImageButton musicButton = (ImageButton) findViewById(R.id.music_button);
        musicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                	Intent musicAppQuery = new Intent();
                	musicAppQuery.setClassName(MUSIC_PACKAGE_ID, "com.android.music.MusicBrowserActivity");
                	musicAppQuery.setAction(Intent.ACTION_MAIN);
                	musicAppQuery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    if (musicAppQuery != null) {
                        startActivity(musicAppQuery);
                    }
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e("Couldn't launch music browser", e);
                }
            }
        });

        final ImageButton homeButton = (ImageButton) findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(
                    new Intent(Intent.ACTION_MAIN)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addCategory(Intent.CATEGORY_HOME));
            }
        });

        final ImageButton nightmodeButton = (ImageButton) findViewById(R.id.nightmode_button);
        nightmodeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDimmed = ! mDimmed;
                doDim(true);
            }
        });

        nightmodeButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                saveScreen();
                return true;
            }
        });

        final View weatherView = findViewById(R.id.weather);
        weatherView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!supportsWeather()) return;

                Intent genieAppQuery;
				try {
					genieAppQuery = getPackageManager()
					    .getLaunchIntentForPackage(GENIE_PACKAGE_ID)
					    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
	                if (genieAppQuery != null) {
	                    startActivity(genieAppQuery);
	                }
				} catch (Exception e) {
				}
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mScreenSaverMode) {
            initViews();
            doDim(false);
            refreshAll();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_alarms) {
            startActivity(new Intent(DeskClock.this, AlarmClock.class));
            return true;
        } else if (item.getItemId() == R.id.menu_item_add_alarm) {
			AlarmSettings settings = new AlarmSettings(this, mDatabase);
			settings.insert();
			AlarmSettings.editAlarm(this, settings.get_Id());
			return true;
        } else if (item.getItemId() == R.id.menu_item_fix_weather) {
        	mFixWeather = !mFixWeather;
        	mKlaxonSettings.setFixWeather(mFixWeather);
        	refreshWeather();
        }
        return false;
    }
  	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.desk_clock_menu, menu);
        return true;
    }
    
    SQLiteDatabase mDatabase;
    @Override
    protected void onCreate(Bundle icicle) {
    	WallpaperHelper.setWindowBackground(this);

		super.onCreate(icicle);
             
		mDatabase = AlarmSettings.getDatabase(this);
		mKlaxonSettings = new KlaxonSettings(this);
		mFixWeather = mKlaxonSettings.getFixWeather();

        mRNG = new Random();

        try {
            mGenieResources = getPackageManager().getResourcesForApplication(GENIE_PACKAGE_ID);
        } catch (PackageManager.NameNotFoundException e) {
            // no weather info available
            Log.w("Can't find "+GENIE_PACKAGE_ID+". Weather forecast will not be available.");
        }

        initViews();
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mDatabase.close();
    }
}
