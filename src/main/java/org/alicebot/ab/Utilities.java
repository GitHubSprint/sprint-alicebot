package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import org.alicebot.ab.utils.CalendarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {
	private static final Logger log = LoggerFactory.getLogger(Utilities.class);
    /**
     * Excel sometimes adds mysterious formatting to CSV files.
     * This function tries to clean it up.
     *
     * @param line     line from AIMLIF file
     * @return   reformatted line
     */
    public static String fixCSV (String line) {
        while (line.endsWith(";")) line = line.substring(0, line.length()-1);
        if (line.startsWith("\"")) line = line.substring(1, line.length());
        if (line.endsWith("\"")) line = line.substring(0, line.length()-1);
        line = line.replaceAll("\"\"", "\"");
        return line;
    }
    public static String tagTrim(String xmlExpression, String tagName) {
        String stag = "<"+tagName+">";
        String etag = "</"+tagName+">";
        if (xmlExpression.length() >= (stag+etag).length()) {
            xmlExpression = xmlExpression.substring(stag.length());
            xmlExpression = xmlExpression.substring(0, xmlExpression.length()-etag.length());
        }
        return xmlExpression;
    }
    public static HashSet<String> stringSet(String... strings)  {
        return new HashSet<>(Arrays.asList(strings));
    }
    public static String getFileFromInputStream(InputStream in)  {
        
        String strLine;
        //Read File Line By Line
        StringBuilder contents = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            while ((strLine = br.readLine()) != null)   {
                if (strLine.isEmpty()) contents.append("\n");
                else contents.append(strLine).append("\n");
            }
        } catch (Exception ex) {
            log.error("getFileFromInputStream Error", ex);
        }
        return contents.toString().trim();
    }
    public static String getFile (String filename) {
        String contents = "";
        try {
            File file = new File(filename);
            if (file.exists()) {
                //log.info("Found file "+filename);
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                contents = getFileFromInputStream(fstream) ;
                fstream.close();
            }
        } catch (Exception e){//Catch exception if any
            log.error("Cannot get file '" + filename + "': " + e, e);
        }
        //log.info("getFile: "+contents);
        return contents;
    }
    public static String getCopyrightFromInputStream(InputStream in)  {
        
        String strLine;
        //Read File Line By Line
        StringBuilder copyright = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            while ((strLine = br.readLine()) != null)   {
                if (strLine.isEmpty()) copyright.append("\n");
                else copyright.append("<!-- ").append(strLine).append(" -->\n");
            }
        } catch (Exception ex) {
            log.error("getCopyrightFromInputStream Error", ex);
        }
        return copyright.toString();
    }
    public static String getCopyright (Bot bot, String AIMLFilename) {
        StringBuilder copyright = new StringBuilder();
        String year = CalendarUtils.year();
        String date = CalendarUtils.date();
        try {
            copyright = new StringBuilder(getFile(MagicStrings.config_path + "/copyright.txt"));
            String[] splitCopyright = copyright.toString().split("\n");
            copyright = new StringBuilder();
            for (String s : splitCopyright) {
                copyright.append("<!-- ").append(s).append(" -->\n");
            }
            copyright = new StringBuilder(copyright.toString().replace("[url]", bot.properties.get("url")));
            copyright = new StringBuilder(copyright.toString().replace("[date]", date));
            copyright = new StringBuilder(copyright.toString().replace("[YYYY]", year));
            copyright = new StringBuilder(copyright.toString().replace("[version]", bot.properties.get("version")));
            copyright = new StringBuilder(copyright.toString().replace("[botname]", bot.name.toUpperCase()));
            copyright = new StringBuilder(copyright.toString().replace("[filename]", AIMLFilename));
            copyright = new StringBuilder(copyright.toString().replace("[botmaster]", bot.properties.get("botmaster")));
            copyright = new StringBuilder(copyright.toString().replace("[organization]", bot.properties.get("organization")));
        } catch (Exception e){//Catch exception if any
            log.error("Cannot get copyright from '" + AIMLFilename + "': " + e, e);
        }
        //log.info("Copyright: "+copyright);
        return copyright.toString();
    }

    public static String getPannousAPIKey () {
       String apiKey = getFile(MagicStrings.config_path+"/pannous-apikey.txt");
       if (apiKey.equals("")) apiKey = MagicStrings.pannous_api_key;
       return apiKey;
    }
    public static String getPannousLogin () {
        String login = getFile(MagicStrings.config_path+"/pannous-login.txt");
        if (login.equals("")) login = MagicStrings.pannous_login;
        return login;
    }



}
