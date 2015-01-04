package team.monroe.org.routetraffic.events;


import android.content.Intent;
import android.util.Pair;

import org.monroe.team.android.box.event.Event;

public class TodayStatisticChangeEvent extends Event<Pair<Long,Long>> {
    @Override
    protected Pair<Long, Long> extractValue(Intent intent) {
        return new Pair<>(
                intent.getLongExtra("out",0),
                intent.getLongExtra("in",0));
    }

    @Override
    protected void putValue(Intent intent, Pair<Long, Long> data) {
        intent.putExtra("out",data.first);
        intent.putExtra("in",data.second);
    }

    @Override
    public String getAction() {
        return "route.event.Today";
    }
}
