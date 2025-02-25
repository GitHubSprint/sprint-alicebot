package org.alicebot.ab.db;

public record Report(
        String frazaCala,
        String fraza,
        String rozpoznanie,
        String label,
        String wiarygodnosc,
        String fakt,
        String licznikFraz,
        String licznikOcen,
        String sposobOceny,
        String ocena,
        String botName,
        String info,
        String klucz,
        String wartosc
) {
    @Override
    public String toString() {
        return "Report{" +
                ", frazaCala='" + frazaCala + '\'' +
                ", fraza='" + fraza + '\'' +
                ", rozpoznanie='" + rozpoznanie + '\'' +
                ", label='" + label + '\'' +
                ", wiarygodnosc='" + wiarygodnosc + '\'' +
                ", fakt='" + fakt + '\'' +
                ", licznikFraz='" + licznikFraz + '\'' +
                ", licznikOcen='" + licznikOcen + '\'' +
                ", sposobOceny='" + sposobOceny + '\'' +
                ", ocena='" + ocena + '\'' +
                ", botName='" + botName + '\'' +
                ", info='" + info + '\'' +
                ", klucz='" + klucz + '\'' +
                ", wartosc='" + wartosc + '\'' +
                '}';
    }
}
