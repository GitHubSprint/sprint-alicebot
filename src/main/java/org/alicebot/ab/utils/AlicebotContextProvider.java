package org.alicebot.ab.utils;

import org.alicebot.ab.model.Report;
import org.alicebot.ab.model.Param;
import java.util.Map;
import java.util.Set;

public interface AlicebotContextProvider {
    Map<String, String> getSessionData(String sessionId);
    void updateSessionData(String sessionId, Map<String, String> data);
    String getDbSelect(String reportName, Set<Param> params);
    void reportSave(String name, String symbol, Report report, String sessionId);
    String getRecordStatus(String parameter);
    String getRecord(String parameter);
    String updateRecord(String parameter);
    String updateRecordStatus(String parameter);
}
