package org.alicebot.ab.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SprintBotDbUtils {
    private static final Logger log = LoggerFactory.getLogger(SprintBotDbUtils.class);

    private static String url = null;
    private static String username;
    private static String password;
    private static String schema;
    private static String timezone;
    private final static ObjectMapper mapper = new ObjectMapper();


    public static void updateConfiguration(String newUrl, String newDriverClassName, String newUsername, String newPassword, String newSchema, String newTimezone) {
        url = newUrl;
        username = newUsername;
        password = newPassword;
        schema = newSchema;
        timezone = newTimezone;
        log.info("updateConfiguration url: {} driverClassName: {} username: {} schema: {} timezone: {}", url, newDriverClassName, username, schema, timezone);
    }


    public static CompletableFuture<Void> saveReportAsync(String name, String symbol, Report report, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            log.info("saveReport sessionId: {} name: {} symbol: {} report: {}", sessionId, name, symbol, report);

            if (url == null) {
                log.warn("{} saveReport invalid dbUrl: {}", sessionId, url);
                return;
            }

            switch (name) {
                case "fraza":
                    botFraza(symbol, report, sessionId);
                    break;
                case "ocena":
                    botOcena(symbol, report, sessionId);
                    break;
                case "info":
                    botInfo(name, symbol, report, sessionId);
                    break;
                default:
                    log.warn("{} saveReport invalid reportName: {} ", sessionId, name);
                    break;
            }
        });
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

    private static void botInfo(String name, String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".bot_info (botname, idsesji, info, klucz, symbol, timestamp, wartosc) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, sessionId);
            preparedStatement.setString(3, report.info());
            preparedStatement.setString(4, report.klucz());
            preparedStatement.setString(5, symbol);
            preparedStatement.setTimestamp(6, getTimestamp());
            preparedStatement.setString(7, report.wartosc());

            int rowsAffected = preparedStatement.executeUpdate();
            log.info("botInfo save report rows affected: {}", rowsAffected);

        } catch (SQLException e) {
            log.warn("botInfo save report error", e);
        }
    }

    private static void botOcena(String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".bot_ocena (idsesji, licznikfraz, licznikocen, ocena, sposoboceny, symbol, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, sessionId);
            if(report.licznikFraz() != null)
                preparedStatement.setInt(2, Integer.parseInt(report.licznikFraz()));
            if(report.licznikOcen() != null)
                preparedStatement.setInt(3, Integer.parseInt(report.licznikOcen()));
            preparedStatement.setString(4, report.ocena());
            preparedStatement.setString(5, report.sposobOceny());
            preparedStatement.setString(6, symbol);
            preparedStatement.setTimestamp(7, getTimestamp());


            int rowsAffected = preparedStatement.executeUpdate();
            log.info("botOcena save report rows affected: {}", rowsAffected);

        } catch (SQLException e) {
            log.warn("botOcena save report error", e);
        }
    }

    private static void botFraza(String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".BOT_FRAZA (fakt, fraza, frazacala, idsesji, label, licznikfraz, rozpoznanie, symbol, timestamp, wiarygodnosc) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, report.fakt());
            preparedStatement.setString(2, report.fraza());
            preparedStatement.setString(3, report.frazaCala());
            preparedStatement.setString(4, sessionId);
            preparedStatement.setString(5, report.label());
            if(report.licznikFraz() != null)
                preparedStatement.setInt(6, Integer.parseInt(report.licznikFraz()));
            preparedStatement.setString(7, report.rozpoznanie());
            preparedStatement.setString(8, symbol);
            preparedStatement.setTimestamp(9, getTimestamp());
            if(report.wiarygodnosc() != null)
                preparedStatement.setInt(10, Integer.parseInt(report.wiarygodnosc()));

            int rowsAffected = preparedStatement.executeUpdate();
            log.info("botFraza save report rows affected: {}", rowsAffected);

        } catch (SQLException e) {
            log.warn("botFraza save report error", e);
        }
    }


    public static Timestamp getTimestamp() {
        Instant instant = Instant.now();
        ZonedDateTime zdt = instant.atZone(ZoneId.of(timezone));
        return Timestamp.from(zdt.toInstant());
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
