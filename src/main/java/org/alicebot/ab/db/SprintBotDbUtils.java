package org.alicebot.ab.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alicebot.ab.llm.LLMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SprintBotDbUtils {
    private static final Logger log = LoggerFactory.getLogger(SprintBotDbUtils.class);

    private static String url = null;
    private static String driverClassName;
    private static String username;
    private static String password;
    private static String schema;
    private final static ObjectMapper mapper = new ObjectMapper();

    public static void updateGeminiToken(String token) {
        LLMConfiguration.geminiToken = token;
    }

    public static void updateLLMConfiguration(String gptToken,
                                              String gptApiUrl,
                                              String ollamaApiUrl,
                                              String geminiToken,
                                              String geminiApiUrl,
                                              boolean gptEnabled,
                                              boolean ollamaEnabled,
                                              boolean geminiEnabled)
    {
        log.info("updateLLMConfiguration gptEnabled: {} ollamaEnabled: {}  geminiEnabled: {}", gptEnabled, ollamaEnabled, geminiEnabled);
        log.info("updateLLMConfiguration gptApiUrl: {} ollamaApiUrl: {}  geminiApiUrl: {}", gptApiUrl, ollamaApiUrl, geminiApiUrl);
        LLMConfiguration.gptToken = gptToken;
        LLMConfiguration.gptApiUrl = gptApiUrl;
        LLMConfiguration.ollamaApiUrl = ollamaApiUrl;
        LLMConfiguration.geminiToken = geminiToken;
        LLMConfiguration.geminiApiUrl = geminiApiUrl;
        LLMConfiguration.gptEnabled = gptEnabled;
        LLMConfiguration.ollamaEnabled = ollamaEnabled;
        LLMConfiguration.geminiEnabled = geminiEnabled;


    }

    public static void updateConfiguration(String newUrl, String newDriverClassName, String newUsername, String newPassword, String newSchema) {
        url = newUrl;
        driverClassName = newDriverClassName;
        username = newUsername;
        password = newPassword;
        schema = newSchema;
        log.info("updateConfiguration url: {} driverClassName: {} username: {} schema: {}", url, driverClassName, username, schema);
    }


    public static String updateRecord(String parameter) {
        log.info("updateRecord parameter: {}", parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("updateRecord invalid dbUrl: {}", url);
            return null;
        }
        String[] parameters = parameter.split("###");
        if(parameter.length() < 3){
            log.warn("updateRecord invalid parameter: {}", parameter);
            return null;
        }

        Map<String, String> data = new HashMap<>();
        data.put(parameters[1], parameters[2]);

        try (Connection connection = DriverManager.getConnection(url, username, password)) {

            String selectSql = "SELECT ext_data FROM " + schema + ".bot_dialer_contact_record where id=" + parameters[0];
            String extData = null;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    if (resultSet.next()) {
                        extData = resultSet.getString("ext_data");
                    }
                }
            }

            TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};

            Map<String, String> currentData = extData != null ?
                    mapper.readValue(extData, typeRef) :
                    new HashMap<>();

            currentData.putAll(data);

            String updateSql = "UPDATE " + schema + ".bot_dialer_contact_record SET ext_data = ? WHERE id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setString(1, mapper.writeValueAsString(currentData));
                updateStmt.setInt(2, Integer.parseInt(parameters[0]));
                int rowsUpdated = updateStmt.executeUpdate();
                log.info("updateRecord updated rows: {} ", rowsUpdated);
            }

        } catch (SQLException | JsonProcessingException e) {
            log.error("updateRecord err", e);
            return null;
        }

        return "OK";
    }


    public static String getRecord(String parameter) {
        log.info("getRecord parameter: {}", parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("getRecord invalid dbUrl: {}", url);
            return null;
        }

        String result = null;


        //17###komunikat
        String[] parameters = parameter.split("###");
        if(parameter.length() < 2){
            log.warn("getRecord invalid parameter: {}", parameter);
            return null;
        }

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {

            // Wykonanie zapytania SELECT
            String sql = "SELECT ext_data FROM " + schema + ".bot_dialer_contact_record where id=" + parameters[0];
            ResultSet resultSet = statement.executeQuery(sql);

            // Przetwarzanie wyników
            while (resultSet.next()) {
                String extData = resultSet.getString("ext_data");
                log.info("getRecord ext_data: {} ",extData);

                if(extData != null) {
                    TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
                    Map<String,String> map = mapper.readValue(extData, typeRef);
                    result = map.get(parameters[1]);
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.error("getRecord err", e);
        }

        return result;
    }

    public static String getData(String parameter, String sessionId) {
        log.info("{} getdata parameter: {}", sessionId, parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("{} getdata invalid dbUrl: {}", sessionId, url);
            return null;
        }

        String result = null;

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT extdata FROM " + schema + ".bot_session WHERE session_id= ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(sql)) {
                selectStmt.setString(1, sessionId);
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String extData = resultSet.getString("extdata");
                        if(extData != null) {
                            TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
                            Map<String,String> map = mapper.readValue(extData, typeRef);
                            result = map.get(parameter);
                        }
                    }
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.error("{} getdata err", sessionId, e);
        }
        return result;
    }

    public static String setData(String parameter, String sessionId) {
        log.info("{} setData parameter: {}", sessionId, parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("{} setData invalid dbUrl: {}", sessionId, url);
            return null;
        }

        String[] parameters = parameter.split("###");
        if(parameter.length() < 2){
            log.warn("{} setData invalid parameter: {}", sessionId, parameter);
            return null;
        }

        Map<String, String> data = new HashMap<>();
        data.put(parameters[0], parameters[1]);

        try (Connection connection = DriverManager.getConnection(url, username, password)) {

            String selectSql = "SELECT extdata FROM " + schema + ".bot_session WHERE session_id = ?";
            String extData = null;

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setString(1, sessionId);
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    if (resultSet.next()) {
                        extData = resultSet.getString("extdata");
                        log.info("{} setData extdata: {} ", sessionId, extData);
                    }
                }
            }

            TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};

            Map<String, String> currentData = extData != null ?
                    mapper.readValue(extData, typeRef) :
                    new HashMap<>();

            currentData.putAll(data);

            String updateSql = "UPDATE " + schema + ".bot_session SET extdata = ? WHERE session_id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setString(1, mapper.writeValueAsString(currentData));
                updateStmt.setString(2, sessionId);
                int rowsUpdated = updateStmt.executeUpdate();
                log.info("{} setData updated rows: {} ", sessionId, rowsUpdated);
            }

        } catch (SQLException | JsonProcessingException e) {
            log.error("{} setData err", sessionId, e);
            return null;
        }

        return "OK";
    }

}
