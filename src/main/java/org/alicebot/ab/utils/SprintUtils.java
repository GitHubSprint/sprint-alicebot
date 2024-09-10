/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alicebot.ab.utils;

import com.mayabot.nlp.fasttext.FastText;
import com.mayabot.nlp.fasttext.ScoreLabelPair;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom methods for polish language and jar plugin call. 
 * @author skost
 */
public class SprintUtils {
    private static Map<String, Class<?>> classCache = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SprintUtils.class);
   

    public static Map<String, FastText> mlModels;
    /**
     * Replace polish marks in string.
     * @param src
     * @param isPolishMarks
     * @return 
     */
    @Deprecated
    public static String unaccent(String src, boolean isPolishMarks) {
        String temp = src;
        if(src==null)
            return null;
        if(!isPolishMarks)
            temp = Normalizer.normalize(src.replaceAll("[łŁ]", "l"), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");   

        return temp;
    }



    public static boolean updateMlModel(String model) {

        FastText fastText = mlModels.get(model);
        if(fastText != null) {
            log.info("updateMlModel. Model " + model + " removed from system.");
            mlModels.remove(model);
        }

        String path = "models/" + model + ".bin";

        File file = new File(path);

        if(!file.exists()) {
            log.error("updateMlModel. Model {} not exists. Path: {}", model, file.getAbsolutePath());
            return false;
        }
        try {
            fastText = FastText.Companion.loadModelFromSingleFile(file);
        } catch (Exception ex) {
            log.error("updateMlModel ERROR : {}", ex, ex);
            return false;
        }

        mlModels.put(model, fastText);

        return true;
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
    public static String ml(String model, String nBest, String threshold, String score, String parameter, String sessionId) {
        String out = "ERR";

        try {
            int best = Integer.parseInt(nBest);
            float fThreshold = Float.parseFloat(threshold);
            int iMinScore = Integer.parseInt(score);


            log.info("ml Request: sessionId: {} Model: {} Nbest: {} Threshold: {} MinScore: {} parameter: {}", sessionId, model, best, fThreshold, iMinScore, parameter);

            FastText fastText = mlModels.get(model);

            if(fastText == null) {
                log.warn("{}\tML Invalid model name", sessionId);
                return "ERR Invalid model name";
            }

            List<ScoreLabelPair> result = fastText.predict(Arrays.asList(parameter.split(" ")), best, fThreshold);

            log.info("ml Response sessionId: {} result.size: {}", sessionId, result.size());
            for(ScoreLabelPair pair : result) {
                int iScore = (int) (pair.getScore() * 100);

                log.info("Response sessionId: {} parameter: {} RESPONSE score: {} iScore: {} label: {} MinScore: {}", sessionId, parameter, pair.getScore(), iScore, pair.getLabel(), iMinScore);

                if(iScore >= iMinScore) {
                    out = pair.getLabel() + " " + iScore;
                    break;
                }
                else
                    out= "__label__oos " + iScore;
            }

        } catch (Exception e) {

            out = "ERR " + e.getMessage();
            log.error("predictSupervisedModel ERROR", e);
        }

        return out;
    }


    public static void resetClassCache() {
        classCache.clear();
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
    public static String callPlugin(String file, String classLoad, String methodName, String parameter, String sessionId) {
        File f = new File(file);
        String out;

        try {
            Class<?> bean = classCache.get(classLoad);
            if (bean == null) {
                try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{f.toURI().toURL()},
                        SprintUtils.class.getClassLoader())) {

                    // Load the target class
                    Class<?> beanClass = urlClassLoader.loadClass(classLoad);

                    classCache.put(classLoad, beanClass);

                    Constructor<?> constructor = beanClass.getConstructor();
                    Object beanObj = constructor.newInstance();
                    Method method = beanClass.getMethod("processCustomResultPocessor", String.class, String.class, String.class);
                    out = (String) method.invoke(beanObj, sessionId, parameter, methodName);

                }
            } else {
                Constructor<?> constructor = bean.getConstructor();
                Object beanObj = constructor.newInstance();
                Method method = bean.getMethod("processCustomResultPocessor", String.class, String.class, String.class);
                out = (String) method.invoke(beanObj, sessionId, parameter, methodName);
                log.info("call from loaded class: {}", classLoad);
            }
        } catch (Exception ex) {
            out = "ERR " + ex.getMessage();
            log.error("callPlugin file: {} parameter : {} ERROR", f, parameter, ex);
        }
        log.info("{} : request parameter: {} method: {} plugin response: {}", sessionId, parameter, methodName, out);
        return out;
    }
    
    
    /**
     * Deprecated method to call bash script, changed to jar callPlugin.
     * @param scrip
     * @param parameters
     * @return 
     */
    @Deprecated
    public static String readBashScript(String scrip, String parameters) {
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
                log.error("readBashScript script: {} parameters : {} InterruptedException ERROR : {}", scrip, parameters, ex, ex);
            }
            while (read.ready()) {
                out = read.readLine();
            }
        } catch (IOException e) {
            log.error("readBashScript script: {} parameters : {} IOException ERROR : {}", scrip, parameters, e, e);
        }
        
        log.info("readBashScript: scrip = {} parameters = {} [response = {}]",scrip, parameters, out);
        return out;
    }
                
}
