package org.alicebot.ab;

import org.alicebot.ab.utils.AlicebotContextProvider;

public class AlicebotContext {
    private static AlicebotContextProvider provider;
    public static void setProvider(AlicebotContextProvider p) {
        provider = p;
    }
    public static AlicebotContextProvider getProvider() {
        return provider;
    }
}
