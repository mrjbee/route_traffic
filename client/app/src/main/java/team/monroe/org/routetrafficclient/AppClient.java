package team.monroe.org.routetrafficclient;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.utils.DateUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;


public class AppClient extends ApplicationSupport<ModelClient> implements SynchronizationService.SynchronizationResultObserver{

    public static final SettingManager.SettingItem<Boolean> SETTING_ACTIVATED = new SettingManager.SettingItem<>("activated",Boolean.class, false);

    public static final SettingManager.SettingItem<Long> SETTING_OUT = new SettingManager.SettingItem<>("out",Long.class, 0l);
    public static final SettingManager.SettingItem<Long> SETTING_IN = new SettingManager.SettingItem<>("in",Long.class, 0l);
    public static final SettingManager.SettingItem<Long> SETTING_LAST_SUCCESS_SYNC_DATE = new SettingManager.SettingItem<>("fetch_data",Long.class, -1l);
    public static final SettingManager.SettingItem<Long> SETTING_FIRST_SYNC_IN_SERIE = new SettingManager.SettingItem<>("last_sync_data",Long.class, -1l);

    private static AppClient instance;
    private Data<TrafficDetails> trafficDetailsDataProvider;
    private Data<Boolean> activatedDataProvider;
    private ServiceConnection serviceConnection;
    private SynchronizationService.SynchronizationDaemon daemon;

    public static AppClient getInstance() {
        return instance;
    }

    @Override
    protected ModelClient createModel() {
        return new ModelClient(this);
    }

    @Override
    protected void onPostCreate() {
        instance = this;
        trafficDetailsDataProvider = new Data<TrafficDetails>(TrafficDetails.class, model()) {
            @Override
            protected TrafficDetails provideData() {
                TrafficDetails answer = prepareTrafficDetails();
                if (answer.synchronizationState == TrafficDetails.SynchronizationState.FAIL){
                    suggestDeactivate();
                }else{
                    dismissDeactivationSuggestion();
                }
                return answer;
            }
        };

        activatedDataProvider = new Data<Boolean>(Boolean.class, model()) {
            @Override
            protected Boolean provideData() {
                return model().usingService(SettingManager.class).get(SETTING_ACTIVATED);
            }
        };

        super.onPostCreate();
        checkAndScheduleNextUpdate();


    }

    private void checkAndScheduleNextUpdate() {
        //Double check on create
        if (getSetting(SETTING_ACTIVATED) && !isNextUpdateAlarmSet()){
            scheduleNextUpdate(time_ms_synchronization_initial_delay());
        }
    }

    private void dismissDeactivationSuggestion() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Application.NOTIFICATION_SERVICE);
        notificationManager.cancel(201);
    }


    private void dismissActivationSuggestion() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Application.NOTIFICATION_SERVICE);
        notificationManager.cancel(202);
    }

    private TrafficDetails prepareTrafficDetails() {

        long in = model().usingService(SettingManager.class).get(AppClient.SETTING_IN);
        long out = model().usingService(SettingManager.class).get(AppClient.SETTING_OUT);
        long msLastSuccessSync = model().usingService(SettingManager.class).get(AppClient.SETTING_LAST_SUCCESS_SYNC_DATE);
        long msFirstSync = model().usingService(SettingManager.class).get(AppClient.SETTING_FIRST_SYNC_IN_SERIE);

        if (msFirstSync == -1){
            TrafficDetails.SynchronizationState state = TrafficDetails.SynchronizationState.AWAITING;
            if (!model().usingService(SettingManager.class).get(AppClient.SETTING_ACTIVATED)){
                state = TrafficDetails.SynchronizationState.DISABLED;
            }
            return new TrafficDetails(out, in, null, state);
        } else {

            long msToUse = msFirstSync;

            if (msLastSuccessSync != -1 && msLastSuccessSync > msFirstSync){
                msToUse = msLastSuccessSync;
            }

            long fetch_delay = DateUtils.now().getTime() - msToUse;
            long worry_time = AppClient.time_ms_worry();

            TrafficDetails.SynchronizationState state = TrafficDetails.SynchronizationState.SUCCESS;

            if (!model().usingService(SettingManager.class).get(AppClient.SETTING_ACTIVATED)){
                state = TrafficDetails.SynchronizationState.DISABLED;
            } else if (fetch_delay > worry_time){
                state = TrafficDetails.SynchronizationState.FAIL;
            }else if (msLastSuccessSync < msFirstSync){
                state = TrafficDetails.SynchronizationState.AWAITING;
            }
            return new TrafficDetails(
                    out, in,
                    (msLastSuccessSync != -1)? new Date(msLastSuccessSync):null,
                    state);
        }
    }

    private void suggestDeactivate() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Traffic synchronization")
                .setContentText("Synchronization failed")
                .setSmallIcon(R.drawable.syncronization)
                .setContentIntent(gotoDashBoardActivity(this))
                .addAction(R.drawable.stop, "Stop", NotificationActor.DEACTIVATE.createPendingIntent(this))
                .addAction(R.drawable.remind, "Stop & Remind", NotificationActor.DEACTIVATE_AND_REMIND.createPendingIntent(this))
                .setAutoCancel(true);

        Notification notification = builder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Application.NOTIFICATION_SERVICE);
        notificationManager.notify(201, notification);
    }

    public void suggestActivation() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Traffic synchronization")
                .setContentText("Synchronization is disabled")
                .setSubText("Do you want to enable synchronization ?")
                .setSmallIcon(R.drawable.syncronization)
                .setContentIntent(gotoDashBoardActivity(this))
                .addAction(R.drawable.start, "Start", NotificationActor.ACTIVATE.createPendingIntent(this))
                .addAction(R.drawable.remind, "Later", NotificationActor.REMIND_ACTIVATION.createPendingIntent(this))
                .setAutoCancel(true);

        Notification notification = builder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Application.NOTIFICATION_SERVICE);
        notificationManager.notify(202, notification);
    }

    public static PendingIntent gotoDashBoardActivity(Context context) {
        return PendingIntent.getActivity(
                context,
                0,
                new Intent(context, ClientDashboardActivity.class), 0);
    }

    public Data<TrafficDetails> data_traffic_details() {
        return trafficDetailsDataProvider;
    }

    public Data<Boolean> data_activated() {
        return activatedDataProvider;
    }

    public void updateActivationStatus(boolean activated) {
       boolean wasValue = model().usingService(SettingManager.class).get(SETTING_ACTIVATED);
       dismissDeactivationSuggestion();
       dismissActivationSuggestion();
       if (activated != wasValue){
           model().usingService(SettingManager.class).set(SETTING_ACTIVATED, activated);
           if (activated){
               model().usingService(SettingManager.class).set(SETTING_FIRST_SYNC_IN_SERIE,-1l);
               scheduleNextUpdate(time_ms_synchronization_initial_delay());
           }else {
               cancelNextUpdate();
               if (daemon != null){
                   daemon.deactivate();
               }
           }

           data_activated().invalidate();
           data_traffic_details().invalidate();
       }
    }


    private void cancelNextUpdate() {
        PendingIntent pendingIntent = AlarmActor.START_SYNCING.checkPendingIntent(this, Collections.EMPTY_LIST);
        if (pendingIntent == null) return;
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private void scheduleNextUpdate(long delay) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                AlarmActor.START_SYNCING.createPendingIntent(this));
    }

    private boolean isNextUpdateAlarmSet() {
        PendingIntent intent = AlarmActor.START_SYNCING.checkPendingIntent(this, Collections.EMPTY_LIST);
        return intent != null;
    }

    public void scheduleSuggestActivation(boolean longer) {
        dismissActivationSuggestion();
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ((longer)?time_ms_synchronization_activation()*2 : time_ms_synchronization_activation()),
                AlarmActor.ACTIVATION_SUGGESTION.createPendingIntent(this));
    }



    public void startSynchronizationDaemon() {

        if (serviceConnection != null && daemon != null && daemon.isActive()) return;

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AppClient.this.onDaemonStart((SynchronizationService.SynchronizationDaemon) service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceConnection = null;
            }
        };
        bindService(new Intent(this, SynchronizationService.class),serviceConnection, Service.BIND_AUTO_CREATE);
    }

    private void onDaemonStart(SynchronizationService.SynchronizationDaemon daemon) {
        if (!model().usingService(SettingManager.class).get(SETTING_ACTIVATED)){
            daemon.deactivate();
            return;
        }
        this.daemon = daemon;
        daemon.activate(this);
    }

    @Override
    public void onSyncResult(SynchronizationService.SynchronizationResult result) {

        model().usingService(SettingManager.class).set(SETTING_IN, result.in);
        model().usingService(SettingManager.class).set(SETTING_OUT, result.out);
        model().usingService(SettingManager.class).set(SETTING_LAST_SUCCESS_SYNC_DATE, DateUtils.now().getTime());
        data_traffic_details().invalidate();
    }

    @Override
    public void onSyncEnd() {
        if (model().usingService(SettingManager.class).get(SETTING_FIRST_SYNC_IN_SERIE) == -1) {
            model().usingService(SettingManager.class).set(SETTING_FIRST_SYNC_IN_SERIE, DateUtils.now().getTime());
        }

        if (model().usingService(SettingManager.class).get(SETTING_ACTIVATED)) {
            scheduleNextUpdate(time_ms_synchronization_delay());
        }

        data_traffic_details().invalidate();
    }

    //Time before suggest activation
    private long time_ms_synchronization_activation() {
        return DateUtils.msMinutes(30);
    }

    //Duration of synchronization
    public long time_ms_synchronization_duration() {
        return DateUtils.msMinutes(5);
    }

    //Interval before first synchronization
    private long time_ms_synchronization_initial_delay() {
        return DateUtils.msSeconds(2);
    }

    //Delay before next synchronization
    private long time_ms_synchronization_delay() {
        //each hour
        return DateUtils.msHour(1);
    }

    //Time after synchronization would fail
    public static long time_ms_worry() {
        return DateUtils.msHour(2);
    }

    public void onBootCompleted() {
        checkAndScheduleNextUpdate();
    }


    public static class TrafficDetails implements Serializable {

        public final long out;
        public final long in;
        public final Date synchronizationDate;
        public final SynchronizationState synchronizationState;

        public TrafficDetails(long out, long in, Date synchronizationDate, SynchronizationState synchronizationState) {
            this.out = out;
            this.in = in;
            this.synchronizationDate = synchronizationDate;
            this.synchronizationState = synchronizationState;
        }

        public static enum SynchronizationState {
            SUCCESS, FAIL, AWAITING, DISABLED
        }
    }
}
