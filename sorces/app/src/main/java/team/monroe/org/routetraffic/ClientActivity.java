package team.monroe.org.routetraffic;

import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.widget.TextView;

import org.monroe.team.android.box.support.ActivitySupport;
import org.monroe.team.socks.broadcast.DefaultBroadcastReceiver;
import org.monroe.team.socks.exception.ConnectionException;

import java.net.InetAddress;
import java.util.Map;


public class ClientActivity extends ActivitySupport<RouteTrafficApp> {

    private DefaultBroadcastReceiver receiver;
    private Handler uiHandler;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            receiver.start(12399);
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        receiver.shutdown();
    }

}
