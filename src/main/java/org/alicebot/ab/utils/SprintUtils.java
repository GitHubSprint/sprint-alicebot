/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alicebot.ab.utils;

import com.mayabot.nlp.fasttext.FastText;
import com.mayabot.nlp.fasttext.ScoreLabelPair;
import org.alicebot.ab.llm.LLMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom methods for polish language and jar plugin call. 
 * @author skost
 */
public class SprintUtils {
    private static final Map<File, SharedClassLoader> sharedClassLoaderMap = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SprintUtils.class);


    public static void updateGeminiToken(Map<String, String> tokens) {
        LLMConfiguration.geminiTokens = new HashMap<>(tokens);
    }

    public static void updateLLMConfiguration(Map<String, String> gptTokens,
                                              String gptApiUrl,
                                              String ollamaApiUrl,
                                              Map<String, String> geminiTokens,
                                              String geminiApiUrl,
                                              String gptDefaultModel,
                                              String ollamaDefaultModel,
                                              int gptMaxHistory,
                                              int ollamaMaxHistory,
                                              int geminiMaxHistory)
    {
        LLMConfiguration.gptTokens = new HashMap<>(gptTokens);
        LLMConfiguration.gptApiUrl = gptApiUrl;
        LLMConfiguration.ollamaApiUrl = ollamaApiUrl;
        LLMConfiguration.geminiTokens = new HashMap<>(geminiTokens);
        LLMConfiguration.geminiApiUrl = geminiApiUrl;

        LLMConfiguration.gptDefaultModel = gptDefaultModel;
        LLMConfiguration.ollamaDefaultModel = ollamaDefaultModel;
        LLMConfiguration.gptMaxHistory = gptMaxHistory;
        LLMConfiguration.ollamaMaxHistory = ollamaMaxHistory;
        LLMConfiguration.geminiMaxHistory = geminiMaxHistory;

        log.info("updateLLMConfiguration gptApiUrl: {} ollamaApiUrl: {}  geminiApiUrl: {}", LLMConfiguration.gptApiUrl, LLMConfiguration.ollamaApiUrl, LLMConfiguration.geminiApiUrl);
        log.info("updateLLMConfiguration gptDefaultModel: {} ollamaDefaultModel: {}", LLMConfiguration.gptDefaultModel, LLMConfiguration.ollamaDefaultModel);
        log.info("updateLLMConfiguration gptMaxHistory: {} ollamaMaxHistory: {} geminiMaxHistory: {}", LLMConfiguration.gptMaxHistory, LLMConfiguration.ollamaMaxHistory, LLMConfiguration.geminiMaxHistory);
    }


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
            log.info("updateMlModel. Model {} removed from system.", model);
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
        StringBuilder out = new StringBuilder();

        try {

            int iMinScore = 0;
            if(score != null)
                iMinScore = Integer.parseInt(score);

            int best = 1;
            if(nBest != null)
                best = Integer.parseInt(nBest);

            float fThreshold = 0;
            if(threshold != null)
                fThreshold = Float.parseFloat(threshold);

            if(fThreshold == 0f) {
                fThreshold = iMinScore/100f;
            }

            log.info("ml Request: sessionId: {} Model: {} Nbest: {} Threshold: {} MinScore: {} parameter: {}", sessionId, model, best, fThreshold, iMinScore, parameter);

            FastText fastText = mlModels.get(model);

            if(fastText == null) {
                log.warn("{}\tML Invalid model name", sessionId);
                return "ERR Invalid model name";
            }

            List<ScoreLabelPair> result = fastText.predict(Arrays.asList(parameter.split(" ")), best, fThreshold);

            log.info("ml Response sessionId: {} result.size: {}", sessionId, result.size());

            for(int i = 0; i < best; i++) {
                int iScore = 0;
                if (i < result.size()) {
                    ScoreLabelPair pair = result.get(i);
                    iScore = (int) (pair.getScore() * 100);
                    log.info("Response sessionId: {} parameter: {} RESPONSE[{}] score: {} iScore: {} label: {} threshold: {}", sessionId, parameter, i, pair.getScore(), iScore, pair.getLabel(), fThreshold);
                    out.append(i > 0 ? " " : "")
                            .append(pair.getLabel())
                            .append(" ").append(iScore);
                } else {
                    out.append(i > 0 ? " " : "")
                            .append("__label__oos ")
                            .append(iScore);
                }
            }

        } catch (Exception e) {
            log.error("predictSupervisedModel ERROR", e);
            return "ERR " + e.getMessage();
        }

        log.info("ml Response sessionId: {} parameter: {} RESPONSE: {}", sessionId, parameter, out);
        return out.toString();
    }


    public static void resetClassCache() {
        sharedClassLoaderMap.forEach((k, loader) -> {
            try {
                loader.close();
            } catch (Exception e) {
                log.error("Error closing URLClassLoader", e);
            }
        });
        sharedClassLoaderMap.clear();
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

            SharedClassLoader beanClass = sharedClassLoaderMap.get(f);
            if (beanClass == null) {
                log.info("{} : callPlugin load new plugin class: {} file: {}", sessionId, classLoad, file);
                beanClass = new SharedClassLoader(f, classLoad);
                sharedClassLoaderMap.put(f, beanClass);
            }
            Object instance = beanClass.newInstance();
            out = beanClass.execute(instance, methodName, parameter, sessionId);

            beanClass.clear(instance, sessionId);

        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            out = "ERR " + (cause != null ? cause.getMessage() : ex.getMessage());
            log.error("{} : callPlugin file: {} parameter : {} ERROR: {}", sessionId, f, parameter, out, ex);
        } catch (Exception ex) {
            out = "ERR " + ex.getMessage();
            log.error("{} : callPlugin file: {} parameter : {} ERROR", sessionId, f, parameter, ex);
        }

        log.info("{} : callPlugin request parameter: {} method: {} plugin response: {}", sessionId, parameter, methodName, out);
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
