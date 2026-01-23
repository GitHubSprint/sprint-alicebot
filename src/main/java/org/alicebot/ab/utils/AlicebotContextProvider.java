package org.alicebot.ab.utils;

import org.alicebot.ab.model.Param;

import java.util.Map;
import java.util.Set;

public interface AlicebotContextProvider {
    Map<String, String> getSessionData(String sessionId);
    void updateSessionData(String sessionId, Map<String, String> data);
    String getDbSelect(String reportName, Set<Param> params);
}
