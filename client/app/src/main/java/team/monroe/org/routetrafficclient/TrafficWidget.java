package team.monroe.org.routetrafficclient;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.monroe.team.android.box.data.Data;
import org.monroe.team.android.box.data.DataProvider;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.corebox.utils.Closure;


/**
 * Implementation of App Widget functionality.
 */
public class TrafficWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        forEachWidget(context, appWidgetManager, appWidgetIds, null);
        fetchActivationStatus(context);
        fetchTraffic(context);
    }



    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(DataProvider.INVALID_DATA.getAction())){
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
    public void onEnabled(Context context) {
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
        if (widgetUpdate != null){
            widgetUpdate.execute(views);
        }
        //Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


