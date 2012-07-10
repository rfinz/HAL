/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HAL.core;

import android.app.Service;
import android.content.*;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.*;

/**
 *
 * @author Prometheoid
 */
public class SettingsService extends Service {

    ProgramThread parent;

    class settLocalBinder extends Binder {

        public SettingsService getInstance() {
            return SettingsService.this;
        }
    }
    IBinder ib = new settLocalBinder();

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        //super.onCreate(icicle);
        // ToDo add your GUI initialization code here
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        //Log.d("SETTINGS_SERVICE","Started");
        return 0;
    }

    public void setFlightMode(ContentResolver cr, int state) {
        boolean isEnabled = Settings.System.getInt(
                getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        if (isEnabled != (state > 0)) {
            Settings.System.putInt(
                    cr,
                    android.provider.Settings.System.AIRPLANE_MODE_ON, state);
            // Post an intent to reload
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", !isEnabled);
            sendBroadcast(intent);
            if (state > 0) {
                Toast.makeText(getBaseContext(), "Flight mode is now ENABLED.", Toast.LENGTH_SHORT).show();
            }
            if (state == 0) {
                Toast.makeText(getBaseContext(), "Flight mode is now DISABLED.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Flight mode did not change state.", Toast.LENGTH_SHORT).show();
        }
    }

    public void kill() {
        super.stopSelf();
    }

    public void setParent(ProgramThread p) {
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
