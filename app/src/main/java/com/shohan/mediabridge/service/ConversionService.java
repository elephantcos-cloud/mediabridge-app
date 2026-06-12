package com.shohan.mediabridge.service;
import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.shohan.mediabridge.MainActivity;
import com.shohan.mediabridge.R;

public class ConversionService extends Service {
    public static final String ACTION_START  = "START";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_STOP   = "STOP";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_TOTAL    = "total";
    private static final String CH = "mb_fg";
    public  static final int    NID = 1001;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true); stopSelf(); return START_NOT_STICKY;
        }
        int  pct      = intent.getIntExtra(EXTRA_PROGRESS, 0);
        int  total    = intent.getIntExtra(EXTRA_TOTAL, 1);
        int  done     = intent.getIntExtra("done", 0);
        String fname  = intent.getStringExtra(EXTRA_FILENAME);
        if (fname == null) fname = "Converting...";
        startForeground(NID, buildNotif(pct, fname, total, done));
        return START_STICKY;
    }

    private Notification buildNotif(int pct, String fname, int total, int done) {
        createCh();
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra("open_tab", 1);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = total > 1 ? "Converting (" + done + "/" + total + ")" : "Converting";
        return new NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_convert)
            .setContentTitle(title)
            .setContentText(fname + "  " + pct + "%")
            .setProgress(100, pct, pct == 0)
            .setOngoing(true).setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build();
    }

    private void createCh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CH, "Conversion Progress",
                NotificationManager.IMPORTANCE_LOW);
            c.setSound(null, null);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
}
