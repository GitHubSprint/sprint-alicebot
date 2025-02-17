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


    public static void main(String[] args) {
        System.out.println(date("HH:mm", "pl","Europe/Warsaw"));
        System.out.println(date("HH:mm", "pl","America/Panama"));
    }

    public static String date(String format, String locale, String timezone)  {

        log.info("start date: Format = {} Locale = {} Timezone = {}",
            		format, locale, timezone);
        
        if (format == null) format = "dd/MM/yyyy";
        Locale loc = Locale.forLanguageTag(Objects.requireNonNullElse(locale, "pl"));
        TimeZone tz = TimeZone.getTimeZone(Objects.requireNonNullElse(timezone, TimeZone.getDefault().getID()));
        
        if (timezone == null) timezone = TimeZone.getDefault().getDisplayName();

        log.info("created date: Format = {} Locale = {} Timezone = {}",
                format, locale, timezone);

        String dateAsString = new Date().toString();
        try {
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(format,loc);

            simpleDateFormat.setTimeZone(tz);

            dateAsString = simpleDateFormat.format(new Date());
        }
        catch (Exception ex) {
            log.info("date Bad date: Format = {} Locale = {} Timezone = {}",
            		format, locale, timezone);
        }
        log.info("CalendarUtils.date: {}", dateAsString);
        return dateAsString;
    }
        
}
