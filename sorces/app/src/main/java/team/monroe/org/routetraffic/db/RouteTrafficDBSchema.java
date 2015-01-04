package team.monroe.org.routetraffic.db;

import org.monroe.team.android.box.db.Schema;

public class RouteTrafficDBSchema extends Schema {

    public RouteTrafficDBSchema() {
        super(1, "Route.db", WanTraffic.class);
    }

    public static class WanTraffic extends VersionTable{

        public final String TABLE_NAME = "wan_traffic";

        public final String _DATE = "stat_date";
        public final String _OUT = "wan_out";
        public final String _IN = "wan_in";

        public WanTraffic() {
            define(1, TABLE_NAME)
                    .column(_DATE, "INTEGER PRIMARY KEY")
                    .column(_IN, "INTEGER")
                    .column(_OUT,"INTEGER")
            ;
        }
    }

}
