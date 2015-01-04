package team.monroe.org.routetraffic.service;

import android.util.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser {

    Pattern pattern = Pattern.compile(".*statistList[ ]*\\=[ ]*new[ ]+Array[ ]*\\(([^)]*)\\);.*",Pattern.DOTALL);

    public Details extractWanDetails(String page) throws ParseException {
        Matcher m = pattern.matcher(page);
        if (m.find()) {
            try {
                String match = m.group(1);
                String[] matchedParsed = match.split(",");
                return new Details(Long.parseLong(matchedParsed[0].trim()),Long.parseLong(matchedParsed[1].trim()));
            }catch (Exception e){
                throw new ParseException(e);
            }
        }
        throw new ParseException(new IllegalStateException("Not found"));
    }

    public static class ParseException extends Exception{

        public ParseException(Exception e) {
            super(e);
        }
    }


    public static class Details{
        public final long wanOut;
        public final long wanIn;

        public Details(long wanOut, long wanIn) {
            this.wanOut = wanOut;
            this.wanIn = wanIn;
        }
    }
}
