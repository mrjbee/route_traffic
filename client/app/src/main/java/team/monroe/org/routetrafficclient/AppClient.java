package team.monroe.org.routetrafficclient;

import android.app.AlarmManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;

import org.monroe.team.android.box.app.ApplicationSupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.UcDataProvider;
import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.utils.DateUtils;

import team.monroe.org.routetrafficclient.uc.GetTrafficDetails;

public class AppClient extends ApplicationSupport<ModelClient> implements SynchronizationService.SynchronizationResultObserver{

    public static final SettingManager.SettingItem<Boolean> SETTING_ACTIVATED = new SettingManager.SettingItem<>("activated",Boolean.class, false);

    public static final SettingManager.SettingItem<Long> SETTING_OUT = new SettingManager.SettingItem<>("out",Long.class, 0l);
    public static final SettingManager.SettingItem<Long> SETTING_IN = new SettingManager.SettingItem<>("in",Long.class, 0l);
    public static final SettingManager.SettingItem<Long> SETTING_LAST_SUCCESS_SYNC_DATE = new SettingManager.SettingItem<>("fetch_data",Long.class, -1l);
    public static final SettingManager.SettingItem<Long> SETTING_FIRST_SYNC_IN_SERIE = new SettingManager.SettingItem<>("last_sync_data",Long.class, -1l);

    private static AppClient instance;
    private Data<GetTrafficDetails.TrafficDetails> trafficDetailsDataProvider;
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
        trafficDetailsDataProvider = new UcDataProvider<GetTrafficDetails.TrafficDetails>(model(),this, GetTrafficDetails.TrafficDetails.class,GetTrafficDetails.class);
        activatedDataProvider = new Data<Boolean>(Boolean.class, model()) {
            @Override
            protected Boolean provideData() {
                return model().usingService(SettingManager.class).get(SETTING_ACTIVATED);
            }
        };
        super.onPostCreate();
    }

    public Data<GetTrafficDetails.TrafficDetails> data_traffic_details() {
        return trafficDetailsDataProvider;
    }

    public Data<Boolean> data_activated() {
        return activatedDataProvider;
    }

    public void updateActivationStatus(boolean activated) {
       boolean wasValue = model().usingService(SettingManager.class).get(SETTING_ACTIVATED);
       if (activated != wasValue){
           model().usingService(SettingManager.class).set(SETTING_ACTIVATED, activated);
           if (activated){
               model().usingService(SettingManager.class).set(SETTING_FIRST_SYNC_IN_SERIE,-1l);
               scheduleNextUpdate(time_ms_synchronization_interval_initial());
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
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(AlarmActor.START_FETCHING.createPendingIntent(this));
    }

    private void scheduleNextUpdate(long delay) {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                AlarmActor.START_FETCHING.createPendingIntent(this));
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
    public void onSyncResult(Object result) {
        if (model().usingService(SettingManager.class).get(SETTING_FIRST_SYNC_IN_SERIE) == -1) {
            model().usingService(SettingManager.class).set(SETTING_FIRST_SYNC_IN_SERIE, DateUtils.now().getTime());
        }
        data_traffic_details().invalidate();

        if (model().usingService(SettingManager.class).get(SETTING_ACTIVATED)) {
            scheduleNextUpdate(time_ms_synchronization_interval());
        }
    }

    public long time_ms_synchronization_time() {
        return 10000;
    }

    private long time_ms_synchronization_interval_initial() {
        return 2 * 1000;
    }

    private long time_ms_synchronization_interval() {
        return 5 * 1000;
    }

    public static long time_ms_worry() {
        return 30 * 1000;
    }
}
