package com.holenet.cowinfo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Calendar;

public class AlarmService extends Service {
    final boolean DEBUG = false;

    boolean quit;
    DatabaseHelper dbHelper;
    NotificationManager notiManager;
    int time = 9;
    long delay = 30000;
    boolean sound = true;

    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = new DatabaseHelper(this);
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        Toast.makeText(this, "Service End", Toast.LENGTH_SHORT).show();
        quit = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        quit = false;
        SharedPreferences pref = getSharedPreferences("settings", 0);
        time = pref.getInt("notice_time", 9);
        sound = pref.getBoolean("notice_sound", true);
        if(DEBUG)
            delay = 1000;
        AlarmThread thread = new AlarmThread(this, handler);
        thread.start();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class AlarmThread extends Thread {
        AlarmService parent;
        Handler handler;

        SharedPreferences pref;
        SharedPreferences.Editor editor;
        boolean loaded = false;
        int year, month, day, hour, minute;

        public AlarmThread(AlarmService parent, Handler handler) {
            pref = getSharedPreferences("alarm", 0);
            editor = pref.edit();
            this.parent = parent;
            this.handler = handler;
        }

        @Override
        public void run() {
            while(!quit) {
                if(!loaded) {
                    load();
                    loaded = true;
                }

                Log.d("CowInfoService", "clock["+time+"]: "+year+" "+month+" "+day+(DEBUG ? " "+hour+" "+minute : ""));

                Calendar now = Calendar.getInstance();
                int cYear = now.get(Calendar.YEAR);
                int cMonth = now.get(Calendar.MONTH);
                int cDay = now.get(Calendar.DAY_OF_MONTH);
                int cHour = now.get(Calendar.HOUR_OF_DAY);
                int cMinute = now.get(Calendar.MINUTE);
                int cSecond = now.get(Calendar.SECOND);
                if((cYear!=year || cMonth!=month || cDay!=day || (DEBUG && (cHour!=hour || cMinute!=minute))) && (DEBUG ? cSecond>=time : cHour>=time)) {
                    year = cYear;
                    month = cMonth;
                    day = cDay;
                    if(DEBUG) {
                        hour = cHour;
                        minute = cMinute;
                    }
                    save();

                    int[] count = getCount();
                    if(DEBUG ? count[0]+count[1]>=0 : count[0]+count[1]>0) {
                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = count;
                        handler.sendMessage(msg);
                    }
                }

                try {
                    Thread.sleep(delay);
                } catch (Exception e) {}
            }
        }

        private void load() {
            year = pref.getInt("year", 0);
            month = pref.getInt("month", 0);
            day = pref.getInt("day", 0);
            if(DEBUG) {
                hour = pref.getInt("hour", 0);
                minute = pref.getInt("minute", 0);
            }
        }

        private void save() {
            editor.putInt("year", year);
            editor.putInt("month", month);
            editor.putInt("day", day);
            if(DEBUG) {
                editor.putInt("hour", hour);
                editor.putInt("minute", minute);
            }
            editor.apply();
        }
    }

    private int[] getCount() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, 1);
        int cYear = now.get(Calendar.YEAR);
        int cMonth = now.get(Calendar.MONTH)+1;
        int cDay = now.get(Calendar.DAY_OF_MONTH);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("select prop_id, content, year, month, day from " + DatabaseHelper.detailTable + " where content = '재발' or content = '분만'", null);
        int recodeCount = c.getCount();

        int cjb = 0;
        int cbm = 0;
        for(int i=0; i<recodeCount; i++) {
            c.moveToNext();
            int prop_id = c.getInt(0);
            String content = c.getString(1);
            int year = c.getInt(2);
            int month = c.getInt(3);
            int day = c.getInt(4);
            if(cYear==year && cMonth==month && cDay==day) {
                if(content.equals("재발"))
                    cjb++;
                else if(content.equals("분만"))
                    cbm++;
            }
        }
        c.close();
        db.close();

        return new int[] {cjb, cbm};
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int[] count = (int[]) msg.obj;
            if (msg.what==0) {
//                Toast.makeText(AlarmService.this, count[0]+" : "+count[1], Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AlarmService.this, RecodeActivity.class);
                intent.putExtra("notification", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent content = PendingIntent.getActivity(AlarmService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification.Builder notiBuilder = new Notification.Builder(AlarmService.this)
                        .setTicker("Alarm")
                        .setContentTitle("재발 예정 "+count[0]+", 분만 예정 "+count[1])
                        .setContentText("한우이력정보")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setLights(0xFFFF00, 1000, 1000)
                        .setAutoCancel(true)
                        .setContentIntent(content);
                if(sound) {
                    notiBuilder = notiBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    Notification noti = notiBuilder.build();
                    notiManager.notify(1, noti);
                    Log.d("CowInfoService", "Notification notified.");
                }
            }
        }
    };
}
