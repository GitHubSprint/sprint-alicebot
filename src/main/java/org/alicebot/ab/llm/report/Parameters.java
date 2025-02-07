package org.alicebot.ab.llm.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameters {
    @JsonProperty(value = "fraza_cala")
    private String frazaCala;

    private String fraza;
    private String rozpoznanie;
    private String label;
    private String wiarygodnosc;
    private String fakt;

    @JsonProperty(value = "licznik_fraz")
    private String licznikFraz;
    @JsonProperty(value = "licznik_ocen")
    private String licznikOcen;
    @JsonProperty(value = "sposob_oceny")
    private String sposobOceny;
    private String ocena;

    //info
    @JsonProperty(value = "bot_name")
    private String botName;
    private String info;
    private String klucz;
    private String wartosc;

    public String getFrazaCala() {
        return frazaCala;
    }

    public void setFrazaCala(String frazaCala) {
        this.frazaCala = frazaCala;
    }

    public String getFraza() {
        return fraza;
    }

    public void setFraza(String fraza) {
        this.fraza = fraza;
    }

    public String getRozpoznanie() {
        return rozpoznanie;
    }

    public void setRozpoznanie(String rozpoznanie) {
        this.rozpoznanie = rozpoznanie;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getWiarygodnosc() {
        return wiarygodnosc;
    }

    public void setWiarygodnosc(String wiarygodnosc) {
        this.wiarygodnosc = wiarygodnosc;
    }

    public String getFakt() {
        return fakt;
    }

    public void setFakt(String fakt) {
        this.fakt = fakt;
    }

    public String getLicznikFraz() {
        return licznikFraz;
    }

    public void setLicznikFraz(String licznikFraz) {
        this.licznikFraz = licznikFraz;
    }

    public String getLicznikOcen() {
        return licznikOcen;
    }

    public void setLicznikOcen(String licznikOcen) {
        this.licznikOcen = licznikOcen;
    }

    public String getSposobOceny() {
        return sposobOceny;
    }

    public void setSposobOceny(String sposobOceny) {
        this.sposobOceny = sposobOceny;
    }

    public String getOcena() {
        return ocena;
    }

    public void setOcena(String ocena) {
        this.ocena = ocena;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getKlucz() {
        return klucz;
    }

    public void setKlucz(String klucz) {
        this.klucz = klucz;
    }

    public String getWartosc() {
        return wartosc;
    }

    public void setWartosc(String wartosc) {
        this.wartosc = wartosc;
    }
}
