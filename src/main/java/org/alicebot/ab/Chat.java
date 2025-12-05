package org.alicebot.ab;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

import org.alicebot.ab.llm.LLMConfiguration;
import org.alicebot.ab.model.block.Block;
import org.alicebot.ab.model.block.Node;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.SprintUtils;
import org.json.JSONException;
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
    public String json;
    public String lastResponse;
    public int maxHistory = 0;
    public String channel;
    public Map<String, String> llmContext = new HashMap<>();
    public String symbol;

    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot    the bot to chat with
     */
    public Chat(Bot bot)  {
        this(bot, UUID.randomUUID().toString(), "CHAT", "sprint");
    }

    /**
     * Constructor
     * @param bot             bot to chat with
     * @param sessionId      unique session id      
     */
    public Chat(Bot bot, String sessionId, String channel, String symbol) {
        this.sessionId = sessionId;
        this.bot = bot;
        this.sessionCreated = Calendar.getInstance().getTime();
        this.channel = channel;
        this.symbol = symbol;
        History<String> contextThatHistory = new History<>();
        contextThatHistory.add(MagicStrings.default_that);
        thatHistory.add(contextThatHistory);
        addPredicates();
        predicates.put("topic", MagicStrings.default_topic);

        Block block = SprintUtils.getBlock(bot.name);

        int iMaxResponse = maxHistory;
        if(iMaxResponse == 0) {
            iMaxResponse = LLMConfiguration.gptMaxHistory;
        }

        if(block != null && !block.nodes().isEmpty()) {
            for(Node node : block.nodes()) {
                log.info("Chat Node: {}", node);
                try {
                    String nodeJson = AIMLProcessor.gptRequest(node.addparams(), sessionId, node.assistant(), node.system(), json, node.model(), null, iMaxResponse);
                    llmContext.put("gpt" + node.name(), nodeJson);
                } catch (JSONException e) {
                    log.error("Chat JSONException",e);
                }
            }

            log.info("{} Chat llmContexts: {}", sessionId, llmContext);
        }
    }



    public Date getSessionCreated() 
    {
        return sessionCreated;
    }
    
    public String getBotName() {
        return bot.name;
    }

    public String getTtsName() {
        return bot.properties.get("tts_name");
    }

    public String getAsrName() {
        return bot.properties.get("asr_name");
    }
       
    /**
     * Load all predicate defaults
     */
    void addPredicates() {
        try {
            predicates.getPredicateDefaults(MagicStrings.config_path+"/predicates.txt") ;
        } catch (Exception ex)  {
            log.error("addPredicates Error", ex);
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
                log.info(sessionId + " chat Robot: "+response);
                bw.write("Human: "+request);
                bw.newLine();
                bw.write("Robot: "+response);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception ex) {
            log.error("chat Error", ex);
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
    public String multisentenceRespond(String request, String gptJson, String lastResponse) {
        this.json = gptJson;
        this.lastResponse = lastResponse;
        return multisentenceRespond(request);
    }

    public void resetClassCache() {
        SprintUtils.resetClassCache();
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param text      client's multiple-sentence input
     * @return
     */
    private static String removePunctuation(String text) {
        return text
                .replaceAll("[?!.]", "")
                .replaceAll("[\\s]+", " ")
                .trim();
    }
    public String multisentenceRespond(String request) {
        StringBuilder response= new StringBuilder();
        matchTrace="";
        try {

            String configLocale = Objects.equals(bot.properties.get("max_input_length"), MagicStrings.unknown_property_value) ?
                    null : bot.properties.get("max_input_length");

            Boolean disableSentenceSplitting = !Objects.equals(bot.properties.get("disable_sentence_splitting"), MagicStrings.unknown_property_value)
                    && Boolean.parseBoolean(bot.properties.get("disable_sentence_splitting"));

            log.info("multisentenceRespond max_input_length: {} disable_sentence_splitting: {}", configLocale, disableSentenceSplitting);

            if(disableSentenceSplitting) {
                request = removePunctuation(request);
            }

            int maskInputLength = 0;
            if (configLocale != null) {
                 maskInputLength = Integer.parseInt(configLocale);
            }
            if (maskInputLength > 0 && request.length() > maskInputLength) {
                request = request.substring(0, maskInputLength);
                log.warn("multisentenceRespond Request length {} exceeds max_input_length {}. Truncating request. New request: {}",
                        request.length(), maskInputLength, request);
            }

            String norm = bot.preProcessor.normalize(request);

            log.info("{} multisentenceRespond request = {} normalized = {}", sessionId, request, norm);

            String[] sentences = bot.preProcessor.sentenceSplit(norm);
            History<String> contextThatHistory = new History<>("contextThat");
            for (String sentence : sentences) {
                log.info("{} Human: {}", sessionId, sentence);
                currentQuestion = sentence.toLowerCase();
                AIMLProcessor.trace_count = 0;
                String reply = respond(sentence, contextThatHistory);
                response.append("  ").append(reply);
                log.info("{} Robot: {}", sessionId, reply);
            }
            requestHistory.add(request);
            responseHistory.add(response.toString());
            thatHistory.add(contextThatHistory);
        } catch (Exception ex) {
            log.error("multisentenceRespond Error", ex);
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
