package team.monroe.org.routetraffic;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Pair;

import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.manager.Model;
import org.monroe.team.android.box.manager.SettingManager;
import org.monroe.team.android.box.support.ApplicationSupport;
import org.monroe.team.android.box.utils.DateUtils;

import java.util.Date;

import team.monroe.org.routetraffic.uc.FetchStatistic;
import team.monroe.org.routetraffic.uc.GetWanTrafficForPeriod;

public class RouteTrafficApp extends ApplicationSupport<RouteTrafficModel>{

    private FetchingDaemon.FetchingServiceControl fetchingServiceControl;
    private ServiceReadyObserver serviceObserver;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            fetchingServiceControl = (FetchingDaemon.FetchingServiceControl) service;
            if (serviceObserver != null) serviceObserver.onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fetchingServiceControl = null;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        initFetchService();
    }

    @Override
    protected RouteTrafficModel createModel() {
        return new RouteTrafficModel(this);
    }

    public void initFetchService(){
        startService(new Intent(this, FetchingDaemon.class));
        bindService(new Intent(this, FetchingDaemon.class),connection, Service.BIND_AUTO_CREATE);
    }

    public synchronized void subscribeOnFetchServiceReady(ServiceReadyObserver observer){
        this.serviceObserver = observer;
        if (fetchingServiceControl != null){
            observer.onServiceReady();
        } else {
            initFetchService();
        }
    }

    public boolean isFetchServiceActivated() throws ServiceUnavailableException {
        checkServiceAvailability();
        return  fetchingServiceControl.isRunning();
    }

    public void startFetchService() throws ServiceUnavailableException {
        checkServiceAvailability();
        fetchingServiceControl.start();
    }

    public void stopFetchService() throws ServiceUnavailableException {
        checkServiceAvailability();
        fetchingServiceControl.stop();
    }

    private void checkServiceAvailability() throws ServiceUnavailableException {
        if (fetchingServiceControl == null)
            throw new ServiceUnavailableException();
    }


    public void fetchWanTraffic(final TrafficStatisticCallback callback) {
        model().execute(FetchStatistic.class, null, new Model.BackgroundResultCallback<FetchStatistic.FetchingStatus>() {
            @Override
            public void onResult(FetchStatistic.FetchingStatus response) {
                if (response == FetchStatistic.FetchingStatus.SUCCESS) {
                    callback.onDone();
                }else {
                    callback.onError(fetchStatusToString(response));
                }

            }

            @Override
            public void onFails(Throwable e) {
                callback.onError("Unexpected error");
            }
        });
    }


    public String bytesToHuman(Long bytes, boolean extended) {
        if (bytes < 0) return "NaN";
        StringBuilder builder = new StringBuilder();
        long gB =  bytes/1073741824l;
        long mB =  (bytes - gB * 1073741824l)/1048576l;
        long kB =  (bytes - gB * 1073741824l - mB*1048576l)/1024;

        if (gB > 0){
            builder.append(gB).append(" GB ");
        }
        if (mB > 0){
            builder.append(mB).append(" MB ");
        }

        if ((gB == 0 && extended) || mB < 1){
                builder.append(kB).append(" KB ");
        }
        return builder.toString().trim();
    }



    public String fetchStatusToString(FetchStatistic.FetchingStatus status) {
        switch (status) {
            case SUCCESS:
                return "Successful";
            case ERROR_NO_CONNECTION:
                return "No connection. Please check your settings";
            case UNSUPPORTED_FORMAT:
                return "Could`n get data. Seems you got unsupported router";
            case ERROR_AUTHORIZATION_FAILED:
                return "Invalid credential. Please check your settings";
            case UNKNOWN:
                return "Something goes bad";
        }
        return null;
    }

    public void getWanMonthTraffic(final WanTrafficCallback trafficCallback) {
         model().execute(GetWanTrafficForPeriod.class, new Pair<Date, Date>(
                 DateUtils.monthOnly(DateUtils.now()),
                 DateUtils.mathMonth(DateUtils.monthOnly(DateUtils.now()),+1)),
             new Model.BackgroundResultCallback<GetWanTrafficForPeriod.WanStat>() {
                 @Override
                 public void onResult(GetWanTrafficForPeriod.WanStat stat) {
                    trafficCallback.onDone(stat.out, stat.in,stat.avrOut, stat.avrIn);
                 }
         });
    }

    public void getWanLastMonthTraffic(final WanTrafficCallback trafficCallback) {
        model().execute(GetWanTrafficForPeriod.class, new Pair<Date, Date>(
                        DateUtils.mathMonth(DateUtils.monthOnly(DateUtils.now()),-1),
                        DateUtils.monthOnly(DateUtils.now())),
                new Model.BackgroundResultCallback<GetWanTrafficForPeriod.WanStat>() {
                    @Override
                    public void onResult(GetWanTrafficForPeriod.WanStat stat) {
                        trafficCallback.onDone(stat.out, stat.in,stat.avrOut, stat.avrIn);
                    }
                });
    }

    public FetchingDaemon.State getLastDaemonStatus() {
        return FetchingDaemon.State.valueOf(model().usingService(SettingManager.class).get(RouteTrafficModel.DAEMON_STATE));
    }

    public String convertDaemonStatus(FetchingDaemon.State lastDaemonStatus) {
        switch (lastDaemonStatus){
            case UNSPECIFIED: return "NaN";
            case LAST_FAIL: return "Fail";
        }
        return "Successful";
    }

    public static interface WanTrafficCallback {
        public void onDone(long out, long in, long aout, long ain);
    }

    public static interface TrafficStatisticCallback {
        public void onDone();
        public void onError(String message);
    }

    public interface ServiceReadyObserver{
        public void onServiceReady();
    }

    public static class ServiceUnavailableException extends Exception{}
}
