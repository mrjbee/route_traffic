package team.monroe.org.routetrafficclient;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import org.monroe.team.android.box.actor.ActorAction;
import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.DataProvider;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.event.GenericEvent;
import org.monroe.team.corebox.utils.Closure;


/**
 * Implementation of App Widget functionality.
 */
public class TrafficWidget extends AppWidgetProvider {

    public static final ActorAction UPDATE_WIDGET = new ActorAction("update_widget", 909, TrafficWidget.class);

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        forEachWidget(context, appWidgetManager, appWidgetIds, null);
        fetchActivationStatus(context);
        fetchTraffic(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (UPDATE_WIDGET.isMe(intent)){
            fetchActivationStatus(context);
            fetchTraffic(context);
        }
    }

    private void fetchActivationStatus(final Context context) {
        AppClient.getInstance().data_activated().fetch(true,new Data.FetchObserver<Boolean>() {
            @Override
            public void onFetch(final Boolean activationValue) {
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                ComponentName componentName = new ComponentName(context.getPackageName(), TrafficWidget.class.getName());
                int[] widgetIds = manager.getAppWidgetIds(componentName);
                forEachWidget(context,manager,widgetIds, new Closure<RemoteViews, Void>() {
                    @Override
                    public Void execute(RemoteViews views) {
                        views.setImageViewResource(R.id.activation_check, activationValue ? R.drawable.activated_on : R.drawable.activated_off);
                        return null;
                    }
                });
            }
            @Override
            public void onError(Data.FetchError fetchError) {

            }
        });
    }


    private void fetchTraffic(final Context context) {
        AppClient.getInstance().data_traffic_details().fetch(true, new Data.FetchObserver<AppClient.TrafficDetails>() {
            @Override
            public void onFetch(final AppClient.TrafficDetails trafficDetails) {
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                ComponentName componentName = new ComponentName(context.getPackageName(), TrafficWidget.class.getName());
                int[] widgetIds = manager.getAppWidgetIds(componentName);
                forEachWidget(context,manager,widgetIds, new Closure<RemoteViews, Void>() {
                    @Override
                    public Void execute(RemoteViews views) {
                        String traffic = ClientDashboardActivity.toHumanBytes(trafficDetails.in)+"/"+ClientDashboardActivity.toHumanBytes(trafficDetails.out);
                        views.setTextViewText(R.id.traffic_value, traffic);
                        return null;
                    }
                });
            }

            @Override
            public void onError(Data.FetchError fetchError) {

            }
        });
    }
    @Override
    public void onEnabled(final Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    private void forEachWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Closure<RemoteViews,Void> widgetUpdate) {
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i],widgetUpdate);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Closure<RemoteViews,Void> widgetUpdate) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.traffic_widget);
        views.setOnClickPendingIntent(R.id.activation_check, WidgetActor.CHANGE_ACTIVATION_STATUS.createPendingIntent(context));
        views.setOnClickPendingIntent(R.id.application_icon, AppClient.gotoDashBoardActivity(context));
        views.setOnClickPendingIntent(R.id.synch_now_btn, WidgetActor.SYNC_NOW.createPendingIntent(context));

        //synch_now_btn
        if (widgetUpdate != null){
            widgetUpdate.execute(views);
        }
        //Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


