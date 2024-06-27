package org.alicebot.ab.report;

import org.alicebot.ab.gpt.GenAIHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenReportHelper {
    private static final Logger logger = LoggerFactory.getLogger(GenReportHelper.class);
    public static JSONObject reportFraza(String reportName, Report report)
            throws JSONException
    {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("report_name", reportName);

        JSONObject parameters = new JSONObject();
        addParameter("fraza_cala", report.frazaCala(), parameters);
        addParameter("fraza", report.fraza(), parameters);
        addParameter("rozpoznanie", report.rozpoznanie(), parameters);
        addParameter("label", report.label(), parameters);
        addParameter("wiarygodnosc", report.wiarygodnosc(), parameters);
        addParameter("fakt", report.fakt(), parameters);
        addParameter("licznik_fraz", report.licznikFraz(), parameters);
        addParameter("licznik_ocen", report.licznikOcen(), parameters);
        addParameter("sposob_oceny", report.sposobOceny(), parameters);
        addParameter("ocena", report.ocena(), parameters);
        addParameter("bot_name", report.botName(), parameters);
        addParameter("info", report.info(), parameters);
        addParameter("klucz", report.klucz(), parameters);
        addParameter("wartosc", report.wartosc(), parameters);

        jsonRequest.put("parameters", parameters);
        logger.info("reportFraza report: {}", jsonRequest);
        return jsonRequest;
    }

    private static void addParameter(String name, String value, JSONObject parameters) throws JSONException {
        if(value != null && !value.isEmpty()){
            parameters.put(name, value);
        }
    }

}
