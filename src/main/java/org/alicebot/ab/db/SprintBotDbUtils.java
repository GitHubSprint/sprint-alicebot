package org.alicebot.ab.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.alicebot.ab.model.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SprintBotDbUtils {
    private static final Logger log = LoggerFactory.getLogger(SprintBotDbUtils.class);

    private static HikariDataSource dataSource;
    private static String schema;
    private static String timezone;
    private static final ObjectMapper mapper = new ObjectMapper();

    // Cache dla często używanych zapytań SQL
    private static final Map<String, String> sqlCache = new ConcurrentHashMap<>();

    // Dedykowany thread pool dla operacji async
    private static final Executor dbExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );

    // TypeReference jako stała - nie trzeba tworzyć za każdym razem
    private static final TypeReference<HashMap<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    public static void updateConfiguration(String url,
                                           String driverClassName,
                                           String username,
                                           String password,
                                           String newSchema,
                                           String newTimezone)
    {
        schema = newSchema;
        timezone = newTimezone;

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Optymalizacje connection pool
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(120000);
        config.setMaxLifetime(1800000);
        config.setAutoCommit(true);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        // Wyczyść cache przy zmianie konfiguracji
        sqlCache.clear();

        log.info("updateConfiguration url: {} username: {} schema: {} timezone: {}",
                url, username, schema, timezone);
    }

    public static String getDbSelect(String reportName, Set<Param> params) {
        log.info("getDbSelect reportName: {} params: {}", reportName, params);

        if (reportName == null || dataSource == null) {
            log.warn("getDbSelect invalid input - reportName: {}, dataSource: {}",
                    reportName, dataSource != null);
            return null;
        }

        // Sprawdź cache
        String queryTemplate = sqlCache.get(reportName);

        // Jeśli nie ma w cache, pobierz z bazy
        if (queryTemplate == null) {
            String sql = "SELECT sql FROM " + schema + ".sys_bot_selects WHERE name = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, reportName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        queryTemplate = rs.getString("sql");
                        if (queryTemplate != null) {
                            sqlCache.put(reportName, queryTemplate);
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("getDbSelect error", e);
                return null;
            }
        }

        if (queryTemplate == null) {
            return null;
        }

        // Zastąp parametry
        String query = queryTemplate;
        for (Param param : params) {
            query = query.replace("{" + param.name().toUpperCase() + "}", param.value());
        }

        log.info("getDbSelect query: {}", query);

        try (Connection conn = dataSource.getConnection()) {
            return getDbSelectData(query, conn);
        } catch (SQLException e) {
            log.error("getDbSelect error executing query", e);
            return null;
        }
    }

    private static String getDbSelectData(String sql, Connection connection) throws SQLException {
        StringBuilder result = new StringBuilder(1024);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (!result.isEmpty()) {
                        result.append("###");
                    }
                    String value = rs.getString(i);
                    if (value != null) {
                        result.append(value);
                    }
                }
            }
        }

        return result.toString();
    }

    public static CompletableFuture<Void> saveReportAsync(String name, String symbol,
                                                          Report report, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            log.info("saveReport sessionId: {} name: {} symbol: {}", sessionId, name, symbol);

            if (dataSource == null) {
                log.warn("{} saveReport invalid dataSource", sessionId);
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
                    botInfo(symbol, report, sessionId);
                    break;
                default:
                    log.warn("{} saveReport invalid reportName: {}", sessionId, name);
                    break;
            }
        }, dbExecutor);
    }

    public static String updateRecord(String parameter) {
        log.info("updateRecord parameter: {}", parameter);

        if (parameter == null || dataSource == null) {
            log.warn("updateRecord invalid input");
            return null;
        }

        String[] params = parameter.split("###");
        if (params.length < 3) {
            log.warn("updateRecord invalid parameter: {}", parameter);
            return null;
        }

        int recordId;
        try {
            recordId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            log.warn("updateRecord invalid id: {}", params[0]);
            return null;
        }

        Map<String, String> data = new HashMap<>();
        data.put(params[1], params[2]);

        String selectSql = "SELECT ext_data FROM " + schema + ".bot_dialer_contact_record WHERE id = ?";
        String updateSql = "UPDATE " + schema + ".bot_dialer_contact_record SET ext_data = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            // SELECT
            String extData = null;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setInt(1, recordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        extData = rs.getString("ext_data");
                    }
                }
            }

            // Merge data
            Map<String, String> currentData = extData != null
                    ? mapper.readValue(extData, MAP_TYPE_REF)
                    : new HashMap<>();
            currentData.putAll(data);

            // UPDATE
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, mapper.writeValueAsString(currentData));
                ps.setInt(2, recordId);
                int rows = ps.executeUpdate();
                log.info("updateRecord updated rows: {}", rows);
            }

            return "OK";

        } catch (SQLException | JsonProcessingException e) {
            log.error("updateRecord error", e);
            return null;
        }
    }

    public static String updateRecordStatus(String parameter) {
        log.info("updateRecordStatus parameter: {}", parameter);

        if (parameter == null || dataSource == null) {
            log.warn("updateRecordStatus invalid input");
            return null;
        }

        String[] params = parameter.split("###");
        if (params.length < 8) {
            log.warn("updateRecordStatus invalid parameter: {}", parameter);
            return null;
        }

        try {
            int id = Integer.parseInt(params[0]);
            int status = Integer.parseInt(params[1]);
            String phone1 = params[2];
            int cntPhone1 = Integer.parseInt(params[3]);
            int statusPhone1 = Integer.parseInt(params[4]);
            String phone2 = params[5];
            int cntPhone2 = Integer.parseInt(params[6]);
            int statusPhone2 = Integer.parseInt(params[7]);

            String updateRecord = "UPDATE " + schema + ".bot_dialer_contact_record SET status = ? WHERE id = ?";

            // FIXED: Bug w oryginalnym SQL - status_phone1 występował dwa razy
            String updatePhone = "UPDATE " + schema + ".bot_dialer_contact_phone p " +
                    "SET cnt_phone1 = ?, status_phone1 = ?, cnt_phone2 = ?, status_phone2 = ? " +
                    "FROM " + schema + ".bot_dialer_contact_record r " +
                    "WHERE p.id = r.contact_phone_id AND r.id = ?";

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Update record
                    try (PreparedStatement ps = conn.prepareStatement(updateRecord)) {
                        ps.setInt(1, status);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }

                    // Update phone
                    try (PreparedStatement ps = conn.prepareStatement(updatePhone)) {
                        ps.setInt(1, cntPhone1);
                        ps.setInt(2, statusPhone1);
                        ps.setInt(3, cntPhone2);
                        ps.setInt(4, statusPhone2);
                        ps.setInt(5, id);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    log.info("updateRecordStatus completed for id: {}", id);
                    return "OK";

                } catch (SQLException e) {
                    conn.rollback();
                    log.error("updateRecordStatus error", e);
                    return "ERR " + e.getMessage();
                }
            }

        } catch (NumberFormatException | SQLException e) {
            log.error("updateRecordStatus error", e);
            return "ERR " + e.getMessage();
        }
    }

    public static String getRecord(String parameter) {
        log.info("getRecord parameter: {}", parameter);

        if (parameter == null || dataSource == null) {
            log.warn("getRecord invalid input");
            return null;
        }

        String[] params = parameter.split("###");
        if (params.length < 2) {
            log.warn("getRecord invalid parameter: {}", parameter);
            return null;
        }

        int recordId;
        try {
            recordId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            log.warn("getRecord invalid id: {}", params[0]);
            return null;
        }

        String sql = "SELECT ext_data FROM " + schema + ".bot_dialer_contact_record WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String extData = rs.getString("ext_data");
                    if (extData != null) {
                        Map<String, String> map = mapper.readValue(extData, MAP_TYPE_REF);
                        return map.get(params[1]);
                    }
                }
            }

        } catch (SQLException | JsonProcessingException e) {
            log.error("getRecord error", e);
        }

        return null;
    }

    public static String getRecordStatus(String parameter) {
        log.info("getRecordStatus parameter: {}", parameter);

        if (parameter == null || dataSource == null) {
            log.warn("getRecordStatus invalid input");
            return null;
        }

        int recordId;
        try {
            recordId = Integer.parseInt(parameter);
        } catch (NumberFormatException e) {
            log.warn("getRecordStatus invalid id: {}", parameter);
            return null;
        }

        String sql = "SELECT r.id, r.status, p.phone1, p.cnt_phone1, p.status_phone1, " +
                "p.phone2, p.cnt_phone2, p.status_phone2 " +
                "FROM " + schema + ".bot_dialer_contact_record r " +
                "JOIN " + schema + ".bot_dialer_contact_phone p ON p.id = r.contact_phone_id " +
                "WHERE r.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return String.format("%d###%d###%s###%d###%d###%s###%d###%d",
                            rs.getInt("id"),
                            rs.getInt("status"),
                            rs.getString("phone1"),
                            rs.getInt("cnt_phone1"),
                            rs.getInt("status_phone1"),
                            rs.getString("phone2"),
                            rs.getInt("cnt_phone2"),
                            rs.getInt("status_phone2")
                    );
                }
            }

        } catch (SQLException e) {
            log.error("getRecordStatus error", e);
        }

        return null;
    }

    private static void botInfo(String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".bot_info " +
                "(botname, idsesji, info, klucz, symbol, timestamp, wartosc) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, report.botName());
            ps.setString(2, sessionId);
            ps.setString(3, report.info());
            ps.setString(4, report.klucz());
            ps.setString(5, symbol);
            ps.setTimestamp(6, getTimestamp());
            ps.setString(7, report.wartosc());

            int rows = ps.executeUpdate();
            log.info("botInfo saved rows: {}", rows);

        } catch (SQLException e) {
            log.error("botInfo error", e);
        }
    }

    private static void botOcena(String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".bot_ocena " +
                "(idsesji, licznikfraz, licznikocen, ocena, sposoboceny, symbol, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionId);

            if (report.licznikFraz() != null) {
                ps.setInt(2, Integer.parseInt(report.licznikFraz()));
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            if (report.licznikOcen() != null) {
                ps.setInt(3, Integer.parseInt(report.licznikOcen()));
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, report.ocena());
            ps.setString(5, report.sposobOceny());
            ps.setString(6, symbol);
            ps.setTimestamp(7, getTimestamp());

            int rows = ps.executeUpdate();
            log.info("botOcena saved rows: {}", rows);

        } catch (SQLException e) {
            log.error("botOcena error", e);
        }
    }

    private static void botFraza(String symbol, Report report, String sessionId) {
        String sql = "INSERT INTO " + schema + ".bot_fraza " +
                "(fakt, fraza, frazacala, idsesji, label, licznikfraz, rozpoznanie, symbol, timestamp, wiarygodnosc) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, report.fakt());
            ps.setString(2, report.fraza());
            ps.setString(3, report.frazaCala());
            ps.setString(4, sessionId);
            ps.setString(5, report.label());

            if (report.licznikFraz() != null) {
                ps.setInt(6, Integer.parseInt(report.licznikFraz()));
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, report.rozpoznanie());
            ps.setString(8, symbol);
            ps.setTimestamp(9, getTimestamp());

            if (report.wiarygodnosc() != null) {
                ps.setInt(10, Integer.parseInt(report.wiarygodnosc()));
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            log.info("botFraza saved rows: {}", rows);

        } catch (SQLException e) {
            log.error("botFraza error", e);
        }
    }

    public static Timestamp getTimestamp() {
        Instant instant = Instant.now();
        ZonedDateTime zdt = instant.atZone(ZoneId.of(timezone));
        return Timestamp.from(zdt.toInstant());
    }

    // Metoda do zamykania pool'a przy shutdown aplikacji
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("DataSource closed");
        }
    }
}