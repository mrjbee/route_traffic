package team.monroe.org.routetraffic.events;


import android.content.Intent;

import org.monroe.team.android.box.event.Event;

import team.monroe.org.routetraffic.FetchingDaemon;

public class DaemonStateChangedEvent extends Event<FetchingDaemon.State> {

    @Override
    protected FetchingDaemon.State extractValue(Intent intent) {
        return FetchingDaemon.State.valueOf(intent.getStringExtra("value_name"));
    }

    @Override
    protected void putValue(Intent intent, FetchingDaemon.State data) {
        intent.putExtra("value_name", data.name());
    }

    @Override
    public String getAction() {
       return "route.event.DaemonState";
    }
}
