package team.monroe.org.routetraffic.service;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import team.monroe.org.routetraffic.uc.FetchStatistic;

public class HttpPageLoader {
    public String loadPage(String url, String user, String pass) throws ConnectivityException, AuthException, OtherException{

        BasicHttpParams httpParameters = new BasicHttpParams();
        String auth = null;
        if (user != null && user.trim().length() != 0) {
            auth = android.util.Base64.encodeToString((user+":"+pass)
                    .getBytes(), android.util.Base64.NO_WRAP);
        }

        HttpConnectionParams.setSoTimeout(httpParameters, 2000);

        HttpClient client = new DefaultHttpClient(httpParameters);
        String getURL = url;
        HttpGet get = new HttpGet(getURL);
        if (auth != null) {
            get.addHeader("Authorization", "Basic " + auth);
        }
        HttpResponse responseGet = null;
        try {
            responseGet = client.execute(get);
        } catch (IOException e) {
            throw new ConnectivityException(e);
        }
        if (responseGet.getStatusLine().getStatusCode() == 401){
            throw new AuthException();
        }
        if (responseGet.getStatusLine().getStatusCode() > 299) {
            throw new OtherException(responseGet.getStatusLine().getStatusCode());
        }

        HttpEntity resEntityGet = responseGet.getEntity();
        if (resEntityGet != null) {
            String s = null;
            try {
                s = EntityUtils.toString(resEntityGet);
                return s;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static class ConnectivityException extends Exception{
        public ConnectivityException(IOException e) {
            super(e);
        }
    }

    public static class AuthException extends Exception{};

    public static class OtherException extends Exception{
       private final int statusCode;

        public OtherException(int statusCode) {
            this.statusCode = statusCode;
        }
    };

}
