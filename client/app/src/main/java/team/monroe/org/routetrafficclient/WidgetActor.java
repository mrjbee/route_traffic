package team.monroe.org.routetrafficclient;

import android.content.Context;
import android.content.Intent;

import org.monroe.team.android.box.actor.Actor;
import org.monroe.team.android.box.actor.ActorAction;

public class WidgetActor extends Actor {

    public static final ActorAction CHANGE_ACTIVATION_STATUS = new ActorAction("change_activation",501,WidgetActor.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        reactOn(CHANGE_ACTIVATION_STATUS, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().revertActivationStatus();
            }
        });
    }

}
