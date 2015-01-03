package team.monroe.org.routetraffic.uc;

import org.monroe.team.android.box.manager.ServiceRegistry;
import org.monroe.team.android.box.uc.UserCaseSupport;

import team.monroe.org.routetraffic.FetchingService;

public class FetchStatistic extends UserCaseSupport<Void,FetchStatistic.FetchingStatus>{

    public FetchStatistic(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    public FetchingStatus execute(Void request) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
        return FetchingStatus.ERROR_NO_CONNECTION;
    }

    public static enum FetchingStatus{
        SUCCESS,
        ERROR_NO_CONNECTION,
        ERROR_AUTHORIZATION_FAILED,
        ERROR_NO_DATA
    }
}
