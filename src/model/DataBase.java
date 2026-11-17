package model;

import java.util.*;

public class DataBase {
	private final Map<String, Airport> airports = new LinkedHashMap<>();
    private final List<Flight> flights = new ArrayList<>();
    
    public Collection<Airport> getAirports() { return airports.values(); }
    public List<Flight> getFlights() { return flights; }
    public Airport findAirport(String code) { return airports.get(code); }

    public void putAirport(Airport a) { airports.put(a.getCode(), a); }
    public void addFlight(Flight f) { flights.add(f); }

    public void clear() { airports.clear(); flights.clear();}
}
