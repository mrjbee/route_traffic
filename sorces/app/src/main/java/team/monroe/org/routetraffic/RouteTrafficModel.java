package team.monroe.org.routetraffic;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.monroe.team.android.box.db.DAOFactory;
import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.DBHelper;
import org.monroe.team.android.box.db.TransactionManager;
import org.monroe.team.android.box.event.Event;
import org.monroe.team.android.box.manager.Model;
import org.monroe.team.android.box.manager.ServiceRegistry;
import org.monroe.team.android.box.manager.SettingManager;

import team.monroe.org.routetraffic.db.Dao;
import team.monroe.org.routetraffic.db.RouteTrafficDBSchema;
import team.monroe.org.routetraffic.events.DaemonStateChangedEvent;
import team.monroe.org.routetraffic.events.TodayStatisticChangeEvent;
import team.monroe.org.routetraffic.service.HttpPageLoader;
import team.monroe.org.routetraffic.service.PageParser;

public class RouteTrafficModel extends Model {


    public static final SettingManager.SettingItem<String> DAEMON_STATE = new SettingManager.SettingItem<>("DAEMON_STATE", String.class, FetchingDaemon.State.UNSPECIFIED.name());
    public static final SettingManager.SettingItem<Boolean> DAEMON_ACTIVE = new SettingManager.SettingItem<>("DAEMON_ACTIVE",Boolean.class,false);
    public static final SettingManager.SettingItem<Long> COMPENSATION_BALANCE_OUT = new SettingManager.SettingItem<>("COMPENSATION_BALANCE_OUT",Long.class,-1l);
    public static final SettingManager.SettingItem<Long> COMPENSATION_BALANCE_IN = new SettingManager.SettingItem<>("COMPENSATION_BALANCE_IN",Long.class,-1l);

    public static final SettingManager.SettingItem<String> ROUTER_URL = new SettingManager.SettingItem<>("ROUTER_URL",String.class,"http://192.168.0.1");
    public static final SettingManager.SettingItem<String> ROUTER_USER = new SettingManager.SettingItem<>("ROUTER_USER",String.class,"admin");
    public static final SettingManager.SettingItem<String> ROUTER_PASS = new SettingManager.SettingItem<>("ROUTER_PASS",String.class,"admin");


    public static final TodayStatisticChangeEvent EVENT_TODAY_STATISTIC_UPDATE = new TodayStatisticChangeEvent();
    public static final DaemonStateChangedEvent EVENT_DAEMON_LAST_STATE = new DaemonStateChangedEvent();

    public RouteTrafficModel(Context context) {
        super("route_traffic", context);
    }

    @Override
    protected void constructor(String appName, Context context, ServiceRegistry serviceRegistry) {
        super.constructor(appName, context, serviceRegistry);
        serviceRegistry.registrate(HttpPageLoader.class, new HttpPageLoader());
        serviceRegistry.registrate(PageParser.class, new PageParser());
        final RouteTrafficDBSchema schema = new RouteTrafficDBSchema();
        serviceRegistry.registrate(TransactionManager.class, new TransactionManager(new DBHelper(context, schema),
                new DAOFactory() {
                    @Override
                    public DAOSupport createInstanceFor(SQLiteDatabase database) {
                        return new Dao(database,schema);
                    }
                }));
    }

    @Override
    protected SharedPreferences getPreferencesForSettings(String appName, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
