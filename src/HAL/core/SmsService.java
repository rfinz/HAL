/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HAL.core;

import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.*;

/**
 *
 * @author Prometheoid
 */
public class SmsService extends Service {

    Context context;
    ContentResolver contentResolver;
    ProgramThread parent;
    String msg = "";

    class smsLocalBinder extends Binder {

        public SmsService getInstance() {
            return SmsService.this;
        }
    }
    IBinder ib = new smsLocalBinder();
    BroadcastReceiver receiver = new BroadcastReceiver() {

        String str = "";
        String originatingAddress = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            //---get the SMS message passed in---
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            if (bundle != null) {
                //---retrieve the SMS message received---
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    str += msgs[i].getMessageBody().toString();
                    originatingAddress = msgs[i].getOriginatingAddress();
                }
                if (originatingAddress.equals(parent.primary) || originatingAddress.equals(parent.secondary)) {
                    if (parent != null) {
                        if (str.contains("whereru?")) {

                            SmsService.this.sendSMS(originatingAddress, "HAL: I am at " + parent.g.longitude()
                                    + " lon. and \n" + parent.g.latitude() + " lat. \n");

                        } else if (str.contains("thankyou")) {
                            SmsService.this.sendSMS(originatingAddress, "HAL: You're welcome.");
                            parent.postAtFrontOfQueue(parent.kill());
                            MainActivity.smsCancelled = true;

                        } else {
                        }
                    }
                }


                //---display the new SMS message---
                //intent.setComponent(null);
                // do what Market/Store/Finsky should have done in the first place
                //List<ResolveInfo> l=context.getPackageManager().queryBroadcastReceivers(intent, 0);
                //Toast.makeText(context, "size: " + l.size(), Toast.LENGTH_SHORT).show();
            }
            str = "";
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        //super.onCreate(icicle);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        this.registerReceiver(this.receiver, filter);
        context = getApplicationContext();
        contentResolver = context.getContentResolver();
        //Log.d("SMS_SERVICE","Started");
        return 0;
    }

    public void sendSMS(String phoneNumber, String message) {
        PendingIntent pi = PendingIntent.getService(this, 0,
                new Intent(this, SmsService.class), 0);
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, pi, null);
            Toast.makeText(getBaseContext(), "SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            //Log.d("ERROR",e.toString());
        }
    }

    //---sends an SMS message to another device---
    public void sendReliable(String phoneNumber, String message) {
        msg = message;
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {

            String msg = SmsService.this.msg;

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case -1:
                        Toast.makeText(getBaseContext(), "SMS sent reliably.",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case -1:
                        Toast.makeText(getBaseContext(), "SMS delivered reliably.",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.unregisterReceiver(this);
                        break;
                    case 0:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        SmsService.this.sendSMS(parent.secondary, msg);
                        SmsService.this.unregisterReceiver(this);
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    public void kill() {
        this.unregisterReceiver(this.receiver);
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
