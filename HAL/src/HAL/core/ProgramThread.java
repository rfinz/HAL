/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HAL.core;

import HAL.core.SmsService.smsLocalBinder;
import HAL.core.GpsService.gpsLocalBinder;
import HAL.core.SettingsService.settLocalBinder;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 *
 * 
 * @author Prometheoid
 */
public class ProgramThread extends Handler implements Runnable{
    public static final long M15=900000,M5=300000,M1=60000;
    int hours;
    /*<alarm services>*/
    public static final String initial = "FIRST_CALL",subsequent = "SNOOZE_BUTTON";
    BroadcastReceiver once = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            ProgramThread.this.post(resumeGroundState());
            ProgramThread.this.postDelayed(broadcastFirstLocation(),10000);
            //Toast.makeText(context, "", 1000).show();
        }
    };

    BroadcastReceiver many = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            ProgramThread.this.post(broadcastLocation());
        }
    };
    
    private PendingIntent api = null, opi = null;
    /*</alarm services>*/
    
    static SmsService s;
    static SettingsService st;
    static GpsService g;
    String primary,secondary;
    long millis, currentTime;
    boolean smsBound=false,gpsBound=false,settBound=false;
    Context context;
    IBinder ib;
    Window window;
    
            
    ProgramThread(String primary, String secondary, long millis, Context context, Window window){
        this.primary = primary;
        this.secondary = secondary;
        this.millis = millis;
        this.context = context;
        this.window = window;
        context.registerReceiver(once, new IntentFilter(initial));
        context.registerReceiver(many, new IntentFilter(subsequent));
    }
    
    
    public void run() {
        currentTime = System.currentTimeMillis();
        float m = millis-currentTime;
        hours = Math.round(m/1000/60/60);
        //Log.d("Thread","Start");
        /*Initialize*/
        this.post(bindAndStart());
        this.postDelayed(confirmGPS(), 2000);
        this.postDelayed(confirmPhoneNumbers(),5000);
        this.postDelayed(setAlarm(), 10000); 
        this.postDelayed(setFlightState(), 30000);
    }
    
    //<editor-fold defaultstate="collapsed" desc="ServiceConnections">
    ServiceConnection sms = new ServiceConnection() {

      public void onServiceDisconnected(ComponentName name) {
       //Log.d("SmsService","End");
       smsBound = false;
       s = null;
      }

      public void onServiceConnected(ComponentName name, IBinder service) {
       //Log.d("SmsService","Start");
       smsBound = true;
       smsLocalBinder mLocalBinder = (smsLocalBinder)service;
       s = mLocalBinder.getInstance();
       s.setParent(ProgramThread.this);
       MainActivity.progress.incrementProgressBy(5);
      }
     };
    ServiceConnection sett = new ServiceConnection() {

      public void onServiceDisconnected(ComponentName name) {

       //Log.d("SettingService","End");
       settBound = false;
       s = null;
      }

      public void onServiceConnected(ComponentName name, IBinder service) {
       //Log.d("SettingService","Start");
       settBound = true;
       settLocalBinder mLocalBinder = (settLocalBinder)service;
       st = mLocalBinder.getInstance();
       st.setParent(ProgramThread.this);
       MainActivity.progress.incrementProgressBy(5);
      }
     };
    ServiceConnection gps = new ServiceConnection() {

      public void onServiceDisconnected(ComponentName name) {
       //Log.d("GpsService","End");
       gpsBound = false;
       g = null;
      }

      public void onServiceConnected(ComponentName name, IBinder service) {
       //Log.d("GpsService","Start");
       gpsBound = true;
       gpsLocalBinder mLocalBinder = (gpsLocalBinder)service;
       g = mLocalBinder.getInstance();
       g.setParent(ProgramThread.this);
       MainActivity.progress.incrementProgressBy(5);
      }
     };
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Runnables">
    public Runnable kill(){
        killMyself();
        Runnable r = new Runnable(){
            public void run(){
                st.setFlightMode(context.getContentResolver(), 0);
                st.setForeground(false);
                s.setForeground(false);
                g.setForeground(false);
                context.unregisterReceiver(once);
                context.unregisterReceiver(many);
                context.unbindService(sms);
                context.unbindService(gps);
                context.unbindService(sett);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        |WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        |WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if(api!=null){
                    alarm.cancel(api);
                    api = null;
                }
                if(opi!=null){
                    alarm.cancel(opi);
                    opi = null;
                }
            }
        };
        return r;
    }
    public Runnable bindAndStart(){
        Runnable r = new Runnable(){
            public void run(){
                bindAndStart("HAL.core.SMS_SERVICE",sms);
                bindAndStart("HAL.core.GPS_SERVICE",gps);
                bindAndStart("HAL.core.SETTINGS_SERVICE",sett);
            }
            private void bindAndStart(String action, ServiceConnection sc){
                Intent i = new Intent();
                i.setAction(action);
                context.bindService(i,sc,Context.BIND_AUTO_CREATE);
                context.startService(i);
            }
        };
        return r;
    }
    public Runnable confirmGPS(){
        Runnable r = new Runnable(){
            public void run(){
                if(!g.gps(context)){
                    ProgramThread.this.postAtFrontOfQueue(kill());
                }
                Toast.makeText(context, "First Location:"+g.longitude()+" lon. , "+g.latitude()+" lat.", 1000).show();
                MainActivity.progress.incrementProgressBy(10);
            }
        };
        return r;
    }
    public Runnable confirmPhoneNumbers(){
       Runnable r = new Runnable(){
            public void run(){
                s.sendSMS(primary, "HAL: You are my primary contact for this research vessel. I will be unreachable for roughly " 
                        +hours+" hours.");
                MainActivity.progress.incrementProgressBy(10);
                s.sendSMS(secondary, "HAL: You are my secondary contact for this research vessel. "
                        + "Secondary contacts are still able to text me for more information after I've landed.");
                MainActivity.progress.incrementProgressBy(10);
            }
        };
        return r;
    }
    public Runnable setFlightState(){
        Runnable r = new Runnable(){
            public void run(){
                st.setFlightMode(context.getContentResolver(), 1);
                st.setForeground(true);
                s.setForeground(true);
                g.setForeground(true);
                MainActivity.progress.setProgress(100);
            }
        };
        return r;
    }
    public Runnable setAlarm(){
        Runnable r = new Runnable(){
            
            public void run(){
                AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                opi = PendingIntent.getBroadcast(context, 0, new Intent(initial), 0);
                alarm.set(AlarmManager.RTC_WAKEUP, millis, opi);
                api = PendingIntent.getBroadcast(context, 0, new Intent(subsequent), 0);
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, millis+M15, M15, api);
                MainActivity.progress.incrementProgressBy(30);
            }
        };
        return r;
    }
    public Runnable resumeGroundState(){
        Runnable r = new Runnable(){
            public void run(){
                st.setFlightMode(context.getContentResolver(), 0);              
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        |WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        |WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                //MainActivity.progress.incrementProgressBy(5);
            }
        };
        return r;        
    }
    public Runnable broadcastFirstLocation(){
        Runnable r = new Runnable(){
            public void run(){
                s.sendReliable(primary,"HAL: I've landed at \n" 
                    + g.longitude()+" lon. \n"
                    + g.latitude() + " lat.");
            }
        };
        return r;        
    }    
    public Runnable broadcastLocation(){
        Runnable r = new Runnable(){
            public void run(){
                s.sendReliable(primary,"HAL: Now I'm at \n"
                        + g.longitude()+" lon. \n"
                        + g.latitude() + " lat. \n"
                        +"Text me \"whereru?\" to get my current location anytime.");
            }
        };
        return r; 
    }
    
    //</editor-fold>
    
    
    public void killMyself(){
        this.removeCallbacksAndMessages(null);
    }
    

    
}
