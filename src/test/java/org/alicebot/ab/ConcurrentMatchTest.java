package org.alicebot.ab;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentMatchTest {

    private static Bot bot;

    @BeforeAll
    public static void setUp() {
        // Initialize minimal bot without loading files from disk
        bot = new Bot("testBot", System.getProperty("java.io.tmpdir"), "none");
        
        // Add Category: STARTSK * * *
        Category cat = new Category(
                0,
                "STARTSK * * *",
                "*",
                "*",
                "<set name=\"topic\">INTENCJE_<star index=\"3\"/></set>",
                "test.aiml"
        );
        bot.brain.addCategory(cat);
    }

    @Test
    public void testMatchWithBindingsIsolation() {
        MatchResult resFormalne = bot.brain.matchWithBindings("STARTSK 123 123 formalne", "*", "*");
        assertNotNull(resFormalne);
        assertEquals("formalne", resFormalne.getStarBindings().inputStars.star(2));

        MatchResult resTechniczne = bot.brain.matchWithBindings("STARTSK 123 123 techniczne", "*", "*");
        assertNotNull(resTechniczne);
        assertEquals("techniczne", resTechniczne.getStarBindings().inputStars.star(2));

        // Ensure the leaf node starBindings in the graph does not overwrite resFormalne
        assertEquals("formalne", resFormalne.getStarBindings().inputStars.star(2));
    }

    @Test
    public void testConcurrentStarBindingsUnderLoad() throws Exception {
        int threadCount = 20;
        int iterationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final boolean isFormalne = (i % 2 == 0);
            futures.add(executor.submit(() -> {
                startLatch.await();
                Chat chat = new Chat(bot, UUID.randomUUID().toString(), "CHAT", "sprint");

                for (int j = 0; j < iterationsPerThread; j++) {
                    String input = isFormalne ? "STARTSK 123 123 formalne" : "STARTSK 123 123 techniczne";
                    String expectedStar = isFormalne ? "formalne" : "techniczne";
                    String expectedResponse = isFormalne ? "INTENCJE_formalne" : "INTENCJE_techniczne";

                    String response = AIMLProcessor.respond(input, "that", "*", chat);

                    if (!expectedResponse.equalsIgnoreCase(response.trim())) {
                        return false;
                    }

                    // Verify that topic predicate was set correctly
                    String currentTopic = chat.predicates.get("topic");
                    if (!expectedResponse.equalsIgnoreCase(currentTopic)) {
                        return false;
                    }
                }
                return true;
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();

        for (Future<Boolean> f : futures) {
            assertTrue(f.get(30, TimeUnit.SECONDS), "Thread detected star collision or corrupted response!");
        }

        executor.shutdown();
    }

    @Test
    public void testLegacyMatchReturnsNodeWithStarBindings() {
        Nodemapper node = bot.brain.match("STARTSK 123 123 formalne", "*", "*");
        assertNotNull(node);
        assertNotNull(node.starBindings);
        assertEquals("formalne", node.starBindings.inputStars.star(2));
    }
}
