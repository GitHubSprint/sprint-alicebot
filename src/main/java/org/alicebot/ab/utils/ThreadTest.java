package org.alicebot.ab.utils;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.cli.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
    private final Map<Integer,Mapa> reqResp = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ThreadTest.class);
    private final int threads = 50;

    public ThreadTest() {
        reqResp.put(0, new Mapa("brak historii leczenia", "brakwizyt"));
        reqResp.put(1, new Mapa("aktualny paszport cowidowy", "certyfikatcovid"));
        reqResp.put(2, new Mapa("brak dostępu do konta dziecka", "dodaniedziecka"));
        reqResp.put(3, new Mapa("a gdy e-recepta nie jest jeszcze zrealizowana", "dostepdoerecepty"));
        reqResp.put(4, new Mapa("chce wypełnić upoważnienie na odbiór dokumentacji medycznej", "edm"));
        reqResp.put(5, new Mapa("czy mogę się zalogować przez edowód", "edowod"));
        reqResp.put(6, new Mapa("błędne dane w e-rejestarcja", "edycjadanych"));
        reqResp.put(7, new Mapa("błąd w wyrobieniu karty ekuz dla dziecka", "ekuz"));
        reqResp.put(8, new Mapa("rejestracja do chirurga", "erejestracja"));
        reqResp.put(9, new Mapa("rejestracja do ginekologa", "erejestracja"));
        reqResp.put(10, new Mapa("ile ważne jest e skierowanie", "eskierowanie"));
        reqResp.put(11, new Mapa("złożenie skargi", "zgloszenienieprawidlowosci"));
        reqResp.put(12, new Mapa("zgłoszenie nieprawidłowości", "zgloszenienieprawidlowosci"));
        reqResp.put(13, new Mapa("co to jest zdarzenie medyczne", "zdarzeniamedyczne"));
        reqResp.put(14, new Mapa("czemu nie pojawiają się wszystkie zdarzenia medyczne na  ikp", "zdarzeniamedyczne"));
    }

    public void start(){
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0; i < threads; i++) {
            executorService.execute(() -> {
                try {
                    testCEZBot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        executorService.shutdown();
    }


    private void testCEZBot() throws Exception {
        Bot bot = new Bot("testowa", MagicStrings.root_path, "chat");
        String session = UUID.randomUUID().toString();
        Chat chatSession = new Chat(bot, session, "CHAT");
        Map<String,String> req = new HashMap<>();
        for(int i=0; i<15;i++)
        {
            Random random = new Random();
            int randomNumber = random.nextInt(11);
            Mapa m = reqResp.get(randomNumber);
            req.put(m.getK(), m.getV());
        }

        String response = chatSession.multisentenceRespond("CEZ");

        if(!response.startsWith("Podaj"))
            log.error("Robot: "+response);

        for (Map.Entry<String, String> entry : req.entrySet()) {
            log.info(session + " " + entry.getKey() + " ::: " + entry.getValue());

            response = chatSession.multisentenceRespond(entry.getKey());

            log.info(session + " response: " + response);

            if(!response.startsWith(entry.getValue())) {
                System.out.println("!!!!!!!!!!!ERROR Key: " + entry.getKey() + " value: " + entry.getValue() + "\t response: " + response);
                log.error(session + " ERROR Key: " + entry.getKey() + " value: " + entry.getValue() + "\t response: " + response);
            }

            Thread.sleep(500);
        }

    }

    private static class Mapa{
        private String k;
        private String v;

        public Mapa(String k, String v) {
            this.k = k;
            this.v = v;
        }

        public String getK() {
            return k;
        }

        public void setK(String k) {
            this.k = k;
        }

        public String getV() {
            return v;
        }

        public void setV(String v) {
            this.v = v;
        }
    }
}
