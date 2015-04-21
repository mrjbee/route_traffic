package team.monroe.org.routetrafficclient;

import android.content.Context;
import android.content.Intent;

import org.monroe.team.android.box.actor.Actor;
import org.monroe.team.android.box.actor.ActorAction;

public class AlarmActor extends Actor {

    public static final ActorAction START_SYNCING = new ActorAction("start_fetching",201,AlarmActor.class);
    public static final ActorAction ACTIVATION_SUGGESTION = new ActorAction("suggest_activation",202,AlarmActor.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        reactOn(START_SYNCING, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().startSynchronizationDaemon();
            }
        });
        reactOn(ACTIVATION_SUGGESTION, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().suggestActivation();
            }
        });
    }

}
