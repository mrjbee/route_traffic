package team.monroe.org.routetrafficclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class SynchronizationService extends Service {

    private Timer timer;
    private SynchronizationResultObserver resultObserver;

    public SynchronizationService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return new SynchronizationDaemon();
    }

    public class SynchronizationDaemon extends Binder {

        public void activate(SynchronizationResultObserver resultObserver){
            SynchronizationService.this.resultObserver = resultObserver;
            SynchronizationService.this.startListener();
        }

        public boolean isActive(){
            return SynchronizationService.this.isStarted();
        }
        public void deactivate(){
            SynchronizationService.this.stopListener();
        }

    }

    public static interface SynchronizationResultObserver{
        public void onSyncResult(Object result);
    }

    private void startListener() {
        if (timer != null) throw new IllegalStateException();
        this.timer = new Timer("daemon_shutdown",true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                resultObserver.onSyncResult(null);
                stopListener();
            }
        }, app().time_ms_synchronization_time());

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.syncronization)
                .setContentTitle("Traffic synchronization")
                .setContentText("Gathering traffic data")
                .setSubText("This will take up to " + (app().time_ms_synchronization_time() / 1000) + " sec")
                .setOngoing(true)
                .setContentIntent(AppClient.gotoDashBoardActivity(this));
        startForeground(105, builder.build());
    }

    private AppClient app() {
        return ((AppClient)getApplication());
    }

    private void stopListener() {
        if (!isStarted()) return;
        timer.cancel();
        timer.purge();
        timer = null;
        stopForeground(true);
        resultObserver = null;
    }

    private boolean isStarted() {
        return timer != null;
    }

}
