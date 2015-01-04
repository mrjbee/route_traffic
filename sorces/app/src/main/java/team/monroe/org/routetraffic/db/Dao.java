package team.monroe.org.routetraffic.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.monroe.team.android.box.Closure;
import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.Schema;

import java.util.Date;
import java.util.List;

public class Dao extends DAOSupport {
    public Dao(SQLiteDatabase db, Schema schema) {
        super(db, schema);
    }

    public Result getForDate(Date now) {
        final Cursor cursor = db.query(table(RouteTrafficDBSchema.WanTraffic.class).TABLE_NAME,
                strs(table(RouteTrafficDBSchema.WanTraffic.class)._DATE,
                        table(RouteTrafficDBSchema.WanTraffic.class)._IN,
                        table(RouteTrafficDBSchema.WanTraffic.class)._OUT),
                table(RouteTrafficDBSchema.WanTraffic.class)._DATE +" == ?",
                strs(now.getTime()),
                null,
                null,
                null);


        return bake(cursor, new Closure<Cursor, Result>() {
            @Override
            public Result execute(Cursor arg) {
                return new Result().with(arg.getLong(0),arg.getLong(1),arg.getLong(2));
            }
        });

    }

    public boolean insertForDate(Date now, int wanIn, int wanOut) {
        ContentValues appContentValue = new ContentValues(5);
        appContentValue.put(table(RouteTrafficDBSchema.WanTraffic.class)._DATE, now.getTime());
        appContentValue.put(table(RouteTrafficDBSchema.WanTraffic.class)._IN, wanIn);
        appContentValue.put(table(RouteTrafficDBSchema.WanTraffic.class)._OUT, wanOut);

        return -1 != db.insert(table(RouteTrafficDBSchema.WanTraffic.class).TABLE_NAME,
                null,
                appContentValue);
    }

    public boolean updateForDate(Date now, long wanIn, long wanOut) {

        ContentValues appContentValue = new ContentValues(5);
        appContentValue.put(table(RouteTrafficDBSchema.WanTraffic.class)._IN, wanIn);
        appContentValue.put(table(RouteTrafficDBSchema.WanTraffic.class)._OUT, wanOut);


        return  1 == db.update(
                table(RouteTrafficDBSchema.WanTraffic.class).TABLE_NAME,
                appContentValue,
                table(RouteTrafficDBSchema.WanTraffic.class)._DATE +" == ?",
                strs(now.getTime())
        );
    }

    public List<Result> getForPeriod(Date startDate, Date endDate) {
        final Cursor cursor = db.query(table(RouteTrafficDBSchema.WanTraffic.class).TABLE_NAME,
                strs(table(RouteTrafficDBSchema.WanTraffic.class)._DATE,
                        table(RouteTrafficDBSchema.WanTraffic.class)._IN,
                        table(RouteTrafficDBSchema.WanTraffic.class)._OUT),
                table(RouteTrafficDBSchema.WanTraffic.class)._DATE +" >= ? AND "+
                table(RouteTrafficDBSchema.WanTraffic.class)._DATE +" < ?",
                strs(startDate.getTime(),endDate.getTime()),
                null,
                null,
                null);


        return bakeMany(cursor, new Closure<Cursor, Result>() {
            @Override
            public Result execute(Cursor arg) {
                return new Result().with(arg.getLong(0),arg.getLong(1),arg.getLong(2));
            }
        });
    }
}
