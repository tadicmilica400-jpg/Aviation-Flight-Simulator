package model;

public class Flight {
	
	private final Airport from;
	private final Airport to;
	private final String departureHHmm;
    private final int durationMin;
    
	public Flight(Airport from, Airport to, String departureHHmm, int durationMin) {
		this.from = from;
		this.to = to;
		this.departureHHmm = departureHHmm;
		this.durationMin = durationMin;
	}
    
	public Airport getFrom() { return from; }
    public Airport getTo() { return to; }
    public String getDepartureHHmm() { return departureHHmm; }
    public int getDurationMin() { return durationMin; }

	@Override
	public String toString() {
		return "Flight "+ from + "-" + to + " , departure:" + departureHHmm + ", duration:"
				+ durationMin;
	}
}
