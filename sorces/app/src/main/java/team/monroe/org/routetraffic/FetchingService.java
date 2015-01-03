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

import team.monroe.org.routetraffic.uc.FetchStatistic;

public class FetchingService extends Service {

    public FetchingService() {}

    private FetchingThread fetchingThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doActualStartIfRequired();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private synchronized void doActualStartIfRequired() {
        if (fetchingThread != null) return;

        Intent intent = new Intent(this, Dashboard.class);
        PendingIntent intent1 = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this)
                .setContentText("Collecting data")
                .setContentTitle("Route Traffic")
                .setContentIntent(intent1)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.white_icon).build();

        startForeground(111,notification);

        fetchingThread = new FetchingThread();
        fetchingThread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FetchingServiceControlImpl();
    }

    public static interface FetchingServiceControl{
        public void doStop();
    }

    class FetchingServiceControlImpl extends Binder implements FetchingServiceControl{
        @Override
        public void doStop() {
            FetchingService.this.stop();

        }
    }

    private synchronized void stop() {
        stopForeground(true);
        fetchingThread.interrupt();
        fetchingThread= null;
        stopSelf();
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
