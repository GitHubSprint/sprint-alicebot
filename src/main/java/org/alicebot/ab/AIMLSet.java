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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implements AIML Sets
 */
public class AIMLSet extends HashSet<String> {
	private static final Logger log = LoggerFactory.getLogger(AIMLSet.class);
    public String setName;
    int maxLength = 1; // there are no empty sets
    String host; // for external sets
    String botid; // for external sets
    boolean isExternal = false;
    

    /**
     * constructor
     * @param name    name of set
     */
    public AIMLSet (String name) {
        super();
        this.setName = name.toLowerCase();
        if (setName.equals(MagicStrings.natural_number_set_name))  maxLength = 1;
    }
    public boolean contains(String s) {
        //if (isExternal)  log.info("External "+setName+" contains "+s+"?");
        //else  log.info("Internal "+setName+" contains "+s+"?");
//        if (isExternal && MagicBooleans.enable_external_sets) {
//            String[] split = s.split(" ");
//            if (split.length > maxLength) return false;
//            String query = MagicStrings.set_member_string+setName.toUpperCase()+" "+s;
//            String response = Sraix.sraix(null, query, "false", null, host, botid, null, "0");
//            log.info("External "+setName+" contains "+s+"? "+response);
//            if (response.equals("true")) return true;
//            else return false;
//        } else
        if (setName.equals(MagicStrings.natural_number_set_name)) {
            Pattern numberPattern = Pattern.compile("[0-9]+");
            Matcher numberMatcher = numberPattern.matcher(s);
            Boolean isanumber = numberMatcher.matches();
            //log.info("AIMLSet isanumber '"+s+"' "+isanumber);
            return isanumber;
        }
        else return super.contains(s);
    }
    public  void writeAIMLSet () {
        log.info("Writing AIML Set "+setName);
        try{
            // Create file
            FileWriter fstream = new FileWriter(MagicStrings.sets_path+"/"+setName+".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            for (String p : this) {

                out.write(p.trim());
                out.newLine();
            }
            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            log.error("Cannot write AIML set " + setName + ": " + e, e);
        }
    }
    public int readAIMLSetFromInputStream(InputStream in, Bot bot)  {
        
        String strLine;
        int cnt = 0;
        //Read File Line By Line
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, MagicStrings.UTF8));
            while ((strLine = br.readLine()) != null  && strLine.length() > 0)   {
                cnt++;
                //strLine = bot.preProcessor.normalize(strLine).toUpperCase();
                // assume the set is pre-normalized for faster loading
                if (strLine.startsWith("external")) {
                    String[] splitLine = strLine.split(":");
                    if (splitLine.length >= 4) {
                        host = splitLine[1];
                        botid = splitLine[2];
                        maxLength = Integer.parseInt(splitLine[3]);
                        isExternal = true;
                        log.info("Created external set at "+host+" "+botid);
                    }
                }
                else {
                    strLine = strLine.toUpperCase().trim();
                    String [] splitLine = strLine.split(" ");
                    int length = splitLine.length;
                    if (length > maxLength) maxLength = length;
                    //log.info("readAIMLSetFromInputStream "+strLine);
                    add(strLine.trim());
                }
                /*Category c = new Category(0, "ISA"+setName.toUpperCase()+" "+strLine.toUpperCase(), "*", "*", "true", MagicStrings.null_aiml_file);
                bot.brain.addCategory(c);*/
            }
        } catch (Exception ex) {
            log.error("readAIMLSetFromInputStream Error", ex);

        }
        return cnt;
    }

    public void readAIMLSet (Bot bot) {
        final String setPath = MagicStrings.sets_path+"/"+setName+".txt";
		log.info("Reading AIML Set " + setPath);
        try{
            // Open the file that is the first
            // command line parameter
            File file = new File(setPath);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(setPath);
                // Get the object
                readAIMLSetFromInputStream(fstream, bot);
                fstream.close();
            }
            else log.info(MagicStrings.sets_path+"/"+setName+".txt not found");
        }catch (Exception e){//Catch exception if any
            log.error("Cannot read AIML set '" + setPath + "': " + e, e);
        }

    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.setName != null ? this.setName.hashCode() : 0);
        hash = 97 * hash + this.maxLength;
        hash = 97 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 97 * hash + (this.botid != null ? this.botid.hashCode() : 0);
        hash = 97 * hash + (this.isExternal ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AIMLSet other = (AIMLSet) obj;
        if (this.maxLength != other.maxLength) {
            return false;
        }
        if (this.isExternal != other.isExternal) {
            return false;
        }
        if ((this.setName == null) ? (other.setName != null) : !this.setName.equals(other.setName)) {
            return false;
        }
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        if ((this.botid == null) ? (other.botid != null) : !this.botid.equals(other.botid)) {
            return false;
        }
        return true;
    }
    
    

}
