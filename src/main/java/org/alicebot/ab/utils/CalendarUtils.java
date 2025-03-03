package org.alicebot.ab.utils;

import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CalendarUtils {
	private static final Logger log = LoggerFactory.getLogger(CalendarUtils.class);

	public static int timeZoneOffset() {
		Calendar cal = Calendar.getInstance();
		int offset = (cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET))/(60*1000);
		return offset;
	}


	public static String year() {
        Calendar cal = Calendar.getInstance();
		return String.valueOf(cal.get(Calendar.YEAR));
	}

	
	public static String date() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM dd, yyyy");
        dateFormat.setCalendar(cal);
		return dateFormat.format(cal.getTime());
	}

    public static String date(String format, Locale locale, TimeZone timezone)  {
        log.info("dateLocale Format = {} Locale = {} Timezone = {}",
                format, locale, timezone);

        String dateAsString = new Date().toString();
        try {
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(format,locale);

            simpleDateFormat.setTimeZone(timezone);

            dateAsString = simpleDateFormat.format(new Date());
        }
        catch (Exception ex) {
            log.info("dateLocale Bad date: Format = {} Locale = {} Timezone = {}",
                    format, locale, timezone);
        }
        log.info("dateLocale: {}", dateAsString);
        return dateAsString;
    }

        
}
