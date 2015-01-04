package team.monroe.org.routetraffic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import org.monroe.team.android.box.Closure;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.manager.EventMessenger;
import org.monroe.team.android.box.manager.SettingManager;
import org.monroe.team.socks.broadcast.DefaultBroadcastAnnouncer;
import org.monroe.team.socks.exception.ConnectionException;
import org.monroe.team.socks.exception.InvalidProtocolException;
import org.monroe.team.socks.exception.SendFailException;

import java.util.HashMap;
import java.util.Map;

import team.monroe.org.routetraffic.uc.FetchStatistic;

public class FetchingDaemon extends Service {

    private DefaultBroadcastAnnouncer broadcastAnnouncer;

    public enum State{
        UNSPECIFIED,
        LAST_FAIL,
        LAST_SUCCESS
    }

    public FetchingDaemon() {}

    private FetchingThread fetchingThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (getSettingManager().get(RouteTrafficModel.DAEMON_ACTIVE)){
            start();
        }
        return START_NOT_STICKY;
    }

    private SettingManager getSettingManager() {
        return ((RouteTrafficApp)getApplication()).model().usingService(SettingManager.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void updateNotification(Long out, Long in) {
        String text = "Collecting data";

        if (out > 0 ){
            text = "Sent:"+((RouteTrafficApp)getApplication()).bytesToHuman(in, false)+", " +
                   "Received:"+((RouteTrafficApp)getApplication()).bytesToHuman(out, false);
        }

        Intent intent = new Intent(this, Dashboard.class);
        PendingIntent intent1 = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentText(text)
                .setContentTitle("Route Traffic")
                .setContentIntent(intent1)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.white_icon).build();


        startForeground(111, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FetchingServiceControlImpl();
    }

    public static interface FetchingServiceControl{
        public boolean isRunning();
        public void start();
        public void stop();
        public void shutdown();
    }

    class FetchingServiceControlImpl extends Binder implements FetchingServiceControl{

        @Override
        public boolean isRunning() {
            return FetchingDaemon.this.isRunning();
        }

        @Override
        public void start() {
            FetchingDaemon.this.start();
        }

        @Override
        public void stop() {
            FetchingDaemon.this.stop();
        }

        @Override
        public void shutdown() {
            FetchingDaemon.this.shutdown();
        }

    }

    private void shutdown() {
        stop();
        stopSelf();
    }

    private synchronized void start() {
        getSettingManager().set(RouteTrafficModel.DAEMON_ACTIVE, true);
        if (isRunning()) return;
        long out = -1, in = -1;
        updateNotification(out, in);
        fetchingThread = new FetchingThread();
        fetchingThread.start();
        Event.subscribeOnEvent(this, this, RouteTrafficModel.EVENT_TODAY_STATISTIC_UPDATE,new Closure<Pair<Long, Long>, Void>() {
            @Override
            public Void execute(final Pair<Long, Long> arg) {
                updateNotification(arg.first, arg.second);
                if (broadcastAnnouncer != null && broadcastAnnouncer.isAlive()){
                    new Thread(){
                        @Override
                        public void run() {
                            Map<String,String> msg = new HashMap<String, String>();
                            msg.put("app","route_traffic");
                            msg.put("out",Long.toString(arg.first));
                            msg.put("in",Long.toString(arg.second));
                            try {
                                broadcastAnnouncer.sendMessage(12399,msg);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }.start();


                }
                return null;
            }
        });

        try {
            broadcastAnnouncer = new DefaultBroadcastAnnouncer();
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized boolean isRunning() {
        return fetchingThread != null;
    }

    private synchronized void stop() {
        getSettingManager().set(RouteTrafficModel.DAEMON_ACTIVE, false);
        stopForeground(true);
        if (fetchingThread != null) {
            fetchingThread.interrupt();
            fetchingThread = null;
            Event.unSubscribeFromEvents(this,this);
        }
        if (broadcastAnnouncer != null){
            broadcastAnnouncer.destroy();
            broadcastAnnouncer = null;
        }
    }

    private void doFetch() {
        State newState = State.UNSPECIFIED;
        FetchStatistic.FetchingStatus status = ((RouteTrafficApp) getApplication()).model().execute(FetchStatistic.class,null);
        if (status != FetchStatistic.FetchingStatus.SUCCESS){
            newState = State.LAST_FAIL;
            String msg = ((RouteTrafficApp) getApplication()).fetchStatusToString(status);

            Intent intent = new Intent(this, Dashboard.class);
            PendingIntent intent1 = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentText(msg)
                    .setContentTitle("Fetch data error")
                    .setContentIntent(intent1)
                    .setSmallIcon(R.drawable.white_icon).build();

            ((RouteTrafficApp) getApplication()).model().usingService(NotificationManager.class).notify(112,notification);
        } else {
            newState = State.LAST_SUCCESS;
        }

        getSettingManager().set(RouteTrafficModel.DAEMON_STATE, newState.name());
        ((RouteTrafficApp)getApplication()).model()
                .usingService(EventMessenger.class).send(RouteTrafficModel.EVENT_DAEMON_LAST_STATE,newState);
    }

    class FetchingThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted() && FetchingDaemon.this.fetchingThread == this){
                FetchingDaemon.this.doFetch();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        }
    }

}
