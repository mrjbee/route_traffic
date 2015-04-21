package team.monroe.org.routetrafficclient;

import android.content.Context;
import android.content.Intent;

import org.monroe.team.android.box.actor.Actor;
import org.monroe.team.android.box.actor.ActorAction;

public class NotificationActor extends Actor {

    public static final ActorAction DEACTIVATE = new ActorAction("deactivate",301,NotificationActor.class);
    public static final ActorAction DEACTIVATE_AND_REMIND = new ActorAction("deactivate_and_notify",302,NotificationActor.class);
    public static final ActorAction ACTIVATE = new ActorAction("activate",303,NotificationActor.class);
    public static final ActorAction REMIND_ACTIVATION = new ActorAction("remind_activate",304,NotificationActor.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        reactOn(DEACTIVATE, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().updateActivationStatus(false);
            }
        });
        reactOn(DEACTIVATE_AND_REMIND, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().updateActivationStatus(false);
                AppClient.getInstance().scheduleSuggestActivation();
            }
        });
        reactOn(ACTIVATE, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().updateActivationStatus(true);
            }
        });
        reactOn(REMIND_ACTIVATION, intent, new SilentReaction() {
            @Override
            protected void reactSilent(Intent intent) {
                AppClient.getInstance().scheduleSuggestActivation();
            }
        });
    }

}
