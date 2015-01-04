package team.monroe.org.routetraffic;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.monroe.team.android.box.Closure;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.support.ActivitySupport;


public class Dashboard extends ActivitySupport<RouteTrafficApp> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        view(R.id.dash_option_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        view(R.id.dash_test_connection, Button.class).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view(R.id.dash_test_connection, Button.class).setEnabled(false);
                application().fetchWanTraffic(new RouteTrafficApp.TrafficStatisticCallback() {
                    @Override
                    public void onDone() {
                        view(R.id.dash_test_connection, Button.class).setEnabled(true);
                    }

                    @Override
                    public void onError(String msg) {
                        Toast.makeText(Dashboard.this, msg, Toast.LENGTH_LONG).show();
                        view(R.id.dash_test_connection, Button.class).setEnabled(true);

                    }
                });
            }
        });
        view(R.id.dash_daemon_enable, CheckBox.class).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    application().startFetchService();
                } else {
                    if (!application().stopFetchService()) {
                        buttonView.setChecked(true);
                    }
                }
            }
        });
        Event.subscribeOnEvent(this, this, RouteTrafficModel.TODAY_STATISTIC_UPDATE, new Closure<Pair<Long, Long>, Void>() {
            @Override
            public Void execute(Pair<Long, Long> arg) {
                view(R.id.dash_today_received_value, TextView.class).setText(application().bytesToHuman(arg.first));
                view(R.id.dash_today_sent_value, TextView.class).setText(application().bytesToHuman(arg.second));
                return null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        Event.unSubscribeFromEvents(this,this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        application().fetchWanTraffic(new RouteTrafficApp.TrafficStatisticCallback() {
            @Override
            public void onDone() {}
            @Override
            public void onError(String message) {}
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                openSettings();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
