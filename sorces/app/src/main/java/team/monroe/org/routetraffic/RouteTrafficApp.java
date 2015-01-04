package team.monroe.org.routetraffic;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.monroe.team.android.box.manager.Model;
import org.monroe.team.android.box.support.ApplicationSupport;

import team.monroe.org.routetraffic.uc.FetchStatistic;

public class RouteTrafficApp extends ApplicationSupport<RouteTrafficModel>{

    private FetchingService.FetchingServiceControl fetchingServiceControl;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            fetchingServiceControl = (FetchingService.FetchingServiceControl) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fetchingServiceControl = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected RouteTrafficModel createModel() {
        return new RouteTrafficModel(this);
    }

    public void startFetchService(){
        startService(new Intent(this, FetchingService.class));
        bindService(new Intent(this, FetchingService.class),connection, Service.BIND_AUTO_CREATE);
    }

    public boolean stopFetchService(){
        if (fetchingServiceControl == null)
            return false;
        fetchingServiceControl.doStop();
        return true;
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

    public static interface TrafficStatisticCallback {
        public void onDone();
        public void onError(String message);
    }
}
