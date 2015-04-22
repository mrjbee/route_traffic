package team.monroe.org.routetrafficclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.monroe.team.socks.broadcast.BroadcastReceiver;
import org.monroe.team.socks.broadcast.DefaultBroadcastReceiver;
import org.monroe.team.socks.exception.ConnectionException;

import java.net.InetAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SynchronizationService extends Service {

    private Timer timer;
    private SynchronizationResultObserver resultObserver;
    private DefaultBroadcastReceiver broadcastReceiver;

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
        public void onSyncResult(SynchronizationResult result);
        public void onSyncEnd();
    }

    private void startListener() {
        if (timer != null) throw new IllegalStateException();

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.syncronization)
                .setContentTitle("Traffic synchronization")
                .setContentText("Gathering traffic data")
                .setSubText("This will take up to " + (app().time_ms_synchronization_duration() / 1000) + " sec")
                .setOngoing(true)
                .setContentIntent(AppClient.gotoDashBoardActivity(this));
        startForeground(105, builder.build());
        broadcastReceiver = new DefaultBroadcastReceiver(new BroadcastReceiver.BroadcastMessageObserver<Map<String, String>>() {
            @Override
            public void onMessage(Map<String, String> stringStringMap, InetAddress inetAddress) {
                if (stringStringMap.containsKey("out")){
                    long out = Long.parseLong(stringStringMap.get("out"));
                    long in = Long.parseLong(stringStringMap.get("in"));
                    if (out > 0){
                        resultObserver.onSyncResult(new SynchronizationResult(in, out));
                        stopListener();
                    }
                }
            }
        });

        try {
            broadcastReceiver.start(12399);
        } catch (ConnectionException e) {
            //TODO: Think how to handle
            throw new IllegalStateException(e);
        }

        this.timer = new Timer("daemon_shutdown",true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopListener();
            }
        }, app().time_ms_synchronization_duration());

    }

    private AppClient app() {
        return ((AppClient)getApplication());
    }

    private void stopListener() {
        if (!isStarted()) return;
        timer.cancel();
        timer.purge();
        timer = null;
        broadcastReceiver.shutdown();
        stopForeground(true);
        resultObserver.onSyncEnd();
        resultObserver = null;
    }

    private boolean isStarted() {
        return timer != null;
    }

    public static class SynchronizationResult{

        public final long out;
        public final long in;

        public SynchronizationResult(long out, long in) {
            this.out = out;
            this.in = in;
        }
    }
}
