package org.alicebot.ab;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is here to simulate a Contacts database for the purpose of testing contactaction.aiml
 */
public class Contact {
	private static final Logger log = LoggerFactory.getLogger(Contact.class);
    public static int contactCount=0;
    public static HashMap<String, Contact> idContactMap = new HashMap<String, Contact>();
    public static HashMap<String, String> nameIdMap = new HashMap<String, String>();
    public String contactId;
    public String displayName;
    public String birthday;
    public HashMap<String, String> phones;
    public HashMap<String, String> emails;
    public static String multipleIds(String contactName) {
        String patternString = " ("+contactName.toUpperCase()+") ";
        while (patternString.contains(" ")) patternString = patternString.replace(" ", "(.*)");
        //log.info("Pattern='"+patternString+"'");
        Pattern pattern = Pattern.compile(patternString);
        Set<String> keys = nameIdMap.keySet();
        StringBuilder result= new StringBuilder();
        int idCount = 0;
        for (String key : keys) {
            Matcher m = pattern.matcher(key);
            if (m.find()) {
                result.append(nameIdMap.get(key.toUpperCase())).append(" ");
                idCount++;
            }
        }
        if (idCount <= 1) result = new StringBuilder("false");
        return result.toString().trim();
    }
    public static String contactId(String contactName) {
        String patternString = " "+contactName.toUpperCase()+" ";
        while (patternString.contains(" ")) patternString = patternString.replace(" ", ".*");
        //log.info("Pattern='"+patternString+"'");
        Pattern pattern = Pattern.compile(patternString);
        Set<String> keys = nameIdMap.keySet();
        String result="unknown";
        for (String key: keys) {
            Matcher m = pattern.matcher(key);
            if (m.find()) result = nameIdMap.get(key.toUpperCase())+" ";
        }
        return result.trim();
    }
    public static String displayName(String id) {
        Contact c = idContactMap.get(id.toUpperCase());
        String result = "unknown";
        if (c != null) {
            result = c.displayName;
        }
        return result;
    }
    public static String dialNumber(String type, String id) {
        String result = "unknown";
        Contact c = idContactMap.get(id.toUpperCase());
        if (c != null) {
            String dialNumber = c.phones.get(type.toUpperCase());
            if (dialNumber != null) result = dialNumber;
        }
        return result;
    }
    public static String emailAddress(String type, String id) {
        String result = "unknown";
        Contact c = idContactMap.get(id.toUpperCase());
        if (c != null) {
            String emailAddress = c.emails.get(type.toUpperCase());
            if (emailAddress != null) result = emailAddress;
        }
        return result;
    }
    public static String birthday(String id) {
        Contact c = idContactMap.get(id.toUpperCase());
        if (c == null) return "unknown";
        else return c.birthday;
    }
    public Contact (String displayName, String phoneType, String dialNumber, String emailType, String emailAddress, String birthday) {
        contactId = "ID"+contactCount;
        contactCount++;
        phones = new HashMap<String, String>();
        emails = new HashMap<String, String>();
        idContactMap.put(contactId.toUpperCase(), this);
        addPhone(phoneType, dialNumber);
        addEmail(emailType, emailAddress);
        addName(displayName);
        addBirthday(birthday);
    }

    public void addPhone(String type, String dialNumber) {
        phones.put(type.toUpperCase(), dialNumber);
    }
    public void addEmail(String type, String emailAddress) {
        emails.put(type.toUpperCase(), emailAddress);
    }
    public void addName (String name) {
        displayName = name;
        nameIdMap.put(displayName.toUpperCase(), contactId);
        //log.info(nameIdMap.toString());
    }
    public void addBirthday(String birthday) {
        this.birthday = birthday;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.contactId != null ? this.contactId.hashCode() : 0);
        hash = 37 * hash + (this.displayName != null ? this.displayName.hashCode() : 0);
        hash = 37 * hash + (this.birthday != null ? this.birthday.hashCode() : 0);
        hash = 37 * hash + (this.phones != null ? this.phones.hashCode() : 0);
        hash = 37 * hash + (this.emails != null ? this.emails.hashCode() : 0);
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
        final Contact other = (Contact) obj;
        if ((this.contactId == null) ? (other.contactId != null) : !this.contactId.equals(other.contactId)) {
            return false;
        }
        if ((this.displayName == null) ? (other.displayName != null) : !this.displayName.equals(other.displayName)) {
            return false;
        }
        if ((this.birthday == null) ? (other.birthday != null) : !this.birthday.equals(other.birthday)) {
            return false;
        }
        if (this.phones != other.phones && (this.phones == null || !this.phones.equals(other.phones))) {
            return false;
        }
        if (this.emails != other.emails && (this.emails == null || !this.emails.equals(other.emails))) {
            return false;
        }
        return true;
    }
    
    


}
