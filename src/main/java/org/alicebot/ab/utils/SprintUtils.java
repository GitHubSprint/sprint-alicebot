/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alicebot.ab.utils;

import com.mayabot.nlp.fasttext.FastText;
import com.mayabot.nlp.fasttext.ScoreLabelPair;
import fasttext.FastTextPrediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Custom methods for polish language and jar plugin call. 
 * @author skost
 */
public class SprintUtils {
    private static final Logger log = LoggerFactory.getLogger(SprintUtils.class);
   
    public static Map<String, fasttext.FastText> mlaModels; 
    public static Map<String, FastText> mlModels;
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
            
        temp = temp.replaceAll("[?]", " ").replaceAll("[!]", " ").replaceAll("[.]", " ");
                             
        return temp;
    }



    public static boolean updateMlModel(String model)
    {

        FastText fastText = mlModels.get(model);
        if(fastText != null)
        {
            log.info("updateMlModel. Model " + model + " removed from system.");
            mlModels.remove(model);
        }

        String path = "models/" + model + ".bin";

        File file = new File(path);

        if(!file.exists())
        {
            log.error("updateMlModel. Model " + model + " not exists. Path: " + file.getAbsolutePath());
            return false;
        }
        try {
            fastText = FastText.Companion.loadModelFromSingleFile(file);
        }
        catch (Exception ex)
        {
            log.error("updateMlModel ERROR : " + ex, ex);
            return false;
        }

        mlModels.put(model, fastText);

        return true;
    }
            
    public static String mla(String model, String threshold, String score, String parameter, String sessionId)
    {

        String out;
        int iMinScore = 0;
        if(score != null && score.length() > 0)
            iMinScore = Integer.parseInt(score);

        float fThreshold = 0f;

        if(threshold != null && threshold.length() > 0)
            fThreshold = Float.parseFloat(threshold);

        log.info("mla Request: sessionId: " + sessionId + " Model: " + model + " MinScore: " + iMinScore + " Threshold: " + fThreshold + " parameter: " + parameter);


        try {

            fasttext.FastText ftmodel = mlaModels.get(model);

            if(ftmodel == null)
            {
                log.warn(sessionId + "\tInvalid model name");
                return "ERR Invalid model name";
            }

            List<FastTextPrediction> result = ftmodel.predictAll(Arrays.asList(parameter.split(" ")),fThreshold);

            int iScore = (int) (result.get(0).probability() * 100);

            if (!result.get(0).label().equals("__label__oos"))
            {
                log.info(sessionId + "\tOK:\t" + parameter + "\tresult : " + result.get(0).label() + " score: " + iScore);
            } else {
                log.warn(sessionId + "\tNO QUALIFICATION:\t" + parameter);
            }

            if(iScore >= iMinScore)
                out= result.get(0).label() + " " + iScore;
            else
                out = "__label__oos" + " " + iScore;



            //out= result.get(0).label() + " " + iScore;

        } catch (Exception e) {

            out = "ERR " + e.getMessage();
            log.error(sessionId + "\tpredictSupervisedModel ERROR : " + e, e);
        }

        return out;
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
    public static String ml(String model, String nBest, String threshold, String score, String parameter, String sessionId)
    {
        String out = "ERR";

        try {
            int iNbest = Integer.parseInt(nBest);
            float fThreshold = Float.parseFloat(threshold);
            int iMinScore = Integer.parseInt(score);


            log.info("ml Request: sessionId: " + sessionId + " Model: " + model + " Nbest: " + iNbest + " Threshold: " + fThreshold + " MinScore: " + iMinScore + " parameter: " + parameter);

            FastText fastText = mlModels.get(model);

            if(fastText == null)
            {
                log.warn(sessionId + "\tInvalid model name");
                return "ERR Invalid model name";
            }

            List<ScoreLabelPair> result = fastText.predict(Arrays.asList(parameter.split(" ")), iNbest, fThreshold);

            log.info("ml Response sessionId: " + sessionId + " result.size: " + result.size());
            for(ScoreLabelPair pair : result)
            {
                int iScore = (int) (pair.getScore() * 100);

                log.info("Response sessionId: " + sessionId + " parameter: " + parameter + " RESPONSE score: " + pair.getScore() + " iScore: " + iScore + " label: " + pair.getLabel() + " MinScore: " + iMinScore);

                if(iScore >= iMinScore)
                {
                    out = pair.getLabel() + " " + iScore;
                    break;
                }
                else
                    out= "__label__oos " + iScore;
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
        URLClassLoader urlClassLoader = null;
        //String f = "jar:file:///" + file + "!/";
        
        File f = new File(file);
        
        String out = "";
        try {
            
            urlClassLoader = new URLClassLoader(new URL[] {f.toURI().toURL()},
                                         SprintUtils.class.getClassLoader());
                                    
            //URL[] classLoaderUrls = new URL[]{new URL(f)};         
            // Create a new URLClassLoader 
            //urlClassLoader = new URLClassLoader(classLoaderUrls);

            // Load the target class
            Class<?> beanClass = urlClassLoader.loadClass(classLoad);

            // Create a new instance from the loaded class
            Constructor<?> constructor = beanClass.getConstructor();             

            Object beanObj = constructor.newInstance();        
            // Getting a method from the loaded class and invoke it
                        
            Method method = beanClass.getMethod("processCustomResultPocessor", String.class, String.class, String.class);    
            String response = (String) method.invoke(beanObj, sessionId, parameter, methodName);
            log.info("Request: sessionId: " + sessionId + " parameter: " + parameter + " method: " + methodName + " plugin response: " + response);
            
            out = response;                                    
        } 
        catch (Exception e) 
        {
            out = "ERR " + e.getMessage();
            log.error("callPlugin file: " + f + " parameter : " +parameter + " ERROR : " + e, e);
        }
        finally {
            if(urlClassLoader != null)
                try {
                    urlClassLoader.close();
                } catch (IOException ex) {
                    out = "ERR " + ex.getMessage();
                    log.error("callPlugin urlClassLoader.close() file: " + f + " parameter : " +parameter + " ERROR : " + ex, ex);
                }                       
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
