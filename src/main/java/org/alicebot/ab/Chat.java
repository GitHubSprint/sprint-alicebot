package org.alicebot.ab;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
/**
 * Class encapsulating a chat session between a bot and a client
 */
public class Chat {
    private static final Logger log = LoggerFactory.getLogger(Chat.class);        
    private final Date sessionCreated;    
    
    public Bot bot;
    public String sessionId = MagicStrings.unknown_session_id;
    public History<History> thatHistory= new History<>("that");
    public History<String> requestHistory= new History<>("request");
    public History<String> responseHistory= new History<>("response");
    public History<String> inputHistory= new History<>("input");
    public Predicates predicates = new Predicates();
    public static String matchTrace = "";
    public static boolean locationKnown = false;
    public static String longitude;
    public static String latitude;

    public String currentQuestion;
    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot    the bot to chat with
     */
    public Chat(Bot bot)  {
        this(bot, UUID.randomUUID().toString());
    }

    /**
     * Constructor
     * @param bot             bot to chat with
     * @param sessionId      unique session id      
     */
    public Chat(Bot bot, String sessionId) {
        this.sessionId = sessionId;
        this.bot = bot;
        this.sessionCreated = Calendar.getInstance().getTime();        
        History<String> contextThatHistory = new History<String>();
        contextThatHistory.add(MagicStrings.default_that);
        thatHistory.add(contextThatHistory);
        addPredicates();
        predicates.put("topic", MagicStrings.default_topic);
    }

    public Date getSessionCreated() 
    {
        return sessionCreated;
    }
    
    public String getBotName()
    {
        return bot.name;
    }
    
       
    /**
     * Load all predicate defaults
     */
    void addPredicates() {
        try {
            predicates.getPredicateDefaults(MagicStrings.config_path+"/predicates.txt") ;
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

    /**
     * Chat session terminal interaction
     */
    public void chat () {
        BufferedWriter bw = null;
        String logFile = MagicStrings.log_path+"/log_"+sessionId+".txt";
        try {
            //Construct the bw object
            bw = new BufferedWriter(new FileWriter(logFile, true)) ;
            String request="SET PREDICATES";
            String response = multisentenceRespond(request);
            while (!request.equals("quit")) {
                System.out.print("Human: ");
				request = IOUtils.readInputTextLine();
                response = multisentenceRespond(request);
                log.info(sessionId + " Robot: "+response);
                bw.write("Human: "+request);
                bw.newLine();
                bw.write("Robot: "+response);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Return bot response to a single sentence input given conversation context
     *
     * @param input         client input
     * @param that          bot's last sentence
     * @param topic         current topic
     * @param contextThatHistory         history of "that" values for this request/response interaction
     * @return              bot's reply
     */
    String respond(String input, String that, String topic, History contextThatHistory) {
        String response;
        inputHistory.add(input);
        response = AIMLProcessor.respond(input, that, topic, this);
        String normResponse = bot.preProcessor.normalize(response);

        String sentences[] = bot.preProcessor.sentenceSplit(normResponse);
        for (int i = 0; i < sentences.length; i++) {
          that = sentences[i];
          //log.info("That "+i+" '"+that+"'");
          if (that.trim().equals("")) that = MagicStrings.default_that;
          contextThatHistory.add(that);
        }
        return response.trim()+"  ";
    }

    /**
     * Return bot response given an input and a history of "that" for the current conversational interaction
     *
     * @param input       client input
     * @param contextThatHistory  history of "that" values for this request/response interaction
     * @return    bot's reply
     */
    String respond(String input, History<String> contextThatHistory) {
        History hist = thatHistory.get(0);
        String that;
        if (hist == null) that = MagicStrings.default_that;
        else that = hist.getString(0);
        return respond(input, that, predicates.get("topic"), contextThatHistory);
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param request      client's multiple-sentence input
     * @return
     */
    public String multisentenceRespond(String request) {
        StringBuilder response= new StringBuilder();
        matchTrace="";
        /*thatHistory.printHistory();
        inputHistory.printHistory();
        requestHistory.printHistory();
        responseHistory.printHistory();*/
        try {
        String norm = bot.preProcessor.normalize(request);

        log.info(sessionId + " multisentenceRespond request = " + request + " normalized = "+norm);
        String[] sentences = bot.preProcessor.sentenceSplit(norm);
        History<String> contextThatHistory = new History<>("contextThat");
        for (int i = 0; i < sentences.length; i++) {
            log.info(sessionId + " Human: "+sentences[i]);
            currentQuestion = sentences[i].toLowerCase();
            AIMLProcessor.trace_count = 0;
            String reply = respond(sentences[i], contextThatHistory);
            response.append("  ").append(reply);
            log.info(sessionId + " Robot: "+reply);
        }
        requestHistory.add(request);
        responseHistory.add(response.toString());
        thatHistory.add(contextThatHistory);
        //if (MagicBooleans.trace_mode)  log.info(matchTrace);
        } catch (Exception ex) {
            ex.printStackTrace();
            return MagicStrings.error_bot_response();
        }

        bot.writeLearnfIFCategories();
        return response.toString().trim();
    }


    public static void setMatchTrace(String newMatchTrace) {
		matchTrace = newMatchTrace;
	}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chat)) return false;

        Chat chat = (Chat) o;

        if (!getSessionCreated().equals(chat.getSessionCreated())) return false;
        return sessionId.equals(chat.sessionId);
    }

    @Override
    public int hashCode() {
        int result = getSessionCreated().hashCode();
        result = 31 * result + sessionId.hashCode();
        return result;
    }
}
