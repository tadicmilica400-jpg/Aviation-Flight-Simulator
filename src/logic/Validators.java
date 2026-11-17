package logic;

import java.util.regex.Pattern;
import model.*;

public class Validators {

    private static final Pattern CODE = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern HHMM = Pattern.compile("^(?:[01]\\d|2[0-3]):[0-5]\\d$");

    private Validators() {}

    public static void checkAirport(String name, String code, double x, double y, DataBase db) {
        if (name == null || name.trim().isEmpty())
            throw new ValidationException("Naziv aerodroma je obavezan.");
        if (code == null || !CODE.matcher(code).matches())
            throw new ValidationException("Kod mora biti tačno 3 VELIKA slova (A–Z).");
        if (x < -90 || x > 90 || y < -90 || y > 90)
            throw new ValidationException("Koordinate moraju biti u opsegu [-90, 90].");
        if (db.findAirport(code) != null)
            throw new ValidationException("Aerodrom sa kodom " + code + " već postoji.");
    }

    public static void checkFlight(String fromCode, String toCode, String hhmm, int duration, DataBase db) {
        if (fromCode == null || toCode == null)
            throw new ValidationException("Kodovi polaznog i dolaznog aerodroma su obavezni.");

        if (!CODE.matcher(fromCode).matches() || !CODE.matcher(toCode).matches())
            throw new ValidationException("Kodovi aerodroma moraju biti u formatu AAA (3 velika slova).");

        if (fromCode.equals(toCode))
            throw new ValidationException("Polazni i dolazni aerodrom ne mogu biti isti.");

        if (db.findAirport(fromCode) == null)
            throw new ValidationException("Nepostojeći polazni aerodrom: " + fromCode + ".");
        if (db.findAirport(toCode) == null)
            throw new ValidationException("Nepostojeći dolazni aerodrom: " + toCode + ".");

        if (hhmm == null || !HHMM.matcher(hhmm).matches())
            throw new ValidationException("Vreme mora biti HH:MM (00:00–23:59).");

        if (duration <= 0)
            throw new ValidationException("Trajanje leta mora biti > 0 minuta.");
    }
}
