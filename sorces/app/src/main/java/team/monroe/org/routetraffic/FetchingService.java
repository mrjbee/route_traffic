package team.monroe.org.routetraffic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import org.monroe.team.android.box.Closure;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.manager.Model;

import java.util.Objects;

import team.monroe.org.routetraffic.uc.FetchStatistic;

public class FetchingService extends Service {

    public FetchingService() {}

    private FetchingThread fetchingThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
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
            return FetchingService.this.isRunning();
        }

        @Override
        public void start() {
            FetchingService.this.start();
        }

        @Override
        public void stop() {
            FetchingService.this.stop();
        }

        @Override
        public void shutdown() {
            FetchingService.this.shutdown();
        }

    }

    private void shutdown() {
        stop();
        stopSelf();
    }

    Object owner = new Object();
    private synchronized void start() {
        if (isRunning()) return;
        long out = -1, in = -1;
        updateNotification(out, in);
        fetchingThread = new FetchingThread();
        fetchingThread.start();
        Event.subscribeOnEvent(this,owner,RouteTrafficModel.TODAY_STATISTIC_UPDATE,new Closure<Pair<Long, Long>, Void>() {
            @Override
            public Void execute(Pair<Long, Long> arg) {
                updateNotification(arg.first, arg.second);
                return null;
            }
        });
    }

    private synchronized boolean isRunning() {
        return fetchingThread != null;
    }

    private synchronized void stop() {
        stopForeground(true);
        if (fetchingThread != null) {
            fetchingThread.interrupt();
            fetchingThread = null;
            Event.unSubscribeFromEvents(this,owner);
        }
    }

    private void doFetch() {
        FetchStatistic.FetchingStatus status = ((RouteTrafficApp) getApplication()).model().execute(FetchStatistic.class,null);
        if (status != FetchStatistic.FetchingStatus.SUCCESS){
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
        }
    }

    class FetchingThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted() && FetchingService.this.fetchingThread == this){
                FetchingService.this.doFetch();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                }
            }
        }
    }

}
