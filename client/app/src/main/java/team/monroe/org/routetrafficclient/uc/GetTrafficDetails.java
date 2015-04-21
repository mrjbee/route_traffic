package team.monroe.org.routetrafficclient.uc;

import org.monroe.team.android.box.services.SettingManager;
import org.monroe.team.corebox.services.ServiceRegistry;
import org.monroe.team.corebox.uc.UserCaseSupport;
import org.monroe.team.corebox.utils.DateUtils;

import java.io.Serializable;
import java.util.Date;

import team.monroe.org.routetrafficclient.AppClient;

public class GetTrafficDetails extends UserCaseSupport<Void, GetTrafficDetails.TrafficDetails> {

    public GetTrafficDetails(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected TrafficDetails executeImpl(Void request) {

        long in = using(SettingManager.class).get(AppClient.SETTING_IN);
        long out = using(SettingManager.class).get(AppClient.SETTING_OUT);
        long msLastSuccessSync = using(SettingManager.class).get(AppClient.SETTING_LAST_SUCCESS_SYNC_DATE);
        long msFirstSync = using(SettingManager.class).get(AppClient.SETTING_FIRST_SYNC_IN_SERIE);

        if (msFirstSync == -1){
            TrafficDetails.SynchronizationState state = TrafficDetails.SynchronizationState.AWAITING;
            if (!using(SettingManager.class).get(AppClient.SETTING_ACTIVATED)){
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
            if (!using(SettingManager.class).get(AppClient.SETTING_ACTIVATED)){
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
