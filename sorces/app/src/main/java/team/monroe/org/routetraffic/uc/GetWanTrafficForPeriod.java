package team.monroe.org.routetraffic.uc;


import android.util.Pair;

import org.monroe.team.android.box.Lists;
import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.manager.ServiceRegistry;
import org.monroe.team.android.box.uc.TransactionUserCase;

import java.util.Date;
import java.util.List;

import team.monroe.org.routetraffic.db.Dao;

public class GetWanTrafficForPeriod extends TransactionUserCase<Pair<Date,Date>,GetWanTrafficForPeriod.WanStat, Dao>{

    public GetWanTrafficForPeriod(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    protected WanStat transactionalExecute(Pair<Date, Date> request, Dao dao) {
        List<DAOSupport.Result> dayTrafficList = dao.getForPeriod(request.first, request.second);
        long out = -1, in = -1, outAvr = -1, inAvr = -1;
        for (DAOSupport.Result result : dayTrafficList) {
            out += result.get(2,Long.class);
            in += result.get(1,Long.class);
        }
        if (dayTrafficList.size() > 1){
            outAvr = (out - Lists.getLast(dayTrafficList).get(2, Long.class)) / dayTrafficList.size()-1;
            inAvr = (in - Lists.getLast(dayTrafficList).get(1, Long.class)) / dayTrafficList.size()-1;
        }
        return new WanStat(in,out, outAvr, inAvr);
    }

    public class WanStat {
        public final long in;
        public final long out;
        public final long avrIn;
        public final long avrOut;

        public WanStat(long in, long out, long avrIn, long avrOut) {
            this.in = in;
            this.out = out;
            this.avrIn = avrIn;
            this.avrOut = avrOut;
        }
    }


}
