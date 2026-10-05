package org.alicebot.ab;

/**
 * Result of Graphmaster match containing matched leaf Nodemapper and StarBindings.
 */
public class MatchResult {
    private final Nodemapper node;
    private final StarBindings starBindings;

    public MatchResult(Nodemapper node, StarBindings starBindings) {
        this.node = node;
        this.starBindings = starBindings;
    }

    public Nodemapper getNode() {
        return node;
    }

    public StarBindings getStarBindings() {
        return starBindings;
    }
}
