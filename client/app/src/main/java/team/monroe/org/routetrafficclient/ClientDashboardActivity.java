package team.monroe.org.routetrafficclient;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import org.monroe.team.android.box.app.ActivitySupport;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.socks.broadcast.DefaultBroadcastReceiver;

import java.text.DateFormat;
import java.text.NumberFormat;

import team.monroe.org.routetrafficclient.uc.GetTrafficDetails;


public class ClientDashboardActivity extends ActivitySupport<AppClient> {

    private DefaultBroadcastReceiver receiver;
    private static NumberFormat byteCountFormatter = NumberFormat.getInstance();
    private static DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private Data.DataChangeObserver<GetTrafficDetails.TrafficDetails> trafficChangeListener = new Data.DataChangeObserver<GetTrafficDetails.TrafficDetails>() {
        @Override
        public void onDataInvalid() {
            ClientDashboardActivity.this.onInvalidTrafficDetails();
        }

        @Override
        public void onData(GetTrafficDetails.TrafficDetails trafficDetails) {}
    };

    private Data.DataChangeObserver<Boolean> activatedChangeListener= new Data.DataChangeObserver<Boolean>() {
        @Override
        public void onDataInvalid() {
            ClientDashboardActivity.this.onInvalidActivationStatus();
        }

        @Override
        public void onData(Boolean aBoolean) {}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        byteCountFormatter.setMaximumFractionDigits(2);
        byteCountFormatter.setMinimumFractionDigits(2);
        setContentView(R.layout.activity_client_dashboard);
        view_check(R.id.activation_check).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                application().updateActivationStatus(isChecked);
            }
        });
    }

    private void updateTrafficDetails(GetTrafficDetails.TrafficDetails trafficDetails) {
        view_text(R.id.out_value).setText(toHumanBytes(trafficDetails.out));
        view_text(R.id.in_value).setText(toHumanBytes(trafficDetails.in));
        if (trafficDetails.synchronizationDate == null){
            view(R.id.in_time).setVisibility(View.INVISIBLE);
            view(R.id.in_time_value).setVisibility(View.INVISIBLE);
        }else {
            view(R.id.in_time).setVisibility(View.VISIBLE);
            view(R.id.in_time_value).setVisibility(View.VISIBLE);
            view_text(R.id.in_time_value).setText(dateFormat.format(trafficDetails.synchronizationDate));
        }
        view_text(R.id.in_status_value).setText(asHumanString(trafficDetails.synchronizationState));
    }

    private String asHumanString(GetTrafficDetails.TrafficDetails.SynchronizationState synchronizationState) {
        switch (synchronizationState){
            case AWAITING:
                return "In Progress";
            case SUCCESS:
                return "Success";
            case FAIL:
                return "Fail";
            case DISABLED:
                return "Disabled";
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        application().data_traffic_details().addDataChangeObserver(trafficChangeListener);
        fetchTrafficDetails();
        application().data_activated().addDataChangeObserver(activatedChangeListener);
        fetchActivationStatus();
    }

    private void fetchActivationStatus() {
        application().data_activated().fetch(true,new Data.FetchObserver<Boolean>() {
            @Override
            public void onFetch(Boolean activated) {
                view_check(R.id.activation_check).setChecked(activated);
            }

            @Override
            public void onError(Data.FetchError fetchError) {
                forceCloseWithErrorCode(203);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        application().data_traffic_details().removeDataChangeObserver(trafficChangeListener);
        application().data_activated().removeDataChangeObserver(activatedChangeListener);
    }

    public void onInvalidTrafficDetails() {
        fetchTrafficDetails();
    }

    private void fetchTrafficDetails() {
        application().data_traffic_details().fetch(true, new Data.FetchObserver<GetTrafficDetails.TrafficDetails>() {
            @Override
            public void onFetch(GetTrafficDetails.TrafficDetails trafficDetails) {
                updateTrafficDetails(trafficDetails);
            }

            @Override
            public void onError(Data.FetchError fetchError) {
                forceCloseWithErrorCode(202);
            }
        });
    }

    private void onInvalidActivationStatus() {
        fetchActivationStatus();
    }

    /*
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        uiHandler = new android.os.Handler(Looper.getMainLooper());
        receiver = new DefaultBroadcastReceiver(new org.monroe.team.socks.broadcast.BroadcastReceiver.BroadcastMessageObserver<Map<String, String>>() {
            @Override
            public void onMessage(final Map<String, String> stringStringMap, InetAddress inetAddress) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        view(R.id.client_daemon_status, TextView.class).setText(stringStringMap.get("status"));
                        view(R.id.client_today_recived_value, TextView.class).setText(application().toHumanBytes(
                                Long.parseLong(stringStringMap.get("out")), true));
                        view(R.id.client_today_sent_value, TextView.class).setText(application().toHumanBytes(
                                Long.parseLong(stringStringMap.get("in")), true));
                    }
                });
              }
        });

     */

    /*

    public String toHumanBytes(Long bytes, boolean extended) {
        if (bytes < 0) return "NaN";

        StringBuilder builder = new StringBuilder();

        double gB =  bytes/1073741824d;
        double mB =  (bytes)/1048576d;
        double kB =  (bytes)/1024;

        NumberFormat formatToUse = extended?byteCountFormatter:byteCountFormatterShort;

        if (gB > 1 && !extended){
            builder.append(byteCountFormatter.format(gB)).append(" GB ");
        }else if (mB > 1){
            builder.append(formatToUse.format(mB)).append(" MB ");
        }else {
            builder.append(formatToUse.format(kB)).append(" KB ");
        }
        return builder.toString().trim();
    }
     */
    public String toHumanBytes(Long bytes) {
        StringBuilder builder = new StringBuilder();

        double gB =  bytes/1073741824d;
        double mB =  (bytes)/1048576d;
        double kB =  (bytes)/1024;

        NumberFormat formatToUse = byteCountFormatter;

        if (gB > 1) {
            builder.append(formatToUse.format(gB)).append(" GB ");
        }else if (mB > 1){
            builder.append(formatToUse.format(mB)).append(" MB ");
        }else {
            builder.append(formatToUse.format(kB)).append(" KB ");
        }
        return builder.toString().trim();
    }


}
