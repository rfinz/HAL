/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HAL.core;

import android.app.Service;
import android.content.*;
import android.location.*;
import android.os.Binder;
import android.os.Bundle;

import android.os.IBinder;
import android.provider.*;
import android.widget.*;

/**
 *
 * @author Prometheoid
 */
public class GpsService extends Service {
    LocationManager lm;
    ProgramThread parent;
    double latitude = 0,longitude = 0;
        class gpsLocalBinder extends Binder{ 
            public GpsService getInstance() {
                return GpsService.this;
            }
        }
        IBinder ib = new gpsLocalBinder();

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        //super.onCreate(icicle);
        // ToDo add your GUI initialization code here        
    }
    
    @Override
    public int onStartCommand(Intent i, int flags, int id){
            //Log.d("GPS_SERVICE","Started");
            lm = (LocationManager)getSystemService(LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
            return 0;
    }
    
    public boolean gps(Context context){
         boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
         if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
         }
         Criteria criteria = new Criteria();
         Toast.makeText(getBaseContext(),"Your highest resolution location data will be provided by "
                 + lm.getBestProvider(criteria, true)+".",Toast.LENGTH_SHORT).show();
         return enabled;
     }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public void onProviderEnabled(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void onProviderDisabled(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    //<editor-fold defaultstate="collapsed" desc="TODO satellites">
    /*
     * public int satellites(){
     * int ret;
     * Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
     * ret = l.getExtras().getInt("satellites");
     * return ret;
     * }
     */
    //</editor-fold>
 
    public double longitude(){
        try{
        Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        longitude = l.getLongitude();
        }catch(Exception e){longitude = 999;}
        return longitude;
    }
    
    public double latitude(){
        try{
        Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        latitude = l.getLatitude();
        }catch(Exception e){latitude = 999;}
        return latitude;
    }
    
    public void kill(){
        lm.removeUpdates(locationListener);
        super.stopSelf();
    } 
    
    public void setParent(ProgramThread p){
        parent = p;
    }      
     
    @Override
    public IBinder onBind(Intent arg0) {
        return ib;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        kill();
        return false;
    }
       
    
}
