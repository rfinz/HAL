package HAL.core;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.text.format.*;
import android.view.View.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {

    static final int LAUNCH_DIALOG_ID = 0;
    public static boolean smsCancelled = false;
    EditText primary, secondary;
    TimePicker tp;
    public static ProgressBar progress;
    Time time = new Time();
    //Handler h = new Handler();
    static ProgramThread p = null;
    String received = "";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        primary = (EditText) findViewById(R.id.ETprimary);
        secondary = (EditText) findViewById(R.id.ETsecondary);
        tp = (TimePicker) findViewById(R.id.timePicker);


        final Button sButton = (Button) findViewById(R.id.sButton);
        sButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                int plen = primary.getText().toString().length();
                int slen = secondary.getText().toString().length();
                if ((plen == 10 || plen == 5) && (slen == 10 || slen == 5)) {
                    if (primary.getText().toString().equals(secondary.getText().toString())) {
                        Toast.makeText(getBaseContext(), "NUMBERS ARE IDENTICAL. HIT BACK AND THEN CANCEL TO CHANGE.", 3000).show();
                    }
                    time.setToNow();
                    time.hour = tp.getCurrentHour();
                    time.minute = tp.getCurrentMinute();
                    if (time.toMillis(true) < System.currentTimeMillis()) {
                        time.monthDay += 1;
                    }
                    p = new ProgramThread(primary.getText().toString(),
                            secondary.getText().toString(),
                            time.toMillis(true),
                            getApplicationContext(),
                            getWindow());
                    sButton.setEnabled(false);
                    tp.setEnabled(false);
                    primary.setEnabled(false);
                    secondary.setEnabled(false);
                    p.run();
                    showDialog(LAUNCH_DIALOG_ID);
                    progress.setProgress(0);
                } else {
                    Toast.makeText(getBaseContext(), "ERR: Phone numbers formatted incorrectly.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Button cButton = (Button) findViewById(R.id.cButton);
        cButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (p != null) {
                    sButton.setEnabled(true);
                    tp.setEnabled(true);
                    primary.setEnabled(true);
                    secondary.setEnabled(true);
                    if (!smsCancelled) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        p.postAtFrontOfQueue(p.kill());
                    }
                    p = null;
                    smsCancelled = false;
                    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    activityManager.killBackgroundProcesses("HAL.core");
                } else {
                    MainActivity.this.finish();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (p != null) {
            p.postAtFrontOfQueue(p.kill());
        }
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses("HAL.core");
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog dialog;
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_dialog, null);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        dialog = new Dialog(MainActivity.this/*mContext*/);
        dialog.setContentView(view);
        dialog.setTitle("Ready to deploy.");
        return dialog;
    }
;
}
