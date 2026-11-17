package logic;

public class Test1 {

	public static void main(String[] args) {
		AvioInfo m = new AvioInfo();
        m.addAirport("Beograd", "BEG", 44.8, 20.5);
        m.addAirport("Nis",     "INI", 43.3, 21.9);
        m.addFlight("BEG", "INI", "09:30", 45);
        System.out.println("Airports: " + m.store().getAirports());
        System.out.println("Flights:  " + m.store().getFlights());
	}

}
