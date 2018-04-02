package com.example.xyzreader.utility;

import android.text.format.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateHelper {

    private static SimpleDateFormat sDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private static SimpleDateFormat sOUTPUT_FORMAT = new SimpleDateFormat();

    // Most time functions can only handle 1902 - 2037
    private static GregorianCalendar sSTART_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static String getPublishedDate(String dateString){
        Date publishedDate;
        String result;
        try {
            publishedDate = sDATE_FORMAT.parse(dateString);
        }catch(ParseException pex){
            publishedDate = new Date();
        }

        if (!publishedDate.before(sSTART_OF_EPOCH.getTime())) {
            result = DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString();
        }else{
            result = sOUTPUT_FORMAT.format(publishedDate);
        }

        return result;
    }
}
