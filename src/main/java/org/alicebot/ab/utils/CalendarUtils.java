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


    public static String date(String format, String locale, String timezone)  {
        //HashSet<String> attributeNames = Utilities.stringSet("jformat","format","locale","timezone");
        Locale loc;
        log.info("CalendarUtils. date: Format = {} Locale = {} Timezone = {}",
            		format, locale, timezone);
        
        if (format == null) format = "dd/MM/yyyy";
        loc = Locale.forLanguageTag(Objects.requireNonNullElse(locale, "pl"));
            
        
        if (timezone == null) timezone = TimeZone.getDefault().getDisplayName();
        //log.info("Format = "+format+" Locale = "+locale+" Timezone = "+timezone);
        String dateAsString = new Date().toString();
        try {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(format,loc);
          dateAsString = simpleDateFormat.format(new Date());
        }
        catch (Exception ex) {
            log.info("CalendarUtils.date Bad date: Format = {} Locale = {} Timezone = {}",
            		format, locale, timezone);
        }
        log.info("CalendarUtils.date: {}", dateAsString);
        return dateAsString;
    }
        
}
