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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alicebot.ab.db.SprintBotDbUtils;
import org.alicebot.ab.llm.GenAIHelper;
import org.alicebot.ab.llm.LLMConfiguration;
import org.alicebot.ab.llm.LLMService;
import org.alicebot.ab.db.Report;
import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.DomUtils;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.SprintUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pl.sprint.sprintvalidator.Validator;
import pl.sprint.sprintvalidator.utils.PeselValidator;

/**
 * The core AIML parser and interpreter.
 * Implements the AIML 2.0 specification as described in
 * AIML 2.0 Working Draft document
 * https://docs.google.com/document/d/1wNT25hJRyupcG51aO89UcQEiG-HkXRXusukADpFnDs4/pub
 */
public class AIMLProcessor {
	
    private static final Logger log = LoggerFactory.getLogger(AIMLProcessor.class);        

    /**
     * when parsing an AIML file, process a category element.
     *
     */
    public static AIMLProcessorExtension extension;
    private static void categoryProcessor(Node n, ArrayList<Category> categories, String topic, String aimlFile, String language) {
        String pattern, that, template;

        NodeList children = n.getChildNodes();
        pattern = "*"; that = "*";  template="";
        for (int j = 0; j < children.getLength(); j++) {
            //log.info("CHILD: "+children.item(j).getNodeName());
            Node m = children.item(j);
            String mName = m.getNodeName();

            switch (mName) {
                case "#text": /*skip*/
                    break;
                case "pattern":
                    pattern = DomUtils.nodeToString(m);
                    break;
                case "that":
                    that = DomUtils.nodeToString(m);
                    break;
                case "topic":
                    topic = DomUtils.nodeToString(m);
                    break;
                case "template":
                    template = DomUtils.nodeToString(m);
                    break;
                default:
                    log.info("categoryProcessor: unexpected {}", mName);
                    break;
            }
        }

        pattern = trimTag(pattern, "pattern");
        that = trimTag(that, "that");
        topic = trimTag(topic, "topic");
        template = trimTag(template, "template");
        
        Category c = new Category(0, pattern, that, topic, template, aimlFile);
        categories.add(c);
    }
    public static String trimTag(String s, String tagName) {
        String stag = "<"+tagName+">";
        String etag = "</"+tagName+">";
        if (s.startsWith(stag) && s.endsWith(etag)) {
            s = s.substring(stag.length());
            s = s.substring(0, s.length()-etag.length());
        }
        return s.trim();
    }
    /**
     * convert an AIML file to a list of categories.
     *
     * @param directory     directory containing the AIML file.
     * @param aimlFile      AIML file name.
     * @return              list of categories.
     */
    public static ArrayList<Category> AIMLToCategories (String directory, String aimlFile) {
        try {
            ArrayList<Category> categories = new ArrayList<>();
            Node root = DomUtils.parseFile(directory+"/"+aimlFile);      // <aiml> tag
            String language = MagicStrings.default_language;
            if (root.hasAttributes()) {
                NamedNodeMap XMLAttributes = root.getAttributes();
                for(int i=0; i < XMLAttributes.getLength(); i++)

                {
                    if (XMLAttributes.item(i).getNodeName().equals("language")) language = XMLAttributes.item(i).getNodeValue();
                }
            }
            NodeList nodelist = root.getChildNodes();
            for (int i = 0; i < nodelist.getLength(); i++)   {
                Node n = nodelist.item(i);
                //log.info("AIML child: "+n.getNodeName());
                if (n.getNodeName().equals("category")) {
                    categoryProcessor(n, categories, "*", aimlFile, language);
                }
                else if (n.getNodeName().equals("topic")) {
                    String topic = n.getAttributes().getNamedItem("name").getTextContent();
                    NodeList children = n.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node m = children.item(j);
                        //log.info("Topic child: "+m.getNodeName());
                        if (m.getNodeName().equals("category")) {
                            categoryProcessor(m, categories, topic, aimlFile, language);
                        }
                    }
                }
            }
            return categories;
        }
        catch (Exception ex) {
            log.error("AIMLToCategories ERROR",ex);
            return null;
        }
    }

    public static int sraiCount = 0;
    public static int repeatCount = 0;
    // Helper functions:
    public static int checkForRepeat(String input, Chat chatSession) {
        if (input.equals(chatSession.inputHistory.get(1))) {
            return 1;
        }
        else return 0;
    }

    /**
     * generate a bot response to a single sentence input.
     *
     * @param input      the input sentence.
     * @param that       the bot's last sentence.
     * @param topic      current topic.
     * @param chatSession     current client session.
     * @return              bot's response.
     */
    public static String respond(String input, String that, String topic, Chat chatSession) {
        return respond(input, that, topic, chatSession, 0);
    }

    /**
     * generate a bot response to a single sentence input.
     *
     * @param input      input statement.
     * @param that       bot's last reply.
     * @param topic      current topic.
     * @param chatSession   current client chat session.
     * @param srCnt         number of <srai> activations.
     * @return              bot's reply.
     */
 public static String respond(String input, String that, String topic, Chat chatSession, int srCnt) {
        String response;
        if (input == null || input.isEmpty()) input = MagicStrings.null_input;
        sraiCount = srCnt;
        response = MagicStrings.default_bot_response();
        
         try {
            Nodemapper leaf = chatSession.bot.brain.match(input, that, topic);
            if (leaf == null) {return(response);}
            //log.info("Template="+leaf.category.getTemplate());
            ParseState ps = new ParseState(0, chatSession, input, that, topic, leaf);
            //chatSession.matchTrace += leaf.category.getTemplate()+"\n";
            response = evalTemplate(leaf.category.getTemplate(), ps);
            //log.info("That="+that);
        } catch (Exception ex) {
             log.error("respond Error", ex);
        }
        return response;
    }

    /** capitalizeString:
     * from http://stackoverflow.com/questions/1892765/capitalize-first-char-of-each-word-in-a-string-java
     *
     * @param string   the string to capitalize
     * @return  the capitalized string
     */

    private static String capitalizeString(String string) {
        char[] chars = string.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i])) {
                found = false;
            }
        }
        return String.valueOf(chars);
    }

    /**
     * explode a string into individual characters separated by one space
     *
     * @param input             input string
     * @return                  exploded string
     */
    private static String explode(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) result.append(" ").append(input.charAt(i));
        return result.toString().trim();
    }

    // Parsing and evaluation functions:

    /**
     * evaluate the contents of an AIML tag.
     * calls recursEval on child tags.
     *
     * @param node        the current parse node.
     * @param ps          the current parse state.
     * @param ignoreAttributes   tag names to ignore when evaluating the tag.
     * @return            the result of evaluating the tag contents.
     */
    public static String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes) {
        StringBuilder result = new StringBuilder();
        try {
        NodeList childList = node.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node child = childList.item(i);
            if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName()))
            {
                String temp = recursEval(child, ps);                
                result.append(temp);
            }
              
        }
        } catch (Exception ex) {
            log.error("Something went wrong with evalTagContent", ex);
        }
        return result.toString();
    }

    /**
     * pass thru generic XML (non-AIML tags, such as HTML) as unevaluated XML
     *
     * @param node       current parse node
     * @param ps         current parse state
     * @return           unevaluated generic XML string
     */
    public static String genericXML(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        return unevaluatedXML(result, node, ps);
    }

    /**
     * return a string of unevaluated XML.      When the AIML parser
     * encounters an unrecognized XML tag, it simply passes through the
     * tag in XML form.  For example, if the response contains HTML
     * markup, the HTML is passed to the requesting process.    However if that
     * markup contains AIML tags, those tags are evaluated and the parser
     * builds the result.
     *
     * @param result         the tag contents.
     * @param node           current parse node.
     * @param ps             current parse state.
     * @return               the unevaluated XML string
     */
    private static String unevaluatedXML(String result, Node node, ParseState ps) {
        String nodeName = node.getNodeName();
        StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            NamedNodeMap XMLAttributes = node.getAttributes();
            for(int i=0; i < XMLAttributes.getLength(); i++)

            {
                attributes.append(" ").append(XMLAttributes.item(i).getNodeName()).append("=\"").append(XMLAttributes.item(i).getNodeValue()).append("\"");
            }
        }
        if (result.equals(""))
            return "<"+nodeName+attributes+"/>";
        else return "<"+nodeName+attributes+">"+result+"</"+nodeName+">";
    }
    public static int trace_count = 0;

    /**
     * implements AIML <srai> tag
     *
     * @param node       current parse node.
     * @param ps         current parse state.
     * @return           the result of processing the <srai>
     *
     */
    private static String srai(Node node, ParseState ps) {
        sraiCount++;
        if (sraiCount > MagicNumbers.max_recursion) 
        {            
            return MagicStrings.too_much_recursion();
        }
        String response = MagicStrings.default_bot_response();
        try {
            String result = evalTagContent(node, ps, null);
            result = result.trim();
            result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
            result = ps.chatSession.bot.preProcessor.normalize(result);
            String topic = ps.chatSession.predicates.get("topic");     // the that stays the same, but the topic may have changed
            if (MagicBooleans.trace_mode) {
                log.info(trace_count+". <srai>"+result+"</srai> from "+ps.leaf.category.inputThatTopic()+" topic="+topic+") ");
                trace_count++;
            }
            Nodemapper leaf = ps.chatSession.bot.brain.match(result, ps.that, topic);
            if (leaf == null) {return(response);}
            //log.info("Srai returned "+leaf.category.inputThatTopic()+":"+leaf.category.getTemplate());
            response = evalTemplate(leaf.category.getTemplate(), new ParseState(ps.depth+1, ps.chatSession, ps.input, ps.that, topic, leaf));
            //log.info("That="+that);
        } catch (Exception ex) {
            log.error("srai Error", ex);
        }
        return response.trim();

    }

    /**
     * in AIML 2.0, an attribute value can be specified by either an XML attribute value
     * or a subtag of the same name.  This function tries to read the value from the XML attribute first,
     * then tries to look for the subtag.
     *
     * @param node       current parse node.
     * @param ps         current parse state.
     * @param attributeName   the name of the attribute.
     * @return             the attribute value.
     */
    // value can be specified by either attribute or tag
    private static String getAttributeOrTagValue (Node node, ParseState ps, String attributeName) {        // AIML 2.0
        //log.info("getAttributeOrTagValue "+attributeName);
        String result = "";
        Node m = node.getAttributes().getNamedItem(attributeName);
        if (m == null) {
            NodeList childList = node.getChildNodes();
            result = null;         // no attribute or tag named attributeName
            for (int i = 0; i < childList.getLength(); i++)   {
                Node child = childList.item(i);
                //log.info("getAttributeOrTagValue child = "+child.getNodeName());
                if (child.getNodeName().equals(attributeName)) {
                    result = evalTagContent(child, ps, null);
                    //log.info("getAttributeOrTagValue result from child = "+result);
                }
            }
        }
        else {
            result = m.getNodeValue();
        }
        //log.info("getAttributeOrTagValue "+attributeName+" = "+result);
        return result;
    }

    /**
     * access external web service for response
     * implements <sraix></sraix>
     * and its attribute variations.
     *
     * @param node   current XML parse node
     * @param ps     AIML parse state
     * @return       response from remote service or string indicating failure.
     */
//    private static String sraix(Node node, ParseState ps) {
//        HashSet<String> attributeNames = Utilities.stringSet("botid", "host");
//        String host = getAttributeOrTagValue(node, ps, "host");
//        String botid = getAttributeOrTagValue(node, ps, "botid");
//        String hint = getAttributeOrTagValue(node, ps, "hint");
//        String limit = getAttributeOrTagValue(node, ps, "limit");
//        String defaultResponse = getAttributeOrTagValue(node, ps, "default");
//        String result = evalTagContent(node, ps, attributeNames);
//
//        return Sraix.sraix(ps.chatSession, result, defaultResponse, hint, host, botid, null, limit);
//
//    }

    /**
     * map an element of one string set to an element of another
     * Implements <map name="mapname"></map>   and <map><name>mapname</name></map>
     *
     * @param node       current XML parse node
     * @param ps         current AIML parse state
     * @return           the map result or a string indicating the key was not found
     */
    private static String map(Node node, ParseState ps) {
        String result = MagicStrings.unknown_map_value;
        HashSet<String> attributeNames = Utilities.stringSet("name");
        String mapName = getAttributeOrTagValue(node, ps, "name");
        String contents = evalTagContent(node, ps, attributeNames);
        if (mapName == null) result = "<map>"+contents+"</map>"; // this is an OOB map tag (no attribute)
        else {
            AIMLMap map = ps.chatSession.bot.mapMap.get(mapName);
            if (map != null) result = map.get(contents.toUpperCase());
            //log.info("AIMLProcessor map "+contents+" "+result);
            if (result == null) result = MagicStrings.unknown_map_value;
            result = result.trim();
        }
        return result;
    }

    /**
     * set the value of an AIML predicate.
     * Implements <set name="predicate"></set> and <set var="varname"></set>
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the result of the <set> operation
     */
    private static String set(Node node, ParseState ps) {                    // add pronoun check
        HashSet<String> attributeNames = Utilities.stringSet("name", "var");
        String predicateName = getAttributeOrTagValue(node, ps, "name");
        String varName = getAttributeOrTagValue(node, ps, "var");
        String value = evalTagContent(node, ps, attributeNames).trim();
        value = value.replaceAll("(\r\n|\n\r|\r|\n)", " ");
        if (predicateName != null) ps.chatSession.predicates.put(predicateName, value);
        if (varName != null) ps.vars.put(varName, value);
        return value;
    }
    
    //////////////////////start sprint custom methods
    
    /**
     * gets all variables
     * @param node  current XML parse node
     * @param ps    AIML parse state
     * @return      the result of the <getall> operation
     */

    private static String getall(Node node, ParseState ps) {
        String predicateName = getAttributeOrTagValue(node, ps, "name");
        Map<String, String> predicates = ps.chatSession.predicates;

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : predicates.entrySet()) {
            String key = entry.getKey();
            if (predicateName == null || predicateName.isEmpty() || key.contains(predicateName)) {
                result.append(key)
                        .append(" = ")
                        .append(entry.getValue())
                        .append("<br />");
            }
        }
        return result.toString();
    }
    
    private static String checkEmpty(String in) {
        if(in == null || in.isEmpty())
            return MagicStrings.unknown_property_value;
        return in;
    }
    
    /**
     * FastText ML integration
     * implements <ml model="tak-nie-model" nbest="2" threshold="0" score="50" parameter="hello"/>
     * @param node current XML parse node
     * @param ps AIML parse state
     * @return
     */
    private static String ml(Node node, ParseState ps) {

        String model = getAttributeOrTagValue(node, ps, "model");
        String nBest = getAttributeOrTagValue(node, ps, "nbest");
        String threshold = getAttributeOrTagValue(node, ps, "threshold");
        String score = getAttributeOrTagValue(node, ps, "score");
        String parameter = getAttributeOrTagValue(node, ps, "parameter");
        log.info("{} ML currentQuestion: {}", ps.chatSession.sessionId, ps.chatSession.currentQuestion);
        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        log.info("{} ML parameter: {} input: {}", ps.chatSession.sessionId, parameter, input);

        if(input == null || input.isEmpty() || input.equals(MagicStrings.unknown_property_value)) {
            input = ps.chatSession.currentQuestion;
        }

        log.info("{} ML parameter: {} input: {}", ps.chatSession.sessionId, parameter, input);
        log.info("{} ML model: {} nBest: {} threshold:  {} score:{}", ps.chatSession.sessionId, model, nBest, threshold, score);

        String out = checkEmpty(SprintUtils.ml(model, nBest, threshold, score, input, ps.chatSession.sessionId));


        return out.replace("__label__", "");
    }
    
    
    /**
     * Regex
     * @param node
     * @param ps
     * @return
     */
    private static String regex(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        String pattern = getAttributeOrTagValue(node, ps, "pattern");
        Integer group = null;
         
        if(pattern == null)
        {
            log.warn("invalid patter");
            return MagicStrings.unknown_property_value;
        }
        
        try 
        { 
            group = Integer.parseInt(getAttributeOrTagValue(node, ps, "group")); 
        } catch (Exception e) {
            log.warn("regex parseInt Exception setting to default 0.");
        } 
        
        
        String input; 
        if(parameter == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(parameter);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter; 
        
        
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(input);
        
        String result = Boolean.toString(matcher.matches());
        
        if(group != null)
        {
            if(matcher.matches())
                result =  matcher.group(group); 
            else
                result = MagicStrings.unknown_property_value; 
        }
        
        log.info("regex pattern: " + pattern 
                + " parameter: " + parameter 
                + "  group: " + group 
                + "  input: " + input
                + " output: " + result);
        
        return checkEmpty(result);
    }
    
    
    /**
     * Levenshtein distance words compare 
     * @param node
     * @param ps
     * @return distance in %
     */
    private static String compare(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        String word = getAttributeOrTagValue(node, ps, "word");
        String minAccuracy = getAttributeOrTagValue(node, ps, "minaccuracy");
       
        
        String param = ps.chatSession.predicates.get(parameter);
                        
        if(Validator.nums(minAccuracy) == null)
            minAccuracy = ps.chatSession.predicates.get(minAccuracy);
        
        
        int min = 90; 
        
        try {
            
            String tmp = Validator.nums(minAccuracy);   
            if(tmp != null)
                min = Integer.parseInt(tmp);  
            else
                log.warn("invalid minAccuracy ({}) setting to default 90.", minAccuracy);
       } catch (Exception e) {
            log.error("compare parseInt Exception setting to default 90.");
            min = 90; 
        }                
        
        int comp = (int)(Validator.compareWords(param, ps.chatSession.predicates.get(word)) * 100);
                
        String out = "OK";
        
        if(comp < min)
            out = "KO";
                        
        log.info("compare word name: " + word 
                + " parameter name: " + parameter                 
                + "  parameter: " + ps.chatSession.predicates.get(parameter)
                + "  compare value: " + comp
                + "  word: " + ps.chatSession.predicates.get(word)
                + "  minAccuracy: " + minAccuracy
                + "  min: " + min
                + " out: " + param);
        
        return out;
    }
    /**
     * Compare two digits
     * @param node
     * @param ps
     * @return
     */
    private static String lessthan(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        String comparator = getAttributeOrTagValue(node, ps, "comparator");
        
        String result = MagicStrings.unknown_property_value; 
        
        if(parameter == null || comparator == null)
        {
            log.warn("lessthan error empty parameter or comparator!");
            return result;
        }
       
        String _parameter = ps.chatSession.predicates.get(parameter);
        String _comparator = ps.chatSession.predicates.get(comparator);
        
        if(_parameter.equals(MagicStrings.unknown_property_value))
            _parameter = parameter; 
        
        if(_comparator.equals(MagicStrings.unknown_property_value))
            _comparator = comparator; 
        
        int par, com; 
        try {
            par = Integer.parseInt(Validator.numsWithSign(_parameter));
            com = Integer.parseInt(Validator.numsWithSign(_comparator));
        } catch (Exception e) {
            log.warn("lessthan error invalid parameter or comparator!");
            return result;
        }
                                
        if(par < com)
            result = "true";
        else
            result = "false";

        log.info("lessthan comparator name: {} parameter name: {}  parameter value: {}  comparator value: {} result: {}",
                comparator, parameter, par, com, result);
        
        return result;
    }
    /**
     * Compare two digits
     * @param node
     * @param ps
     * @return
     */
    private static String greaterthan(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        String comparator = getAttributeOrTagValue(node, ps, "comparator");
        
        String result = MagicStrings.unknown_property_value; 
        
        if(parameter == null || comparator == null)
        {
            log.warn("greatherthan error empty parameter or comparator!");
            return result;
        }
       
        String _parameter = ps.chatSession.predicates.get(parameter);
        String _comparator = ps.chatSession.predicates.get(comparator);
        
        if(_parameter.equals(MagicStrings.unknown_property_value))
            _parameter = parameter; 
        
        if(_comparator.equals(MagicStrings.unknown_property_value))
            _comparator = comparator; 
        
        int par, com; 
        try {
            par = Integer.parseInt(Validator.numsWithSign(_parameter));
            com = Integer.parseInt(Validator.numsWithSign(_comparator));
        } catch (Exception e) {
            log.warn("greatherthan error invalid parameter or comparator!");
            return result;
        }
                                
        if(par > com)
            result = "true";
        else
            result = "false";
                        
        log.info("greatherthan comparator name: " + comparator
                + " parameter name: " + parameter                 
                + "  parameter value: " + par
                + "  comparator value: " + com                
                + " result: " + result);
        
        return result;
    }
    
    /**
     * Validate PESEL
     * @param node
     * @param ps
     * @return PESEL or "unknown"
     */
    
    private static String pesel(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
                                                                     
        String pesel; 
        if(parameter == null)        
            pesel = evalTagContent(node, ps, null);     
        else
            pesel = ps.chatSession.predicates.get(parameter);  
                                                       
        if(pesel.equals(MagicStrings.unknown_property_value))
            pesel = parameter; 
        
        String result = Validator.pesel(pesel);

        log.info("pesel  parameter name: {} parameter: {} result: {}", parameter, pesel, result);
                                        
        return checkEmpty(result);
    }
    
    
    private static String currency(Node node, ParseState ps) {
        
        String parameter = getAttributeOrTagValue(node, ps, "parameter");                                                     
        
        String input; 
        if(parameter == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(parameter);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter; 
        
        String result = Validator.currency(input);

        log.info("currency  parameter name: {} parameter: {} result: {}",
                parameter, input, result);
                                        
        return checkEmpty(result);
    }
    private static String txt2num(Node node, ParseState ps) {
        String language = getAttributeOrTagValue(node, ps, "language");
        String parameter = getAttributeOrTagValue(node, ps, "parameter"); 
        
        
        if(language == null || language.isEmpty())
            language = "PL";
        
        language = language.toUpperCase(); 
        
        String input; 
        if(parameter == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(parameter);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;  
                                        
        String result = Validator.WordsToNumbers(language, input); 
        
        log.info("txt2num "
                + " parameter: " + parameter                 
                + " input: " + input
                + " result: " + result);                
                                        
        return checkEmpty(result);
    }

    private static String txt2dec(Node node, ParseState ps) {
        String language = getAttributeOrTagValue(node, ps, "language");
        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        if(language == null || language.isEmpty())
            language = "PL";

        language = language.toUpperCase();

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String result = Validator.WordsToNumbersDec(language, input);

        log.info("txt2dec  parameter: {} input: {} result: {}", parameter, input, result);

        return checkEmpty(result);
    }
    
    
    private static String zip(Node node, ParseState ps) {
        String country = getAttributeOrTagValue(node, ps, "country");
        String parameter = getAttributeOrTagValue(node, ps, "parameter"); 
        
        
        if(country == null || country.isEmpty())
            country = "PL";
        
        country = country.toUpperCase(); 
        
        String input; 
        if(parameter == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(parameter);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;  
                                        
        String result = Validator.zip(country, input);

        log.info("zip  parameter: {} input: {} result: {}", parameter, input, result);
                                        
        return checkEmpty(result);
    }

    private static String getRecord(Node node, ParseState ps) {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;


        String result = SprintBotDbUtils.getRecord(input);

        log.info("getRecord  parameter: {} input: {} result: {}", parameter, input, result);

        return checkEmpty(result);
    }

    private static String getRecordStatus(Node node, ParseState ps) {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;


        String result = SprintBotDbUtils.getRecordStatus(input);

        log.info("getRecordStatus  parameter: {} input: {} result: {}", parameter, input, result);

        return checkEmpty(result);
    }
    private static String getData(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");
        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String result = SprintBotDbUtils.getData(input, ps.chatSession.sessionId);

        log.info("{} getdata parameter: {} input: {} result: {}", ps.chatSession.sessionId, parameter, input, result);

        return checkEmpty(result);
    }

    private static String updateRecord(Node node, ParseState ps) {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String result = SprintBotDbUtils.updateRecord(input);
        log.info("updateRecord  parameter: {} input: {} result: {}", parameter, input, result);
        return result == null ? "ERR" : result;
    }

    private static String updateRecordStatus(Node node, ParseState ps) {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String result = SprintBotDbUtils.updateRecordStatus(input);
        log.info("updateRecordStatus  parameter: {} input: {} result: {}", parameter, input, result);
        return result == null ? "ERR" : result;
    }
    private static String setData(Node node, ParseState ps) {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String result = SprintBotDbUtils.setData(input, ps.chatSession.sessionId);
        log.info("{} setData  parameter: {} input: {} result: {}", ps.chatSession.sessionId, parameter, input, result);
        return result == null ? "ERR" : result;
    }

    private static String dateadd(Node node, ParseState ps) throws ParseException {

        String parameter = getAttributeOrTagValue(node, ps, "parameter");

        String days = getAttributeOrTagValue(node, ps, "days");
        String format = getAttributeOrTagValue(node, ps, "format");
        String locale = getAttributeOrTagValue(node, ps, "locale");

        if(format == null)
            format="dd/MM/yyyy";

        if(locale == null || locale.isEmpty())
            locale = "pl";

        String input;
        if(parameter == null)
            input = evalTagContent(node, ps, null);
        else
            input = ps.chatSession.predicates.get(parameter);

        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter;

        String _days;
        if(days == null)
            _days = evalTagContent(node, ps, null);
        else
            _days = ps.chatSession.predicates.get(days);

        if(_days.equals(MagicStrings.unknown_property_value))
            _days = days;


        Locale loc = Locale.forLanguageTag(locale);
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, loc);
        int iDays = Integer.parseInt(_days);

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFormat.parse(input));
        cal.add(Calendar.DATE, iDays);
        String result = dateFormat.format(cal.getTime());

        log.info("dateadd "
                + " parameter: " + parameter
                + " input: " + input
                + " days: " + days
                + " format: " + format
                + " locale: " + locale
                + " result: " + result);

        return checkEmpty(result);
    }
    
    
    private static String setall(Node node, ParseState ps) { 
        
        String values = getAttributeOrTagValue(node, ps, "values");
        String variables = getAttributeOrTagValue(node, ps, "variables");
        String delimiter = getAttributeOrTagValue(node, ps, "delimiter");
        
        
        String input; 
        if(values == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(values);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = values;  

        String[] vars = variables.split(delimiter);
        String[] vals = input.split(delimiter);
        
        if(vars.length != vals.length)
            return "ERR";
        
        for(int i=0;i<vars.length;i++) {
            if (vars[i] != null) ps.chatSession.predicates.put(vars[i], vals[i]);
            if (vars[i] != null) ps.vars.put(vars[i], vals[i]);
        }
                
        return "OK";
    }
    
    
    
    private static String num2txt(Node node, ParseState ps) {
        String language = getAttributeOrTagValue(node, ps, "language");
        String parameter = getAttributeOrTagValue(node, ps, "parameter");           
         
        if(language == null || language.isEmpty())
            language = "PL";
        
        language = language.toUpperCase(); 
        
        String input; 
        if(parameter == null)        
            input = evalTagContent(node, ps, null);     
        else
            input = ps.chatSession.predicates.get(parameter);  
                                                       
        if(input.equals(MagicStrings.unknown_property_value))
            input = parameter; 
                       
        long  num = 0;
        try {
            num = Long.parseLong(input);
        } catch (Exception ex) {
            log.error("num2txt Error", ex);
            return MagicStrings.unknown_property_value; 
        }
        
        String result = Validator.NumbersToWords(language, num);

        log.info("num2txt  parameter: {} input: {} result: {}", parameter, input, result);
                                        
        return checkEmpty(result);
    }
    
    /**
     * Returns sex form PESEL
     * @param node
     * @param ps
     * @return M = Men, K = Woman, "unknown" = invalid input
     */    
    private static String sexpesel(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String pesel; 
        if(parameter == null)        
            pesel = evalTagContent(node, ps, null);     
        else
            pesel = ps.chatSession.predicates.get(parameter);  
                                                       
        if(pesel.equals(MagicStrings.unknown_property_value))
            pesel = parameter; 
                        
        String result = Validator.getSexByPesel(pesel);                
                        
        log.info("sexpesel "
                + " parameter: " + parameter                 
                + " pesel: " + pesel
                + " result: " + result);
        
        return checkEmpty(result);
    }
    /**
     * Return birthdate from pesel
     * @param node
     * @param ps
     * @return
     */
    private static String birtPesel(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        String format = getAttributeOrTagValue(node, ps, "format");
        
        if(format == null) format="dd/MM/yyyy"; 
        
        String pesel; 
        if(parameter == null)        
            pesel = evalTagContent(node, ps, null);     
        else
            pesel = ps.chatSession.predicates.get(parameter);  
                                                       
        if(pesel.equals(MagicStrings.unknown_property_value))
            pesel = parameter;

        if(pesel == null)
            return MagicStrings.unknown_property_value;
        
        PeselValidator validator = new PeselValidator(pesel);
        
        if(!validator.isValid()) {
            return MagicStrings.unknown_property_value;
        }
        
        String result = Validator.getBirthdateFromPesel(pesel, format);

        log.info("birtPesel  parameter: {} pesel: {} format: {} result: {}", parameter, pesel, format, result);
        
        return checkEmpty(result);
    }
    private static String nip(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String nip; 
        if(parameter == null)        
            nip = evalTagContent(node, ps, null);     
        else
            nip = ps.chatSession.predicates.get(parameter);  
                                                       
        if(nip.equals(MagicStrings.unknown_property_value))
            nip = parameter; 
        
        if(nip == null)
            return MagicStrings.unknown_property_value;

        String result = Validator.nip(nip);

        log.info("nip  parameter: {} nip: {} result: {}", parameter, nip, result);
        
        return checkEmpty(result);
    }


    
    private static String nums(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String nums; 
        if(parameter == null)        
            nums = evalTagContent(node, ps, null);     
        else
            nums = ps.chatSession.predicates.get(parameter);  
                                                       
        if(nums.equals(MagicStrings.unknown_property_value))
            nums = parameter; 
        
        String result = Validator.nums(nums); 
        
        log.info("nums "
                + " parameter: " + parameter                 
                + " nums: " + nums
                + " result: " + result);                
                                        
        return checkEmpty(result);
    }
    
    
    private static String implode(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String temp; 
        if(parameter == null)        
            temp = evalTagContent(node, ps, null);     
        else
            temp = ps.chatSession.predicates.get(parameter);  
                                                       
        if(temp.equals(MagicStrings.unknown_property_value))
            temp = parameter; 
        
        if(temp == null)
            return MagicStrings.unknown_property_value;
        
        
        String[] split = temp.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
            if (i != split.length - 1) {
                sb.append("");
            }
        }       
        
        String result = sb.toString();
        
        log.info("implode "
                + " parameter: " + parameter                 
                + " temp: " + temp
                + " result: " + result);                
                                        
        return checkEmpty(result);
    }
    
    private static String increment(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");                  
        
        String num;
        if(parameter == null)        
            num = evalTagContent(node, ps, null);
        else
            num = ps.chatSession.predicates.get(parameter);
                                                       
        if(num.equals(MagicStrings.unknown_property_value))
            num = parameter;

        String result = MagicStrings.unknown_property_value;

        try {
            int i = Integer.parseInt(num);
            i++;
            result = String.valueOf(i);

        } catch (Exception e) {
            log.warn("increment error invalid parameter: " + parameter);
            return result;
        }

        log.info("increment "
                + " parameter: " + parameter
                + " num: " + num
                + " output: " + result);

        return result;
    }
    private static String decrement(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");                  
        String num;
        if(parameter == null)        
            num = evalTagContent(node, ps, null);
        else
            num = ps.chatSession.predicates.get(parameter);
                                                       
        if(num.equals(MagicStrings.unknown_property_value))
            num = parameter;


        String result = MagicStrings.unknown_property_value;

        try {
            int i = Integer.parseInt(num);
            i--;
            result = String.valueOf(i);

        } catch (Exception e) {
            log.warn("decrement error invalid parameter: " + parameter);
            return result;
        }

        log.info("decrement "
                + " parameter: " + parameter                 
                + " num: " + num
                + " result: " + result);

        return result;
    }
    
    /**
     * Check is valid time number 9 digits
     * @param node
     * @param ps
     * @return
     */
    
    private static String phone(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");
        boolean ext = Boolean.parseBoolean(getAttributeOrTagValue(node, ps, "ext"));

        String phone; 
        if(parameter == null)        
            phone = evalTagContent(node, ps, null);     
        else
            phone = ps.chatSession.predicates.get(parameter);  
                                                       
        if(phone.equals(MagicStrings.unknown_property_value))
            phone = parameter; 

        String result;

        if(ext)
            result = Validator.extphone(phone);
        else
            result = Validator.phone(phone);

        log.info("phone  parameter: {} phone: {} result: {}",
                parameter, phone, result);
        
        return checkEmpty(result);
    }

    private static String txt2time(Node node, ParseState ps) throws Exception {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String time; 
        if(parameter == null)        
            time = evalTagContent(node, ps, null);     
        else
            time = ps.chatSession.predicates.get(parameter);  
                                                       
        if(time.equals(MagicStrings.unknown_property_value))
            time = parameter; 
        
        String result = Validator.convertTime(time);                 

        log.info("txt2time "
                + " parameter: " + parameter                 
                + " time: " + time
                + " result: " + result);
        
        return checkEmpty(result);
    }
    
    
    private static String bankAccount(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String account; 
        if(parameter == null)        
            account = evalTagContent(node, ps, null);     
        else
            account = ps.chatSession.predicates.get(parameter);  
                                                       
        if(account.equals(MagicStrings.unknown_property_value))
            account = parameter; 
        
        String result = Validator.bankAccount(account);

        log.info("bankAccount  parameter: {} account: {} result: {}",
                parameter, account, result);
        
        return checkEmpty(result);
    }

    private static String reportSave(Node node, ParseState ps) {
        String reportName = getAttributeOrTagValue(node, ps, "report_name");

        //fraza
        String licznikFraz = getAttributeOrTagValue(node, ps, "licznik_fraz");
        String frazaCala = getAttributeOrTagValue(node, ps, "fraza_cala");
        String fraza = getAttributeOrTagValue(node, ps, "fraza");
        String rozpoznanie = getAttributeOrTagValue(node, ps, "rozpoznanie");
        String label = getAttributeOrTagValue(node, ps, "label");
        String wiarygodnosc = getAttributeOrTagValue(node, ps, "wiarygodnosc");
        String fakt = getAttributeOrTagValue(node, ps, "fakt");

        //ocena
        String licznikOcen = getAttributeOrTagValue(node, ps, "licznik_ocen");
        String sposobOceny = getAttributeOrTagValue(node, ps, "sposob_oceny");
        String ocena = getAttributeOrTagValue(node, ps, "ocena");

        //info
        String botName = getAttributeOrTagValue(node, ps, "bot_name");
        String info = getAttributeOrTagValue(node, ps, "info");
        String klucz = getAttributeOrTagValue(node, ps, "klucz");
        String wartosc = getAttributeOrTagValue(node, ps, "wartosc");

        Report report = new Report(
                getPredicate(frazaCala, node, ps),
                getPredicate(fraza,node, ps),
                getPredicate(rozpoznanie,node, ps),
                getPredicate(label,node, ps),
                getPredicate(wiarygodnosc,node, ps),
                getPredicate(fakt,node, ps),
                getPredicate(licznikFraz,node, ps),
                getPredicate(licznikOcen,node, ps),
                getPredicate(sposobOceny,node, ps),
                getPredicate(ocena,node, ps),
                getPredicate(botName,node, ps),
                getPredicate(info,node, ps),
                getPredicate(klucz,     node, ps),
                getPredicate(wartosc,node, ps));

        CompletableFuture<Void> future = SprintBotDbUtils.saveReportAsync(reportName, ps.chatSession.symbol, report, ps.chatSession.sessionId);

        future.thenRun(() -> log.info("saveReportAsync reportName: {} saved", reportName))
                .exceptionally(ex -> {
                    log.warn("saveReportAsync Error", ex);
                    return null;
                });

        return "";
    }

    private static String getPredicate(String value, Node node, ParseState ps) {
        if(value == null)
            return null;
        else {
            String resp = ps.chatSession.predicates.get(value);
            if(resp.equals(MagicStrings.unknown_property_value))
                return null;
            return resp;
        }
    }

    private static String txt2date(Node node, ParseState ps) {
        String parameter = getAttributeOrTagValue(node, ps, "parameter"); 
        String format = getAttributeOrTagValue(node, ps, "format");
        String locale = getAttributeOrTagValue(node, ps, "locale");
        boolean isPast = Boolean.parseBoolean(getAttributeOrTagValue(node, ps, "ispast"));

        if(locale == null)
            locale = "pl";

        if(format == null)
            format="dd/MM/yyyy"; 
        
        String date; 
        if(parameter == null)        
            date = evalTagContent(node, ps, null);     
        else
            date = ps.chatSession.predicates.get(parameter);  
                                                       
        if(date.equals(MagicStrings.unknown_property_value))
            date = parameter; 
        
        String result = MagicStrings.unknown_property_value; 
        try {
            result = Validator.dateFormat(date, format, isPast,  locale);
        } catch (Exception ex) {
            log.error("datetext Error", ex);
        }

        log.info("datetext  parameter: {} date: {} format: {} isPast: {} output: {}",
                parameter, date, format, isPast, result);
        
        return checkEmpty(result);
    }
    private static String txt2datetime(Node node, ParseState ps)
    {        
        String parameter = getAttributeOrTagValue(node, ps, "parameter"); 
        String format = getAttributeOrTagValue(node, ps, "format");
        String locale = getAttributeOrTagValue(node, ps, "locale");
        boolean isPast = Boolean.parseBoolean(getAttributeOrTagValue(node, ps, "ispast"));

        if(locale == null)
            locale = "pl";
        
        if(format == null)
            format="dd/MM/yyyy"; 
        
        String date; 
        if(parameter == null)        
            date = evalTagContent(node, ps, null);     
        else
            date = ps.chatSession.predicates.get(parameter);  
                                                       
        if(date.equals(MagicStrings.unknown_property_value))
            date = parameter; 
        
        String result = MagicStrings.unknown_property_value; 
        try {
            result = Validator.txt2dateTime(date, format, isPast, locale);
        } catch (Exception ex) {            
            log.error("txt2dateTime Error", ex);
        }

        log.info("txt2dateTime  parameter: {} date: {} format: {} isPast: {} output: {}",
                parameter, date, format, isPast, result);
        
        return checkEmpty(result);
    }
    
    
    private static String math(Node node, ParseState ps) throws Exception
    {                
        String operation = getAttributeOrTagValue(node, ps, "operation");
        
        String format = getAttributeOrTagValue(node, ps, "format");
        if(format == null)
            format = "#";
                       
        if(operation == null)        
            operation = evalTagContent(node, ps, null);     
        else
            operation = ps.chatSession.predicates.get(operation);  
                                                               
        
        operation = operation.replaceAll(",", ".");

        log.info("math  operation: {} format: {}", operation, format);
                  
        //result = String.format(format,Validator.math(operation));
        String result = new DecimalFormat(format).format(Validator.math(operation));

        log.info("math  operation: {} format: {} result: {}", operation, format, result);
        
        return checkEmpty(result);
    }


    private static String aiCheckResponse(String channel, String response) {
        log.info("aiCheckResponse channel: {} response: {}", channel, response);
        if(channel == null)
            return response;

        if(response == null)
            return null;

        if(channel.equals("VOICE"))
            response = response
                    .replace("\\n",".");
        else
            response = response
                    .replace("\\n","<br />");
        return response;
    }

    private static String saveContext(Node node, ParseState ps) {
        String type = getAttributeOrTagValue(node, ps, "type");
        String contextName = getAttributeOrTagValue(node, ps, "name");

        if(contextName == null)
            contextName = evalTagContent(node, ps, null);
        else
            contextName = ps.chatSession.predicates.get(contextName);

        String sessionId = ps.chatSession.sessionId;

        log.info("{}\tsaveContext type: {} name : {}", sessionId, type, contextName);

        if(type.equals(MagicStrings.unknown_property_value) || contextName.equals(MagicStrings.unknown_property_value))
            return "";

        ps.chatSession.llmContext.put(type+contextName,ps.chatSession.json);

        log.info("{}\tsaveContext json:\t{}", sessionId, ps.chatSession.json);

        return "";
    }

    private static String gpt(Node node, ParseState ps) throws Exception {

        String model = getAttributeOrTagValue(node, ps, "model");

        String assistant = getAttributeOrTagValue(node, ps, "assistant");
        String max_history = getAttributeOrTagValue(node, ps, "max_history");

        String contextName = getAttributeOrTagValue(node, ps, "context");
        if(contextName == null)
            contextName = evalTagContent(node, ps, null);
        else
            contextName = ps.chatSession.predicates.get(contextName);

        String context = null;

        String sessionId = ps.chatSession.sessionId;

        if (contextName != null && !contextName.equals(MagicStrings.unknown_property_value)) {
            context = ps.chatSession.llmContext.get("gpt"+contextName);
            log.info("{}\tgetContext context name: {} value:\t{}", sessionId, contextName, context);
        }


        String addparams = getAttributeOrTagValue(node, ps, "addparams");
        if(addparams == null)
            addparams = evalTagContent(node, ps, null);
        else
            addparams = ps.chatSession.predicates.get(addparams);

        String user = getAttributeOrTagValue(node, ps, "user");
        if(user == null)
            user = evalTagContent(node, ps, null);
        else
            user = ps.chatSession.predicates.get(user);

        String system = getAttributeOrTagValue(node, ps, "system");
        if(system == null)
            system = evalTagContent(node, ps, null);
        else
            system = ps.chatSession.predicates.get(system);


        if(assistant == null)
            assistant = evalTagContent(node, ps, null);
        else
            assistant = ps.chatSession.predicates.get(assistant);


        if(assistant == null || assistant.equals("unknown") || assistant.isEmpty())
            assistant = ps.chatSession.lastResponse;



        if(model == null)
            model = evalTagContent(node, ps, null);
        else
            model = ps.chatSession.predicates.get(model);

        if(model == null || model.equals(MagicStrings.unknown_property_value) || model.isEmpty()) {
            model = LLMConfiguration.gptDefaultModel;
        }

        String json = ps.chatSession.json;
        if(context != null) json = context;

        int iMaxResponse = ps.chatSession.maxHistory;
        if(iMaxResponse == 0) {
            if (max_history == null || max_history.equals(MagicStrings.unknown_property_value)) {
                iMaxResponse = LLMConfiguration.gptMaxHistory;
            } else {
                iMaxResponse = Integer.parseInt(max_history);
            }
            ps.chatSession.maxHistory = iMaxResponse;
        }

        String botname = ps.chatSession.bot.name;

        log.info("{}\tGPT botname: {} model: {} user: {} system: {} assistant: {} addparams: {} maxResponse: {} json: \n{}\n",
                sessionId, botname, model, user, system, assistant, addparams, iMaxResponse, json);


        Map<String, String> additionalParameters = new HashMap<>();
        if(addparams != null && !addparams.isEmpty()) {
            String[] params = addparams.split(",");
            for(String param : params) {
                String[] keyVal = param.split("=");
                if(keyVal.length == 2) {
                    additionalParameters.put(keyVal[0].trim(), keyVal[1].trim());
                }
            }
        }

        log.info("{}\tGPT  assistant: {} system: {}", sessionId, assistant, system);

        String request;

        if(assistant != null && !assistant.isEmpty() && system != null && !system.isEmpty()) {
            json = null;
        }

        if(json == null) {
            JSONObject responseJson = GenAIHelper
                    .createGPTResponse(model, system, user, assistant, additionalParameters);
            request = responseJson.toString();
        } else {
            if(assistant != null && !assistant.isEmpty())
                json = GenAIHelper
                        .addGptMessageToJSON(json,"assistant", assistant.replaceAll("\\<.*?\\>", ""), iMaxResponse);
            if(system != null && !system.isEmpty())
                json = GenAIHelper
                        .addGptMessageToJSON(json,"system", system.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            json = GenAIHelper.addGptMessageToJSON(json,"user", user.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            request = json;
        }
        ps.chatSession.json = request;

        int timeout = Objects.equals(ps.chatSession.bot.properties.get("timeout"), MagicStrings.unknown_property_value) ?
                10 : Integer.parseInt(ps.chatSession.bot.properties.get("timeout"));

        if(LLMConfiguration.timeout != timeout) {
            LLMService.setTimeout(timeout);
            log.info("GPT new timeout: {}", timeout);
        }

        String response = aiCheckResponse(ps.chatSession.channel, LLMService.chatGpt(request, LLMConfiguration.gptTokens.get(botname)));
        ps.chatSession.lastResponse = response;

        log.info("{}\tGPT response: {}", sessionId, response);

        return response;

    }

    private static String ollama(Node node, ParseState ps) throws Exception {

        String model = getAttributeOrTagValue(node, ps, "model");
        String system = getAttributeOrTagValue(node, ps, "system");
        String user = getAttributeOrTagValue(node, ps, "user");
        String stream = getAttributeOrTagValue(node, ps, "stream");
        String max_history = getAttributeOrTagValue(node, ps, "max_history");


        String contextName = getAttributeOrTagValue(node, ps, "context");
        if(contextName == null)
            contextName = evalTagContent(node, ps, null);
        else
            contextName = ps.chatSession.predicates.get(contextName);


        String context = null;

        if (contextName != null && !contextName.equals(MagicStrings.unknown_property_value)) {
            context = ps.chatSession.llmContext.get("gpt"+contextName);
            log.info("OLLAMA context name: {} value: {}", contextName, context);
        }


        if(user == null)
            user = evalTagContent(node, ps, null);
        else
            user = ps.chatSession.predicates.get(user);


        if(system == null)
            system = evalTagContent(node, ps, null);
        else
            system = ps.chatSession.predicates.get(system);


        String json = ps.chatSession.json;
        if(context != null) json = context;

        String sessionId = ps.chatSession.sessionId;
        log.info("{} OLLAMA  user: {} system: {} stream: {} json: \n{}\n", sessionId, user, system, stream, json);


        if(model == null || model.equals(MagicStrings.unknown_property_value) || model.isEmpty()) {
            model = LLMConfiguration.ollamaDefaultModel;
        }

        int iMaxResponse = ps.chatSession.maxHistory;
        if(iMaxResponse == 0) {
            if (max_history == null || max_history.equals(MagicStrings.unknown_property_value)) {
                iMaxResponse = LLMConfiguration.ollamaMaxHistory;
            } else {
                iMaxResponse = Integer.parseInt(max_history);
            }
            ps.chatSession.maxHistory = iMaxResponse;
        }



        boolean bStream = false;
        if(stream != null) bStream = Boolean.parseBoolean(stream);

        log.info("{} OLLAMA  model: {} user: {} max_history: {} stream: {}",
                ps.chatSession.sessionId, model, user, iMaxResponse, bStream);

        log.info("{} OLLAMA system: {}", ps.chatSession.sessionId, system);

        String request;
        if(json == null) {
            JSONObject responseJson = GenAIHelper
                    .createOllamaResponse(model, system, user, bStream);

            request = responseJson.toString();
        } else {
            if(system != null && !system.isEmpty())
                json = GenAIHelper
                        .addOllamaMessageToJSON(json,"system", system.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            json = GenAIHelper.addOllamaMessageToJSON(json,"user", user.replaceAll("\\<.*?\\>", ""), iMaxResponse);
            request = json;
        }
        ps.chatSession.json = request;

        int timeout = Objects.equals(ps.chatSession.bot.properties.get("timeout"), MagicStrings.unknown_property_value) ?
                10 : Integer.parseInt(ps.chatSession.bot.properties.get("timeout"));

        if(LLMConfiguration.timeout != timeout) {
            LLMService.setTimeout(timeout);
            log.info("OLLAMA new timeout: {}", timeout);
        }

        String response = aiCheckResponse(ps.chatSession.channel, LLMService.chatOllama(request));
        ps.chatSession.lastResponse = response;

        log.info("OLLAMA response: {}", response);

        return response;

    }

    private static String gemini(Node node, ParseState ps) throws Exception {

        String context = getAttributeOrTagValue(node, ps, "context");
        String bot = getAttributeOrTagValue(node, ps, "bot");
        String user = getAttributeOrTagValue(node, ps, "user");

        String max_history = getAttributeOrTagValue(node, ps, "max_history");
        String addparams = getAttributeOrTagValue(node, ps, "addparams");

        if(addparams == null)
            addparams = evalTagContent(node, ps, null);
        else
            addparams = ps.chatSession.predicates.get(addparams);

        Map<String, String> additionalParameters = new HashMap<>();
        if(addparams != null && !addparams.isEmpty()) {
            String[] params = addparams.split(",");
            for(String param : params) {
                String[] keyVal = param.split("=");
                if(keyVal.length == 2) {
                    additionalParameters.put(keyVal[0].trim(), keyVal[1].trim());
                }
            }
        }

        String contextName = getAttributeOrTagValue(node, ps, "context");
        if(contextName == null)
            contextName = evalTagContent(node, ps, null);
        else
            contextName = ps.chatSession.predicates.get(contextName);


        String gContext = null;

        if (contextName != null && !contextName.equals(MagicStrings.unknown_property_value)) {
            gContext = ps.chatSession.llmContext.get("gpt"+contextName);
            log.info("GEMINI context name: {} value: {}", contextName, gContext);
        }

        if(user == null)
            user = evalTagContent(node, ps, null);
        else
            user = ps.chatSession.predicates.get(user);


        if(context == null)
            context = evalTagContent(node, ps, null);
        else
            context = ps.chatSession.predicates.get(context);


        if(bot == null)
            bot = evalTagContent(node, ps, null);
        else
            bot = ps.chatSession.predicates.get(bot);


        if(bot == null || bot.equals("unknown") || bot.isEmpty())
            bot = ps.chatSession.lastResponse;

        String json = ps.chatSession.json;

        if(gContext != null) json = gContext;

        String sessionId = ps.chatSession.sessionId;

        String botname = ps.chatSession.bot.name;
        log.info("{} gemini botname: {} context: {} user: {} bot: {} json: \n{}\n",
                sessionId, botname, context, user, bot, json);

        int iMaxResponse = ps.chatSession.maxHistory;
        if(iMaxResponse == 0) {
            if (max_history == null || max_history.equals(MagicStrings.unknown_property_value)) {
                iMaxResponse = LLMConfiguration.geminiMaxHistory;
            } else {
                iMaxResponse = Integer.parseInt(max_history);
            }
            ps.chatSession.maxHistory = iMaxResponse;
        }

        log.info("{} gemini  user: {} addparams: {}",
                ps.chatSession.sessionId, user, addparams);

        String request;
        if(json == null) {
            JSONObject responseJson = GenAIHelper
                    .createGeminiResponse(context, user, additionalParameters);
            request = responseJson.toString();
        } else {
            if(bot != null && !bot.isEmpty())
                json = GenAIHelper
                        .addGeminiMessageToJSON(json, context,"bot", bot.replaceAll("\\<.*?\\>", ""), iMaxResponse);

            json = GenAIHelper.addGeminiMessageToJSON(json,context,"user", user.replaceAll("\\<.*?\\>", ""), iMaxResponse);
            request = json;
        }

        ps.chatSession.json = request;

        int timeout = Objects.equals(ps.chatSession.bot.properties.get("timeout"), MagicStrings.unknown_property_value) ?
                10 : Integer.parseInt(ps.chatSession.bot.properties.get("timeout"));

        if(LLMConfiguration.timeout != timeout) {
            LLMService.setTimeout(timeout);
            log.info("GEMINI new timeout: {}", timeout);
        }

        String response = aiCheckResponse(ps.chatSession.channel, LLMService.chatGemini(request, LLMConfiguration.geminiTokens.get(botname)));
        ps.chatSession.lastResponse = response;

        log.info("GEMINI response: {}", response);

        return response;

    }

    public static void resetClassCache() {
        SprintUtils.resetClassCache();
    }

    /**
     * Implements jar plugin integration 
     * @param node current XML parse node
     * @param ps AIML parse state
     * @return
     * @throws IOException 
     */
    private static String plugin(Node node, ParseState ps) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        
        String file = getAttributeOrTagValue(node, ps, "file");  
        String classLoad = getAttributeOrTagValue(node, ps, "class");
        
        String method = getAttributeOrTagValue(node, ps, "method");  
        String parameter = getAttributeOrTagValue(node, ps, "parameter");  
        
        String path = new File(".").getCanonicalPath().replace("\\", "/");
        log.info("method: " + method + " parameter: " + parameter + " value: " + ps.chatSession.predicates.get(parameter));
        String out = SprintUtils.callPlugin(path + "/lib/" + file, classLoad, method, ps.chatSession.predicates.get(parameter), ps.chatSession.sessionId);
           
        if(parameter !=null && out.startsWith(parameter)) {
            out = out.substring(parameter.length());
        }
        
        return out;
    }
    
    ///////////////////////end sprint custom methods

    /** get the value of an AIML predicate.
     * implements <get name="predicate"></get>  and <get var="varname"></get>
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the result of the <get> operation
     */
    private static String get(Node node, ParseState ps) {
        String result = MagicStrings.unknown_predicate_value;
        String predicateName = getAttributeOrTagValue(node, ps, "name");
        String varName = getAttributeOrTagValue(node, ps, "var");
        if (predicateName != null)
           result = ps.chatSession.predicates.get(predicateName).trim();
        else if (varName != null)
           result = ps.vars.get(varName).trim();
        return result;
    }

    /**
     * return the value of a bot property.
     * implements {{{@code <bot name="property"/>}}}
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the bot property or a string indicating the property was not found.
     */
    private static String bot(Node node, ParseState ps) {
        String result = MagicStrings.unknown_property_value;
        //HashSet<String> attributeNames = Utilities.stringSet("name");
        String propertyName = getAttributeOrTagValue(node, ps, "name");
        
        log.info("bot propertyName: " + propertyName);
        
        if (propertyName != null)
           result = ps.chatSession.bot.properties.get(propertyName).trim();
        //log.info("BOT: "+m.getNodeValue()+"="+result);
        return result;
    }

    /**
     * implements formatted date tag <date format="format"/> and <date format="format"/>
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the formatted date
     */
    private static String date(Node node, ParseState ps)  {
        String format = getAttributeOrTagValue(node, ps, "format");      // AIML 2.0
        String locale = getAttributeOrTagValue(node, ps, "locale");
        String timezone = getAttributeOrTagValue(node, ps, "timezone");

        String configLocale = Objects.equals(ps.chatSession.bot.properties.get("locale"), MagicStrings.unknown_property_value) ?
                null : ps.chatSession.bot.properties.get("locale");
        String configTimezone = Objects.equals(ps.chatSession.bot.properties.get("timezone"), MagicStrings.unknown_property_value) ?
                null : ps.chatSession.bot.properties.get("timezone");

        log.info("date format: {} locale: {} timezone: {} configLocale: {} configTimezone: {}",
                format, locale, timezone, configLocale, configTimezone);

        if(locale == null) locale = configLocale;
        if(timezone == null) timezone = configTimezone;

        Locale loc = Locale.forLanguageTag(Objects.requireNonNullElse(locale, "pl"));
        TimeZone tz = TimeZone.getTimeZone(Objects.requireNonNullElse(timezone, TimeZone.getDefault().getID()));

        log.info("Format = {} Locale = {} Timezone = {}", format, locale, timezone);

        String dateAsString = CalendarUtils.date(format, loc, tz);

        log.info(dateAsString);
        return dateAsString;
    }
    
    

    /**
     *    <interval><style>years</style></style><format>MMMMMMMMM dd, yyyy</format><from>August 2, 1960</from><to><date><format>MMMMMMMMM dd, yyyy</format></date></to></interval>
     */

    private static String interval(Node node, ParseState ps)  {
        HashSet<String> attributeNames = Utilities.stringSet("style","format","from","to");
        String style = getAttributeOrTagValue(node, ps, "style");      // AIML 2.0
        String format = getAttributeOrTagValue(node, ps, "format");      // AIML 2.0
        String from = getAttributeOrTagValue(node, ps, "from");
        String to = getAttributeOrTagValue(node, ps, "to");


        String locale = Objects.equals(ps.chatSession.bot.properties.get("locale"), MagicStrings.unknown_property_value) ?
                null : ps.chatSession.bot.properties.get("locale");
        String timezone = Objects.equals(ps.chatSession.bot.properties.get("timezone"), MagicStrings.unknown_property_value) ?
                null : ps.chatSession.bot.properties.get("timezone");

        log.info("interval format: {} locale: {} timezone: {}", format, locale, timezone);

        if(locale == null || locale.equals(MagicStrings.unknown_property_value)) locale = null;
        if(timezone == null || timezone.equals(MagicStrings.unknown_property_value)) timezone = null;

        Locale loc = Locale.forLanguageTag(Objects.requireNonNullElse(locale, "pl"));
        TimeZone tz = TimeZone.getTimeZone(Objects.requireNonNullElse(timezone, TimeZone.getDefault().getID()));


        if (style == null) style = "years";
        if (format == null) format = "MMMMMMMMM dd, yyyy";
        if (from == null) from = "January 1, 1970";
        if (to == null) {
            to = CalendarUtils.date(format, loc, tz);
        }
        String result = "unknown";
        if (style.equals("years")) result = ""+Interval.getYearsBetween(from, to, format);
        if (style.equals("months")) result = ""+Interval.getMonthsBetween(from, to, format);
        if (style.equals("days")) result = ""+Interval.getDaysBetween(from, to, format);
        if (style.equals("hours")) result = ""+Interval.getHoursBetween(from, to, format);
        return result;
    }

    /**
     * get the value of an index attribute and return it as an integer.
     * if it is not recognized as an integer, return 0
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the the integer intex value
     */
    private static int getIndexValue(Node node, ParseState ps) {
        int index=0;
        String value = getAttributeOrTagValue(node, ps, "index");
        if (value != null) try {index = Integer.parseInt(value)-1;} catch (Exception ex) {log.error("getIndexValue Error", ex);}
        return index;
    }

    /**
     * implements {@code <star index="N"/>}
     * returns the value of input words matching the Nth wildcard (or AIML Set).
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the word sequence matching a wildcard
     */
    private static String inputStar(Node node, ParseState ps) {
        int index=getIndexValue(node, ps);
//        log.info(ps.chatSession.sessionId + " inputStar index: " + index);
        if (ps.leaf.starBindings.inputStars.star(index)==null) return "";
        else return ps.leaf.starBindings.inputStars.star(index).trim();
    }
    /**
     * implements {@code <thatstar index="N"/>}
     * returns the value of input words matching the Nth wildcard (or AIML Set) in <that></that>.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the word sequence matching a wildcard
     */
    private static String thatStar(Node node, ParseState ps) {
        int index=getIndexValue(node, ps);
        if (ps.leaf.starBindings.thatStars.star(index)==null) return "";
        else return ps.leaf.starBindings.thatStars.star(index).trim();
    }
    /**
     * implements <topicstar/> and <topicstar index="N"/>
     * returns the value of input words matching the Nth wildcard (or AIML Set) in a topic pattern.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the word sequence matching a wildcard
     */
    private static String topicStar(Node node, ParseState ps) {
        int index=getIndexValue(node, ps);
        if (ps.leaf.starBindings.topicStars.star(index)==null) return "";
        else return ps.leaf.starBindings.topicStars.star(index).trim();
    }

    /**
     * return the client ID.
     * implements {@code <id/>}
     *
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         client ID
     */

    private static String id(Node node, ParseState ps) {
        return ps.chatSession.sessionId;
    }
    /**
     * return the size of the robot brain (number of AIML categories loaded).
     * implements {@code <size/>}
     *
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         bot brain size
     */
    private static String size(Node node, ParseState ps) {
        int size = ps.chatSession.bot.brain.getCategories().size();
        return String.valueOf(size);
    }
    /**
     * return the size of the robot vocabulary (number of words the bot can recognize).
     * implements {@code <vocabulary/>}
     *
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         bot vocabulary size
     */
    private static String vocabulary(Node node, ParseState ps) {
        int size = ps.chatSession.bot.brain.getVocabulary().size();
        return String.valueOf(size);
    }
    /**
     * return a string indicating the name and version of the AIML program.
     * implements {@code <program/>}
     *
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         AIML program name and version.
     */
    private static String program(Node node, ParseState ps) {
        return MagicStrings.programNameVersion;
    }

    /**
     * implements the (template-side) {@code <that index="M,N"/>}    tag.
     * returns a normalized sentence.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the nth last sentence of the bot's mth last reply.
     */
    private static String that(Node node, ParseState ps) {
        int index=0;
        int jndex=0;
        String value = getAttributeOrTagValue(node, ps, "index");
        if (value != null)
            try {
                String[] spair = value.split(",");
                index = Integer.parseInt(spair[0])-1;
                jndex = Integer.parseInt(spair[1])-1;
                log.info("That index={},{}", index, jndex);
            } catch (Exception ex) { log.error("that Error", ex);; }
        String that = MagicStrings.unknown_history_item;
        History hist = ps.chatSession.thatHistory.get(index);
        if (hist != null) that = (String)hist.get(jndex);
        return that.trim();
    }

    /**
     * implements {@code <input index="N"/>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the nth last sentence input to the bot
     */

    private static String input(Node node, ParseState ps) {
        int index=getIndexValue(node, ps);
        return ps.chatSession.inputHistory.getString(index);
    }
    /**
     * implements {@code <request index="N"/>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the nth last multi-sentence request to the bot.
     */
    private static String request(Node node, ParseState ps) {             // AIML 2.0
        int index=getIndexValue(node, ps);
        return ps.chatSession.requestHistory.getString(index).trim();
    }
    /**
     * implements {@code <response index="N"/>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the bot's Nth last multi-sentence response.
     */
    private static String response(Node node, ParseState ps) {            // AIML 2.0
        int index=getIndexValue(node, ps);
        return ps.chatSession.responseHistory.getString(index).trim();
    }
    /**
     * implements {@code <system>} tag.
     * Evaluate the contents, and try to execute the result as
     * a command in the underlying OS shell.
     * Read back and return the result of this command.
     *
     * The timeout parameter allows the botmaster to set a timeout
     * in ms, so that the <system></system>   command returns eventually.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         the result of executing the system command or a string indicating the command failed.
     */
    private static String system(Node node, ParseState ps) {
        HashSet<String> attributeNames = Utilities.stringSet("timeout");
        //String stimeout = getAttributeOrTagValue(node, ps, "timeout");
        String evaluatedContents = evalTagContent(node, ps, attributeNames);
		String result = IOUtils.system(evaluatedContents, MagicStrings.system_failed);
		return result;
    }
    /**
     * implements {@code <think>} tag
     *
     * Evaluate the tag contents but return a blank.
     * "Think but don't speak."
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return        a blank empty string
     */
    private static String think(Node node, ParseState ps) {
        evalTagContent(node, ps, null);
        return "";
    }

    /**
     * Transform a string of words (separtaed by spaces) into
     * a string of individual characters (separated by spaces).
     * Explode "ABC DEF" = "A B C D E F".
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         exploded string
     */
    private static String explode(Node node, ParseState ps) {              // AIML 2.0
        String result = evalTagContent(node, ps, null);
        return explode(result);
    }
    /**
     * apply the AIML normalization pre-processor to the evaluated tag contenst.
     * implements {@code <normalize>} tag.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         normalized string
     */
    private static String normalize(Node node, ParseState ps) {            // AIML 2.0
        String result = evalTagContent(node, ps, null);
        return ps.chatSession.bot.preProcessor.normalize(result);
    }
    /**
     * apply the AIML denormalization pre-processor to the evaluated tag contenst.
     * implements {@code <normalize>} tag.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         denormalized string
     */
    private static String denormalize(Node node, ParseState ps) {            // AIML 2.0
        String result = evalTagContent(node, ps, null);
        return ps.chatSession.bot.preProcessor.denormalize(result);
    }
    /**
     * evaluate tag contents and return result in upper case
     * implements {@code <uppercase>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         uppercase string
     */
    private static String uppercase(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        return result.toUpperCase();
    }
    /**
     * evaluate tag contents and return result in lower case
     * implements {@code <lowercase>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         lowercase string
     */
    private static String lowercase(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        return result.toLowerCase();
    }
    /**
     * evaluate tag contents and capitalize each word.
     * implements {@code <formal>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         capitalized string
     */
     private static String formal(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        return capitalizeString(result);
    }
    /**
     * evaluate tag contents and capitalize the first word.
     * implements {@code <sentence>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         string with first word capitalized
     */
    private static String sentence(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        if (result.length() > 1) return result.substring(0, 1).toUpperCase()+result.substring(1, result.length());
        else return "";
    }
    /**
     * evaluate tag contents and swap 1st and 2nd person pronouns
     * implements {@code <person>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         sentence with pronouns swapped
     */
    private static String person(Node node, ParseState ps) {
        String result;
        if (node.hasChildNodes())
          result = evalTagContent(node, ps, null);
        else result = ps.leaf.starBindings.inputStars.star(0);   // for <person/>
        result = " "+result+" ";
        result = ps.chatSession.bot.preProcessor.person(result);
        return result.trim();
    }
    /**
     * evaluate tag contents and swap 1st and 3rd person pronouns
     * implements {@code <person2>} tag
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         sentence with pronouns swapped
     */
    private static String person2(Node node, ParseState ps) {
        String result;
        if (node.hasChildNodes())
            result = evalTagContent(node, ps, null);
        else result = ps.leaf.starBindings.inputStars.star(0);   // for <person2/>
        result = " "+result+" ";
        result = ps.chatSession.bot.preProcessor.person2(result);
        return result.trim();
    }
    /**
     * implements {@code <gender>} tag
     * swaps gender pronouns
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         sentence with gender ronouns swapped
     */
    private static String gender(Node node, ParseState ps) {
        String result = evalTagContent(node, ps, null);
        result = " "+result+" ";
        result = ps.chatSession.bot.preProcessor.gender(result);
        return result.trim();
    }

    /**
     * implements {@code <random>} tag
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         response randomly selected from the list
     */
    private static String random(Node node, ParseState ps) {
        NodeList childList = node.getChildNodes();
        ArrayList<Node> liList = new ArrayList<Node>();
        for (int i = 0; i < childList.getLength(); i++)
            if (childList.item(i).getNodeName().equals("li")) liList.add(childList.item(i));
        return evalTagContent(liList.get((int) (Math.random() * liList.size())), ps, null);
    }
    private static String unevaluatedAIML(Node node, ParseState ps) {
        String result = learnEvalTagContent(node, ps);
        return unevaluatedXML(result, node, ps);
    }

    private static String recursLearn(Node node, ParseState ps) {
        String nodeName = node.getNodeName();
        if (nodeName.equals("#text")) return node.getNodeValue();
        else if (nodeName.equals("eval")) return evalTagContent(node, ps, null);                // AIML 2.0
        else return unevaluatedAIML(node, ps);
    }
    private static String learnEvalTagContent(Node node, ParseState ps) {
        StringBuilder result = new StringBuilder();
        NodeList childList = node.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node child = childList.item(i);
            result.append(recursLearn(child, ps));
        }
        return result.toString();
    }

    private static String learn(Node node, ParseState ps)   {                 // learn, learnf AIML 2.0
        NodeList childList = node.getChildNodes();
        String pattern = "";
        String that="*";
        String template = "";
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeName().equals("category")) {
                NodeList grandChildList = childList.item(i).getChildNodes();
                for (int j = 0; j < grandChildList.getLength(); j++)  {
                    if (grandChildList.item(j).getNodeName().equals("pattern")) {
                        pattern = recursLearn(grandChildList.item(j), ps);
                    }
                    else if (grandChildList.item(j).getNodeName().equals("that")) {
                        that = recursLearn(grandChildList.item(j), ps);
                    }
                    else if (grandChildList.item(j).getNodeName().equals("template")) {
                        template = recursLearn(grandChildList.item(j), ps);
                    }
                }
                pattern = pattern.substring("<pattern>".length(),pattern.length()-"</pattern>".length());
                //log.info("Learn Pattern = "+pattern);
                if (template.length() >= "<template></template>".length()) template = template.substring("<template>".length(),template.length()-"</template>".length());
                if (that.length() >= "<that></that>".length()) that = that.substring("<that>".length(),that.length()-"</that>".length());
                pattern = pattern.toUpperCase();
                that = that.toUpperCase();
                if (MagicBooleans.trace_mode) {
                    log.info("Learn Pattern = "+pattern);
                    log.info("Learn That = "+that);
                    log.info("Learn Template = "+template);
                }
                Category c;
                if (node.getNodeName().equals("learn"))
                    c = new Category(0, pattern, that, "*", template, MagicStrings.null_aiml_file);
                else {// learnf
                    c = new Category(0, pattern, that, "*", template, MagicStrings.learnf_aiml_file);
                    //ps.chatSession.bot.learnfCategories.add(c);
                    ps.chatSession.bot.learnfGraph.addCategory(c);
                    //ps.chatSession.bot.categories.add(c);
                }
                ps.chatSession.bot.brain.addCategory(c);
                  //ps.chatSession.bot.brain.printgraph();
            }
        }
        return "";
    }

    /**
     * implements {@code <condition> with <loop/>}
     * re-evaluate the conditional statement until the response does not contain {@code <loop/>}
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         result of conditional expression
     */
    private static String loopCondition(Node node, ParseState ps) {
        boolean loop = true;
        StringBuilder result= new StringBuilder();
        int loopCnt = 0;
        while (loop && loopCnt < MagicNumbers.max_loops) {

            String loopResult = condition(node, ps);
            if (loopResult.trim().equals(MagicStrings.too_much_recursion())) return MagicStrings.too_much_recursion();
            if (loopResult.contains("<loop/>")) {
                loopResult = loopResult.replace("<loop/>","");
                loop = true;
            }
            else loop = false;
            result.append(loopResult);
            loopCnt++;
        }
        if (loopCnt >= MagicNumbers.max_loops) result = new StringBuilder(MagicStrings.too_much_looping());
        return result.toString();
    }
    /**
     * implements {@code <comparecondition> with <loop/>}
     * re-evaluate the compare conditional statement until the response does not contain {@code <loop/>}
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         result of conditional expression
     */
    private static String loopCompareCondition(Node node, ParseState ps) {
        boolean loop = true;
        
        String minAccuracy = getAttributeOrTagValue(node, ps, "minaccuracy");        
        int min = 90;         
        try {
            min = Integer.parseInt(minAccuracy); 
        } catch (Exception e) {
            log.error("compare parseInt Exception setting to default 90.");
            min = 90; 
        }    
        
        StringBuilder result= new StringBuilder();
        int loopCnt = 0;
        while (loop && loopCnt < MagicNumbers.max_loops) {
            String loopResult = compareCondition(node, ps, min);
            if (loopResult.trim().equals(MagicStrings.too_much_recursion())) return MagicStrings.too_much_recursion();
            if (loopResult.contains("<loop/>")) {
                loopResult = loopResult.replace("<loop/>","");
                loop = true;
            }
            else loop = false;
            result.append(loopResult);
            loopCnt++;
        }
        if (loopCnt >= MagicNumbers.max_loops) result = new StringBuilder(MagicStrings.too_much_looping());
        return result.toString();
    }

    /**
     * implements all 3 forms of the {@code <condition> tag}
     * In AIML 2.0 the conditional may return a {@code <loop/>}
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         result of conditional expression
     */
    private static String condition(Node node, ParseState ps) {
        String result="";
        //boolean loop = true;
        NodeList childList = node.getChildNodes();
        ArrayList<Node> liList = new ArrayList<Node>();
        String predicate=null, varName=null, value=null; //Node p=null, v=null;
        HashSet<String> attributeNames = Utilities.stringSet("name", "var", "value");
        // First check if the <condition> has an attribute "name".  If so, get the predicate name.
        predicate = getAttributeOrTagValue(node, ps, "name");
        varName = getAttributeOrTagValue(node, ps, "var");
        // Make a list of all the <li> child nodes:
        for (int i = 0; i < childList.getLength(); i++)
            if (childList.item(i).getNodeName().equals("li")) liList.add(childList.item(i));
        // if there are no <li> nodes, this is a one-shot condition.
        if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null   &&
                   predicate != null  &&
                   ps.chatSession.predicates.get(predicate).equals(value))  {
                   return evalTagContent(node, ps, attributeNames);
        }
        else if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null   &&
                varName != null  &&
                ps.vars.get(varName).equals(value))  {
            return evalTagContent(node, ps, attributeNames);
        }
        // otherwise this is a <condition> with <li> items:
        else for (int i = 0; i < liList.size() && result.equals(""); i++) {
            Node n = liList.get(i);
            String liPredicate = predicate;
            String liVarName = varName;
            if (liPredicate == null) liPredicate = getAttributeOrTagValue(n, ps, "name");
            if (liVarName == null) liVarName = getAttributeOrTagValue(n, ps, "var");
            value = getAttributeOrTagValue(n, ps, "value");
            //log.info("condition name="+liPredicate+" value="+value);
            if (value != null) {
                // if the predicate equals the value, return the <li> item.
                if (liPredicate != null && value != null && (ps.chatSession.predicates.get(liPredicate).equals(value) ||
                        (ps.chatSession.predicates.containsKey(liPredicate) && value.equals("*"))))
                    return evalTagContent(n, ps, attributeNames);
                else if (liVarName != null && value != null && (ps.vars.get(liVarName).equals(value) ||
                        (ps.vars.containsKey(liPredicate) && value.equals("*"))))
                    return evalTagContent(n, ps, attributeNames);
            }
            else  // this is a terminal <li> with no predicate or value, i.e. the default condition.
                return evalTagContent(n, ps, attributeNames);
        }
        return "";

    }
    /**
     * implements all 3 forms of the {@code <comparecondition> tag}
     * In AIML 2.0 the conditional may return a {@code <loop/>}
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     * @return         result of conditional expression
     */
    private static String compareCondition(Node node, ParseState ps, int minAccuracy) {
        String result="";
                        
        NodeList childList = node.getChildNodes();
        ArrayList<Node> liList = new ArrayList<Node>();
        String predicate=null, varName=null, value=null; //Node p=null, v=null;
        HashSet<String> attributeNames = Utilities.stringSet("name", "var", "value");
        // First check if the <condition> has an attribute "name".  If so, get the predicate name.
        predicate = getAttributeOrTagValue(node, ps, "name");
        varName = getAttributeOrTagValue(node, ps, "var");
        // Make a list of all the <li> child nodes:
        for (int i = 0; i < childList.getLength(); i++)
            if (childList.item(i).getNodeName().equals("li")) liList.add(childList.item(i));
        // if there are no <li> nodes, this is a one-shot condition.
        if (liList.isEmpty() && (value = getAttributeOrTagValue(node, ps, "value")) != null   &&
                   predicate != null  &&
                   ps.chatSession.predicates.get(predicate).equals(value))  {
                   return evalTagContent(node, ps, attributeNames);
        }
        else if (liList.isEmpty() && (value = getAttributeOrTagValue(node, ps, "value")) != null   &&
                varName != null  &&
                ps.vars.get(varName).equals(value))  {
            return evalTagContent(node, ps, attributeNames);
        }
        // otherwise this is a <condition> with <li> items:
        else for (int i = 0; i < liList.size() && result.equals(""); i++) {
            Node n = liList.get(i);
            String liPredicate = predicate;
            String liVarName = varName;
            if (liPredicate == null) liPredicate = getAttributeOrTagValue(n, ps, "name");
            if (liVarName == null) liVarName = getAttributeOrTagValue(n, ps, "var");
            value = getAttributeOrTagValue(n, ps, "value");
            
            if (value != null) {
                // if the predicate equals the value, return the <li> item.
                int compPredicate = (int)(Validator.compareWords(ps.chatSession.predicates.get(liPredicate), value) * 100);
                int compVarname = (int)(Validator.compareWords(ps.vars.get(liVarName), value) * 100);
                
                log.info("condition name="+liPredicate+" value="+value + " compPredicate: " + compPredicate + " compVarname: " + compVarname);
                
                if (liPredicate != null && (compPredicate >= minAccuracy ||
                        (ps.chatSession.predicates.containsKey(liPredicate) && value.equals("*"))))
                    return evalTagContent(n, ps, attributeNames);
                else if (liVarName != null && (compVarname >= minAccuracy ||
                        (ps.vars.containsKey(liPredicate) && value.equals("*"))))
                    return evalTagContent(n, ps, attributeNames);
            }
            else  // this is a terminal <li> with no predicate or value, i.e. the default condition.
                return evalTagContent(n, ps, attributeNames);
        }
        return "";

    }

    /**
     * check to see if a result contains a {@code <loop/>} tag.
     *
     * @param node     current XML parse node
     * @return         true or false
     */
    public static boolean evalTagForLoop(Node node) {
        NodeList childList = node.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++)
            if (childList.item(i).getNodeName().equals("loop")) return true;
        return false;
    }

    /**
     * Recursively descend the XML DOM tree, evaluating AIML and building a response.
     *
     * @param node     current XML parse node
     * @param ps       AIML parse state
     */


    private static String recursEval(Node node, ParseState ps) {
        try 
        {
            String nodeName = node.getNodeName();
            if (nodeName.equals("#text")) 
                return node.getNodeValue();
            else if (nodeName.equals("#comment")) {                
                return "";
            }
            else if (nodeName.equals("template"))
                return evalTagContent(node, ps, null);
            else if (nodeName.equals("random"))
                return random(node, ps);
            else if (nodeName.equals("condition"))
                return loopCondition(node, ps);
            else if (nodeName.equals("srai"))
                return srai(node, ps);
            else if (nodeName.equals("sr"))
                  return respond(ps.leaf.starBindings.inputStars.star(0), ps.that, ps.topic, ps.chatSession, sraiCount);
//            else if (nodeName.equals("sraix"))
//                return sraix(node, ps);
            else if (nodeName.equals("set"))
                return set(node, ps);
            else if (nodeName.equals("get"))
                return get(node, ps);       
            else if (nodeName.equals("map"))  // AIML 2.0 -- see also <set> in pattern
                return map(node, ps);
            else if (nodeName.equals("bot"))
                return bot(node, ps);
            else if (nodeName.equals("id"))
                return id(node, ps);
            else if (nodeName.equals("size"))
                return size(node, ps);
            else if (nodeName.equals("vocabulary")) // AIML 2.0
                return vocabulary(node, ps);
            else if (nodeName.equals("program"))
                return program(node, ps);
            
                
            //sprint modyfikcation start
            else if(nodeName.equals("getall"))
                return getall(node, ps); //sprint
            else if (nodeName.equals("date")) //sprint
                return date(node, ps);
            else if (nodeName.equals("plugin")) //sprint
                return plugin(node, ps);
            else if (nodeName.equals("gpt")) //sprint
                return gpt(node, ps);
            else if (nodeName.equals("ollama")) //sprint
                return ollama(node, ps);
            else if (nodeName.equals("gemini")) //sprint
                return gemini(node, ps);
            else if (nodeName.equals("save-context")) //sprint
                return saveContext(node, ps);
            else if (nodeName.equals("predictf")) //sprint
                return ml(node, ps);
            else if (nodeName.equals("ml")) //sprint
                return ml(node, ps);
            else if (nodeName.equals("regex")) //sprint
                return regex(node, ps);
            else if (nodeName.equals("pesel")) //sprint NEW
                return pesel(node, ps);
            
            else if (nodeName.equals("nip")) //sprint NEW
                return nip(node, ps);
            else if (nodeName.equals("compare")) //sprint NEW
                return compare(node, ps);
            else if (nodeName.equals("nums")) //sprint NEW
                return nums(node, ps);
            else if (nodeName.equals("phone")) //sprint NEW
                return phone(node, ps);
            else if (nodeName.equals("bankaccount")) //sprint NEW
                return bankAccount(node, ps);
            else if (nodeName.equals("report-save")) //sprint NEW
                return reportSave(node, ps);
            else if (nodeName.equals("sexpesel")) //sprint NEW
                return sexpesel(node, ps);
            else if (nodeName.equals("datetext")) //sprint NEW
                return txt2date(node, ps);
            else if (nodeName.equals("txt2date")) //sprint NEW
                return txt2date(node, ps);
            else if (nodeName.equals("birthpesel")) //sprint NEW
                return birtPesel(node, ps);
            else if (nodeName.equals("math")) //sprint NEW
                return math(node, ps);
            else if (nodeName.equals("compare-condition"))
                return loopCompareCondition(node, ps);
            else if (nodeName.equals("increment"))
                return increment(node, ps);
            else if (nodeName.equals("decrement"))
                return decrement(node, ps);
            else if (nodeName.equals("lessthan"))
                return lessthan(node, ps);
            else if (nodeName.equals("greaterthan"))
                return greaterthan(node, ps);
            else if (nodeName.equals("currency"))
                return currency(node, ps);
            else if (nodeName.equals("txt2num"))
                return txt2num(node, ps);
            else if (nodeName.equals("txt2dec"))
                return txt2dec(node, ps);
            else if (nodeName.equals("num2txt"))
                return num2txt(node, ps);
            else if (nodeName.equals("txt2time"))
                return txt2time(node, ps);
            else if (nodeName.equals("txt2datetime"))
                return txt2datetime(node, ps);
           else if (nodeName.equals("implode"))
                return implode(node, ps);
           else if (nodeName.equals("setall"))
                return setall(node, ps);
           else if (nodeName.equals("zip"))
                return zip(node, ps);
           else if (nodeName.equals("dateadd"))
                return dateadd(node, ps);
           else if (nodeName.equals("getrecord"))
                return getRecord(node, ps);
           else if (nodeName.equals("updaterecord"))
                return updateRecord(node, ps);
           else if (nodeName.equals("getrecordstatus"))
                return getRecordStatus(node, ps);
           else if (nodeName.equals("updaterecordstatus"))
                return updateRecordStatus(node, ps);
           else if (nodeName.equals("getdata"))
                return getData(node, ps);
           else if (nodeName.equals("setdata"))
                return setData(node, ps);
           
           
           
           
            //sprint modyfikcation stop
            else if (nodeName.equals("interval"))
                return interval(node, ps);
            //else if (nodeName.equals("gossip"))       // removed from AIML 2.0
            //    return gossip(node, ps);
            else if (nodeName.equals("think"))
                return think(node, ps);
            else if (nodeName.equals("system"))
                return system(node, ps);
            else if (nodeName.equals("explode"))
                return explode(node, ps);
            else if (nodeName.equals("normalize"))
                return normalize(node, ps);
            else if (nodeName.equals("denormalize"))
                return denormalize(node, ps);
            else if (nodeName.equals("uppercase"))
                return uppercase(node, ps);
            else if (nodeName.equals("lowercase"))
                return lowercase(node, ps);
            else if (nodeName.equals("formal"))
                return formal(node, ps);
            else if (nodeName.equals("sentence"))
                return sentence(node, ps);
            else if (nodeName.equals("person"))
                return person(node, ps);
            else if (nodeName.equals("person2"))
                return person2(node, ps);
            else if (nodeName.equals("gender"))
                return gender(node, ps);
            else if (nodeName.equals("star"))
                return inputStar(node, ps);
            else if (nodeName.equals("thatstar"))
                return thatStar(node, ps);
            else if (nodeName.equals("topicstar"))
                return topicStar(node, ps);
            else if (nodeName.equals("that"))
                return that(node, ps);
            else if (nodeName.equals("input"))
                return input(node, ps);
            else if (nodeName.equals("request"))
                return request(node, ps);
            else if (nodeName.equals("response"))
                return response(node, ps);
            else if (nodeName.equals("learn") || nodeName.equals("learnf"))
                return learn(node, ps);
            else if (extension != null && extension.extensionTagSet().contains(nodeName)) return extension.recursEval(node, ps) ;
            else return (genericXML(node, ps));
        } catch (Exception ex) {
            log.error("recurseEval Error", ex);
            return "ERR " + ex.getMessage();
        }
    }

    /**
     * evaluate an AIML template expression
     *
     * @param template      AIML template contents
     * @param ps            AIML Parse state
     * @return              result of evaluating template.
     */
    private static String evalTemplate(String template, ParseState ps) {
        String response = MagicStrings.template_failed;
        try {
            template = "<template>"+template+"</template>";
            Node root = DomUtils.parseString(template);
            response = recursEval(root, ps);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    /**
     * check to see if a template is a valid XML expression.
     *
     * @param template      AIML template contents
     * @return              true or false.
     */
    public static boolean validTemplate(String template) {
        try {
            template = "<template>"+template+"</template>";
            DomUtils.parseString(template);
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            log.info("Invalid Template "+template);
            return false;
        }

    }

}



