package team.monroe.org.routetrafficclient;

import org.monroe.team.android.box.app.ApplicationSupport;

public class AppClient extends ApplicationSupport<ModelClient>{
    @Override
    protected ModelClient createModel() {
        return new ModelClient(this);
    }
}
