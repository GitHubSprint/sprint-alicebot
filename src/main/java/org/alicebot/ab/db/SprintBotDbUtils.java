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

    /**
     * Updates the status of a contact record and its associated phone numbers.
     *
     * @param parameter A string containing the record ID, new status, and phone details.
     *                  (id, status, phone1, cntPhone1, statusPhone1, phone2, cntPhone2, statusPhone2)
     * @return "OK" if the update was successful, or an error message if it failed.
     */
    public static String updateRecordStatus(String parameter) {
        log.info("updateRecordStatus parameter: {}", parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("updateRecordStatus invalid dbUrl: {}", url);
            return null;
        }

        String[] parameters = parameter.split("###");
        if(parameter.length() < 8){
            log.warn("updateRecordStatus invalid parameter: {}", parameter);
            return null;
        }

        int id = Integer.parseInt(parameters[0]);
        int status = Integer.parseInt(parameters[1]);
        String phone1 = parameters[2];
        int cntPhone1 = Integer.parseInt(parameters[3]);
        int statusPhone1 = Integer.parseInt(parameters[4]);
        String phone2 = parameters[5];
        int cntPhone2 = Integer.parseInt(parameters[6]);
        int statusPhone2 = Integer.parseInt(parameters[7]);

        log.info("updateRecordStatus id: {}, status: {}, phone1: {}, cntPhone1: {}, statusPhone1: {}, phone2: {}, cntPhone2: {}, statusPhone2: {}",
                id, status, phone1, cntPhone1, statusPhone1, phone2, cntPhone2, statusPhone2);

        String updateRecord = "UPDATE " + schema + ".bot_dialer_contact_record SET status = ? WHERE id = ?";

        String updatePhone = "UPDATE " + schema + ".bot_dialer_contact_phone p " +
            "SET cnt_phone1 = ?, status_phone1 = ?,  cnt_phone2 = ?, status_phone1 = ? " +
            "FROM " + schema + ".bot_dialer_contact_record r " +
            "WHERE p.id = r.contact_phone_id AND r.id = ?";


        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);

            try (PreparedStatement psRecord = connection.prepareStatement(updateRecord);
                 PreparedStatement psPhone = connection.prepareStatement(updatePhone))
            {
                psRecord.setInt(1, status);
                psRecord.setInt(2, id);
                int psRecordRows = psRecord.executeUpdate();

                psPhone.setInt(1, cntPhone1);
                psPhone.setInt(2, statusPhone1);
                psPhone.setInt(3, cntPhone2);
                psPhone.setInt(4, statusPhone2);
                psPhone.setInt(5, id);
                int psPhoneRows = psPhone.executeUpdate();
                log.info("updateRecordStatus updated record rows: {} and phone rows: {}", psRecordRows, psPhoneRows);
                connection.commit();
            } catch (SQLException e) {
                log.error("updateRecordStatus error during update", e);
                connection.rollback();
                return "ERR " + e.getMessage();
            }
        } catch (SQLException e) {
            log.error("updateRecordStatus err", e);
            return "ERR " + e.getMessage();
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

            String sql = "SELECT ext_data FROM " + schema + ".bot_dialer_contact_record where id=" + parameters[0];
            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                String extData = resultSet.getString("ext_data");
                log.info("getRecord ext_data: {} ",extData);

                if(extData != null) {
                    TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
                    Map<String,String> map = mapper.readValue(extData, typeRef);
                    result = map.get(parameters[1]);
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.error("getRecord err", e);
        }

        return result;
    }


    /**
     * Retrieves the status of a contact record by its ID.
     *
     * @param parameter The ID of the contact record.
     * @return A formatted string containing the record's details or null if not found
     *          (id, status, phone1, cntPhone1, statusPhone1, phone2, cntPhone2, statusPhone2).
     */
    public static String getRecordStatus(String parameter) {
        log.info("getRecordStatus parameter: {}", parameter);
        if(parameter == null)
            return null;

        if(url == null) {
            log.warn("getRecordStatus invalid dbUrl: {}", url);
            return null;
        }

        String result = null;

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement())
        {
            String sql = "SELECT r.id, r.status, p.phone1, p.cnt_phone1, p.status_phone1, p.phone2, p.cnt_phone2, p.status_phone2 " +
                    "FROM " + schema + ".bot_dialer_contact_record r, " + schema + ".bot_dialer_contact_phone p " +
                    "WHERE p.id = r.contact_phone_id and r.id =" + parameter;

            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                Integer id = resultSet.getInt("id");
                Integer status = resultSet.getInt("status");
                String phone1 = resultSet.getString("phone1");
                Integer cntPhone1 = resultSet.getInt("cnt_phone1");
                Integer statusPhone1 = resultSet.getInt("status_phone1");
                String phone2 = resultSet.getString("phone2");
                Integer cntPhone2 = resultSet.getInt("cnt_phone2");
                Integer statusPhone2 = resultSet.getInt("status_phone2");
                log.info("getRecordStatus id: {}, status: {}, phone1: {}, cntPhone1: {}, statusPhone1: {}, phone2: {}, cntPhone2: {}, statusPhone2: {}",
                        id, status, phone1, cntPhone1, statusPhone1, phone2, cntPhone2, statusPhone2);
                result = String.format("%d###%d###%s###%d###%d###%s###%d###%d",
                        id, status, phone1, cntPhone1, statusPhone1, phone2, cntPhone2, statusPhone2);
            }
        } catch (SQLException e) {
            log.error("getRecordStatus err", e);
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
        log.info("botInfo report: {}", report);

        String sql = "INSERT INTO " + schema + ".bot_info (botname, idsesji, info, klucz, symbol, timestamp, wartosc) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, report.botName());
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
