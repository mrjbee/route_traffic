package team.monroe.org.routetraffic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
        view(R.id.dash_daemon_enable_check, Button.class).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view(R.id.dash_daemon_enable_check, Button.class).setEnabled(false);
                application().fetchWanTraffic(new RouteTrafficApp.TrafficStatisticCallback() {
                    @Override
                    public void onDone() {
                        view(R.id.dash_daemon_enable_check, Button.class).setEnabled(true);
                    }

                    @Override
                    public void onError(String msg) {
                        Toast.makeText(Dashboard.this, msg, Toast.LENGTH_LONG).show();
                        view(R.id.dash_daemon_enable_check, Button.class).setEnabled(true);

                    }
                });
            }
        });

        view(R.id.dash_daemon_enable_check, CheckBox.class).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    try {
                        if (isChecked) {
                            application().startFetchService();
                        } else {
                            application().stopFetchService();
                        }
                    } catch (RouteTrafficApp.ServiceUnavailableException e) {
                        requestFetchServiceDetails();
                    }
            }
        });

        requestFetchServiceDetails();

        application().getWanLastMonthTraffic(new RouteTrafficApp.WanTrafficCallback() {
            @Override
            public void onDone(long out, long in,long aout, long ain) {
                view(R.id.dash_last_month_panel).setVisibility((out>0)?View.VISIBLE:View.GONE);
                view(R.id.dash_stat_last_month_avarege_value, TextView.class).setText(
                        application().toHumanBytes(out, false)+"/"+
                                application().toHumanBytes(in, false)
                                +" & "+
                        application().toHumanBytes(aout, false)+"/"+
                                application().toHumanBytes(ain, false)
                );
            }
        });
        FetchingDaemon.State state = application().getLastDaemonStatus();
        updateLastState(state);
    }

    private void updateLastState(FetchingDaemon.State state) {
        view(R.id.dash_daemon_status_value, TextView.class).setText(application().toHumanDaemonStatus(state));
    }

    private void requestFetchServiceDetails() {
        view(R.id.dash_daemon_panel).setVisibility(View.GONE);
        application().subscribeOnFetchServiceReady(new RouteTrafficApp.ServiceReadyObserver() {
            @Override
            public void onServiceReady() {
                try {
                    view(R.id.dash_daemon_enable_check, CheckBox.class).setChecked(application().isFetchServiceActivated());
                    view(R.id.dash_daemon_panel).setVisibility(View.VISIBLE);
                } catch (RouteTrafficApp.ServiceUnavailableException e) {
                    requestFetchServiceDetails();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
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
        Event.subscribeOnEvent(this, this, RouteTrafficModel.EVENT_TODAY_STATISTIC_UPDATE, new Closure<Pair<Long, Long>, Void>() {
            @Override
            public Void execute(Pair<Long, Long> arg) {
                view(R.id.dash_today_received_value, TextView.class).setText(application().toHumanBytes(arg.first, true));
                view(R.id.dash_today_sent_value, TextView.class).setText(application().toHumanBytes(arg.second, true));
                application().getWanMonthTraffic(new RouteTrafficApp.WanTrafficCallback() {
                    @Override
                    public void onDone(long out, long in,long aout, long ain) {
                        view(R.id.dash_month_recevied_value, TextView.class).setText(application().toHumanBytes(out, true));
                        view(R.id.dash_month_sent_value, TextView.class).setText(application().toHumanBytes(in, true));
                        view(R.id.dash_stat_month_avarege_value, TextView.class).setText(
                                application().toHumanBytes(aout, false)+"/"+
                                        application().toHumanBytes(ain, false)
                        );
                    }
                });
                return null;
            }
        });
        Event.subscribeOnEvent(this, this, RouteTrafficModel.EVENT_DAEMON_LAST_STATE, new Closure<FetchingDaemon.State, Void>() {

            @Override
            public Void execute(FetchingDaemon.State arg) {
                updateLastState(arg);
                return null;
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        Event.unSubscribeFromEvents(this,this);
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
