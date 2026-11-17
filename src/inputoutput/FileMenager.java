package inputoutput;

import model.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class FileMenager {
    private FileMenager() {}

    // regex pomoćnici za validaciju
    private static final Pattern CODE = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern HHMM = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d$");

    // CSV pomoćnici
    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\""))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
    private static String unescape(String s) {
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length()-1).replace("\"\"", "\"");
        return s;
    }
    private static double parseDouble(String s, String msg) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { throw new IllegalArgumentException(msg); }
    }
    private static int parseInt(String s, String msg) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { throw new IllegalArgumentException(msg); }
    }

    // brza validacija da li postojeće CSV ima dobar oblik (sekcije i redove) + duplikati u samom fajlu
    private static void validateExistingCsvStructure(File file) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            boolean readingAirports = false, readingFlights = false;
            boolean sawAirports = false, sawFlights = false;

            // set za detekciju duplikata KODOVA unutar fajla (samo u [AIRPORTS] sekciji)
            Set<String> seenCodesInFile = new HashSet<>();

            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("[AIRPORTS]")) { readingAirports = true; readingFlights = false; sawAirports = true; continue; }
                if (line.equals("[FLIGHTS]"))  { readingAirports = false; readingFlights = true;  sawFlights  = true; continue; }

                String[] t = line.split(",", -1);

                if (readingAirports) {
                    if (t.length != 4)
                        throw new IOException("AIRPORTS deo ima pogrešan broj kolona (očekuje se name,code,x,y). Linija: " + line);
                    if (!CODE.matcher(t[1]).matches())
                        throw new IOException("AIRPORTS: Kod mora biti 3 velika slova (A–Z). Nađeno: " + t[1]);
                    // duplikat koda u samom fajlu
                    String code = t[1];
                    if (!seenCodesInFile.add(code))
                        throw new IOException("AIRPORTS: Duplikat koda u fajlu: " + code);
                    parseDouble(t[2], "AIRPORTS: X koordinata nije broj: " + t[2]);
                    parseDouble(t[3], "AIRPORTS: Y koordinata nije broj: " + t[3]);
                } else if (readingFlights) {
                    if (t.length != 4)
                        throw new IOException("FLIGHTS deo ima pogrešan broj kolona (očekuje se from,to,departureHHmm,durationMin). Linija: " + line);
                    if (!CODE.matcher(t[0]).matches() || !CODE.matcher(t[1]).matches())
                        throw new IOException("FLIGHTS: Kodovi aerodroma moraju biti 3 velika slova. Linija: " + line);
                    if (!HHMM.matcher(t[2]).matches())
                        throw new IOException("FLIGHTS: Vreme mora biti HH:MM (00:00–23:59). Nađeno: " + t[2]);
                    parseInt(t[3], "FLIGHTS: Trajanje nije ceo broj: " + t[3]);
                } else {
                    throw new IOException("Fajl sadrži podatke van sekcija; očekivane su oznake [AIRPORTS] i [FLIGHTS]. Linija: " + line);
                }
            }

            if (!sawAirports || !sawFlights) {
                throw new IOException("Fajl ne sadrži obe sekcije [AIRPORTS] i [FLIGHTS].");
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Pogrešan format CSV-a: " + ex.getMessage());
        }
    }

    // dodavanje BEZ brisanja (append)
    public static void saveToFile(String filename, DataBase db) throws IOException {
        if (filename == null || filename.trim().isEmpty())
            throw new IOException("Nije uneto ime fajla za čuvanje.");

        File file = new File(filename);
        boolean exists = file.exists();

        // ako fajl postoji proveri dosadašnji sadržaj (i duplikate unutar fajla)
        if (exists) {
            validateExistingCsvStructure(file);
        }

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, true))) {
            if (exists) w.newLine();

            w.write("[AIRPORTS]"); w.newLine();
            for (Airport a : db.getAirports()) {
                w.write(escape(a.getName()) + "," + a.getCode() + "," + a.getX() + "," + a.getY());
                w.newLine();
            }

            w.write("[FLIGHTS]"); w.newLine();
            for (Flight f : db.getFlights()) {
                w.write(f.getFrom().getCode() + "," + f.getTo().getCode() + ","
                        + f.getDepartureHHmm() + "," + f.getDurationMin());
                w.newLine();
            }
        }
    }

    // dodavanje iz fajla (NE briše postojeće), ali NE dozvoljava duplikat aerodroma u memoriji
    public static void loadFromFile(String filename, DataBase db) throws IOException {
        if (filename == null || filename.trim().isEmpty())
            throw new IOException("Nije uneto ime fajla za učitavanje.");

        File file = new File(filename);
        if (!file.exists())
            throw new IOException("Fajl ne postoji: " + filename);

        validateExistingCsvStructure(file);

        // učitavanje (bez db.clear), ali zabranjujemo duplikate u memoriji
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            boolean readingAirports = false, readingFlights = false;

            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("[AIRPORTS]")) { readingAirports = true;  readingFlights = false; continue; }
                if (line.equals("[FLIGHTS]"))  { readingAirports = false; readingFlights = true;  continue; }

                String[] t = line.split(",", -1);
                if (readingAirports) {
                    if (t.length != 4)
                        throw new IOException("AIRPORTS deo nema očekivane kolone (name,code,x,y). Linija: " + line);
                    if (!CODE.matcher(t[1]).matches())
                        throw new IOException("AIRPORTS: Kod mora biti 3 velika slova (A–Z). Nađeno: " + t[1]);

                    String code = t[1];
                    if (db.findAirport(code) != null)
                        throw new IOException("AIRPORTS: Duplikat aerodroma — kod " + code + " već postoji u memoriji.");

                    double x = parseDouble(t[2], "X koordinata nije broj: " + t[2]);
                    double y = parseDouble(t[3], "Y koordinata nije broj: " + t[3]);
                    db.putAirport(new Airport(unescape(t[0]), code, x, y));
                } else if (readingFlights) {
                    if (t.length != 4)
                        throw new IOException("FLIGHTS deo nema očekivane kolone (from,to,departureHHmm,durationMin). Linija: " + line);
                    if (!CODE.matcher(t[0]).matches() || !CODE.matcher(t[1]).matches())
                        throw new IOException("FLIGHTS: Kodovi aerodroma moraju biti 3 velika slova. Linija: " + line);
                    if (!HHMM.matcher(t[2]).matches())
                        throw new IOException("FLIGHTS: Vreme mora biti HH:MM (00:00–23:59). Nađeno: " + t[2]);

                    Airport from = db.findAirport(t[0]);
                    Airport to   = db.findAirport(t[1]);
                    if (from == null || to == null)
                        throw new IOException("Nepostojeći kod aerodroma u FLIGHTS: " + t[0] + " ili " + t[1]);

                    int dur = parseInt(t[3], "Trajanje nije ceo broj: " + t[3]);
                    db.addFlight(new Flight(from, to, t[2], dur));
                } else {
                    throw new IOException("Fajl ne sadrži oznake [AIRPORTS] i [FLIGHTS].");
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Pogrešan format CSV-a: " + ex.getMessage());
        }
    }
}
