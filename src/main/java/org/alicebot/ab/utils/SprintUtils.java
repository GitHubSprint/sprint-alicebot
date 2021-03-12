/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alicebot.ab.utils;

import com.mayabot.nlp.fasttext.FastText;
import com.mayabot.nlp.fasttext.ScoreLabelPair;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom methods for polish language and jar plugin call. 
 * @author skost
 */
public class SprintUtils {
    private static final Logger log = LoggerFactory.getLogger(SprintUtils.class);
    
    /**
     * Replace polish marks in string.
     * @param src
     * @param isPolishMarks
     * @return 
     */
    public static String unaccent(String src, boolean isPolishMarks) 
    {
        String temp = src;
        if(src==null)
            return null;
        
        
        if(!isPolishMarks)
        {            
            temp = Normalizer.normalize(src.replaceAll("[łŁ]", "l"), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");   
        }
            
        temp = temp.replaceAll("[?]", "").replaceAll("[!]", "").replaceAll("[.]", "");
                             
        return temp;
    }  
    
    /**
     * Predict fastText label tranined suprvised model
     * @param model name of model (all models should be installed to ./models/
     * @param nBest number of responses
     * @param threshold
     * @param score score percent (e.g. 50 = 50% prediction) 
     * @param parameter 
     * @param sessionId Bot SessionId
     * @return 
     */
    public static String predictSupervisedModel(String model, String nBest, String threshold, String score, String parameter, String sessionId)
    {
        String out = "ERR Null";
        
        
        try {                                    
            String path = new File(".").getCanonicalPath().replace("\\", "/");            
            String sModel = path + "/models/" + model;
            int iNbest = Integer.parseInt(nBest); 
            float fThreshold = Float.parseFloat(threshold);
            int iMinScore = Integer.parseInt(score);
            
            
            log.info("Request: sessionId: " + sessionId + " Model: " + sModel + " Nbest: " + iNbest + " Threshold: " + fThreshold + " MinScore: " + iMinScore + " parameter: " + parameter);
            
            FastText fastText = FastText.Companion.loadModel(new File(sModel), true); 
            List<ScoreLabelPair> result = fastText.predict(Arrays.asList(parameter.split(" ")), iNbest, fThreshold);
                  
            log.info("Response sessionId: " + sessionId + " result.size: " + result.size());
            for(ScoreLabelPair pair : result)
            {
                int iScore = (int) (pair.getScore() * 100); 
                
                log.info("Response sessionId: " + sessionId + " parameter: " + parameter + " RESPONSE score: " + pair.getScore() + " iScore: " + iScore + " label: " + pair.getLabel() + " MinScore: " + iMinScore);
                
                if(iScore >= iMinScore)
                {                    
                    out= pair.getLabel() + " " + iScore; 
                    break;
                }
                else
                    out= parameter + " " + iScore;                     
            }                        
            
        } catch (Exception e) {
            
            out = "ERR " + e.getMessage();
            log.error("predictSupervisedModel ERROR : " + e, e);
        }
        
        return out;
    }
    
    
    /**
     * Java jar integration method. 
     * @param file jar file url
     * @param classLoad class name
     * @param methodName method name
     * @param parameter parameter to send 
     * @param sessionId sessionid
     * @return plugin reponse
     */
    public static String callPlugin(String file, String classLoad, String methodName, String parameter, String sessionId)
    {
        //"pl.sprint.chatbot.ext.Test"
        
        //log.info("file: " + file + " classLoad: " + classLoad + " methodName: " + methodName + " parameters: " + Arrays.toString(parameters));
        String f = "jar:file:///" + file + "!/";
        String out = "";
        try {
            
                                    
            URL[] classLoaderUrls = new URL[]{new URL(f)};         
            // Create a new URLClassLoader 
            URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);

            // Load the target class
            Class<?> beanClass = urlClassLoader.loadClass(classLoad);

            // Create a new instance from the loaded class
            Constructor<?> constructor = beanClass.getConstructor();             

            Object beanObj = constructor.newInstance();        
            // Getting a method from the loaded class and invoke it
            
            // public String processCustomResultPocessor(String session, String parameter, String method);
            Method method = beanClass.getMethod("processCustomResultPocessor", String.class, String.class, String.class);    
            String response = (String) method.invoke(beanObj, sessionId, parameter, methodName);
            log.info("Request: sessionId: " + sessionId + " parameter: " + parameter + " method: " + methodName + " plugin response: " + response);
            
            out = response;
            
        } catch (Exception e) 
        {
            out = "ERR " + e.getMessage();
            log.error("callPlugin file: " + f + " parameter : " +parameter + " ERROR : " + e, e);
        }
        
        return out;
    }
    
    
    /**
     * Deprecated method to call bash script, changed to jar callPlugin.
     * @param scrip
     * @param parameters
     * @return 
     */
    @Deprecated
    public static String readBashScript(String scrip, String parameters)
    {                
        String out = "";
        
        if (scrip == null)
            return out;
          
        
        if(System.getProperty("os.name").equals("Linux"))
            scrip+=".sh";
        else
            scrip+=".bat";
        
        try {
            Process proc = Runtime.getRuntime().exec("external/" + scrip + " " + parameters); 
            BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                proc.waitFor();
            } catch (InterruptedException ex) {
                log.error("readBashScript script: " + scrip + " parameters : " +parameters + " InterruptedException ERROR : " + ex, ex); 
            }
            while (read.ready()) {
                out = read.readLine();
            }
        } catch (IOException e) {
            log.error("readBashScript script: " + scrip + " parameters : " +parameters + " IOException ERROR : " + e, e); 
        }
        
        log.info("readBashScript: scrip = {} parameters = {} [response = {}]",scrip, parameters, out);
        return out;
    }
}
