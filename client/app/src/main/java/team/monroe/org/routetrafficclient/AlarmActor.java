package team.monroe.org.routetrafficclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.monroe.team.android.box.actor.Actor;
import org.monroe.team.android.box.actor.ActorAction;

public class AlarmActor extends Actor {

    public static final ActorAction START_FETCHING = new ActorAction("start_fetching",201,AlarmActor.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        reactOn(START_FETCHING, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().startSynchronizationDaemon();
            }
        });
    }

}
