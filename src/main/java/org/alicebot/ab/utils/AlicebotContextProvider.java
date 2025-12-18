package org.alicebot.ab.utils;

import java.util.Map;

public interface AlicebotContextProvider {
    Map<String, String> getSessionData(String sessionId);
    void updateSessionData(String sessionId, Map<String, String> data);
}
