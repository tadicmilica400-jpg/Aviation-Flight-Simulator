package logic;

import model.*;
import inputoutput.*; 

public class AvioInfo {
	private final DataBase db = new DataBase();

    public DataBase store() { return db; }

    public Airport addAirport(String name, String code, double x, double y) {
        Validators.checkAirport(name, code, x, y, db);
        Airport a = new Airport(name, code, x, y);
        db.putAirport(a);
        return a;
    }

    public Flight addFlight(String fromCode, String toCode, String hhmm, int durationMin) {
        Validators.checkFlight(fromCode, toCode, hhmm, durationMin, db);
        Airport from = db.findAirport(fromCode);
        Airport to   = db.findAirport(toCode);
        Flight f = new Flight(from, to, hhmm, durationMin);
        db.addFlight(f);
        return f;
    }
    public void saveToFile(String filename) throws Exception {
        FileMenager.saveToFile(filename, db);
    }

    public void loadFromFile(String filename) throws Exception {
        FileMenager.loadFromFile(filename, db);
    }
}
