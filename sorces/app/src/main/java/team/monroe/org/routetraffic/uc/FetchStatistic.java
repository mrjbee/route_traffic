package team.monroe.org.routetraffic.uc;

import android.util.Pair;

import org.monroe.team.android.box.db.DAOSupport;
import org.monroe.team.android.box.db.TransactionManager;
import org.monroe.team.android.box.manager.EventMessenger;
import org.monroe.team.android.box.manager.ServiceRegistry;
import org.monroe.team.android.box.manager.SettingManager;
import org.monroe.team.android.box.uc.UserCaseSupport;
import org.monroe.team.android.box.utils.DateUtils;

import java.util.Date;

import team.monroe.org.routetraffic.RouteTrafficModel;
import team.monroe.org.routetraffic.db.Dao;
import team.monroe.org.routetraffic.service.HttpPageLoader;
import team.monroe.org.routetraffic.service.PageParser;

public class FetchStatistic extends UserCaseSupport<Void,FetchStatistic.FetchingStatus>{

    public FetchStatistic(ServiceRegistry serviceRegistry) {
        super(serviceRegistry);
    }

    @Override
    public FetchingStatus execute(Void request) {

        try {

            String page = using(HttpPageLoader.class).loadPage(
                    using(SettingManager.class).get(RouteTrafficModel.ROUTER_URL)+"/userRpm/StatusRpm.htm",
                    using(SettingManager.class).get(RouteTrafficModel.ROUTER_USER),
                    using(SettingManager.class).get(RouteTrafficModel.ROUTER_PASS));
            if (page == null) return FetchingStatus.UNSUPPORTED_FORMAT;
            PageParser.Details data = using(PageParser.class).extractWanDetails(page);
            PageParser.Details details = postToDB(data);
            using(EventMessenger.class).send(RouteTrafficModel.EVENT_TODAY_STATISTIC_UPDATE,new Pair<Long, Long>(details.wanOut,details.wanIn));
            return FetchingStatus.SUCCESS;
        } catch (HttpPageLoader.ConnectivityException e) {
            return FetchingStatus.ERROR_NO_CONNECTION;
        } catch (HttpPageLoader.AuthException e) {
            return FetchingStatus.ERROR_AUTHORIZATION_FAILED;
        } catch (HttpPageLoader.OtherException e) {
            return FetchingStatus.UNSUPPORTED_FORMAT;
        } catch (PageParser.ParseException e) {
            return FetchingStatus.UNSUPPORTED_FORMAT;
        } catch (Exception e){
            e.printStackTrace();
            return FetchingStatus.UNKNOWN;
        }
    }

    private PageParser.Details postToDB(final PageParser.Details data) {
       return using(TransactionManager.class).execute(new TransactionManager.TransactionAction<PageParser.Details>() {
            @Override
            public PageParser.Details execute(DAOSupport daos) {
                if (data.wanOut == 0) return new PageParser.Details(0,0);
                long outBalance = using(SettingManager.class).get(RouteTrafficModel.COMPENSATION_BALANCE_OUT);
                long inBalance = using(SettingManager.class).get(RouteTrafficModel.COMPENSATION_BALANCE_IN);

                Dao dao = (Dao) daos;
                Date now = DateUtils.dateOnly(DateUtils.now());
                DAOSupport.Result result = dao.getForDate(now);

                long wanIn,wanOut;

                if (result == null){
                    dao.insertForDate(now,0,0);
                    wanIn = 0;
                    wanOut = 0;
                } else {
                    wanIn = result.get(1,Long.class);
                    wanOut = result.get(2,Long.class);
                }

                if (outBalance != -1 ){
                    //no balance at all
                    if (outBalance > data.wanOut){
                        //seems that router reset values
                        wanOut += data.wanOut;
                        wanIn += data.wanIn;
                    } else {
                        wanOut += (data.wanOut - outBalance);
                        wanIn += (data.wanIn - inBalance);
                    }
                    dao.updateForDate(now, wanIn,wanOut);
                }

                using(SettingManager.class).set(RouteTrafficModel.COMPENSATION_BALANCE_OUT,data.wanOut);
                using(SettingManager.class).set(RouteTrafficModel.COMPENSATION_BALANCE_IN,data.wanIn);

                return new PageParser.Details(wanOut, wanIn);
            }
        });
    }


    public static enum FetchingStatus{
        SUCCESS,
        ERROR_NO_CONNECTION,
        ERROR_AUTHORIZATION_FAILED,
        UNKNOWN, UNSUPPORTED_FORMAT
    }
}
