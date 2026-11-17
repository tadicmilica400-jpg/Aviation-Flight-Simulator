package logic;

import java.util.*;
import model.Flight;
import model.DataBase;

public final class SimulationEngine {

    // Listener
    public static final class AirborneState {
        public final Flight flight;
        public final double x; // world coords
        public final double y;
        public AirborneState(Flight f, double x, double y) { this.flight=f; this.x=x; this.y=y; }
    }

    public interface SimulationListener {
        void onTick(long simMinutes, List<AirborneState> aircraft);
        void onFinished(); // poziva se kada se simulacija sama zavrsi
    }

    private static final class FlightPlan {
        final Flight f;
        final int departMin;
        final int arriveMin;
        FlightPlan(Flight f, int departMin) {
            this.f = f;
            this.departMin = departMin;
            this.arriveMin = departMin + Math.max(1, f.getDurationMin());
        }
    }

    private final DataBase db;
    private final List<FlightPlan> plans = new ArrayList<>();
    private final List<SimulationListener> listeners = new ArrayList<>();

    /*// --- REROUTE SUPPORT (runtime bez novih javnih klasa) ---
    private static final class RerouteOverride {
        final model.Airport newTo;
        final long startMin;         // kada je naređen reroute (sim minute)
        final double startX, startY; // pozicija u tom trenutku
        final long arriveMin;        // kada treba da sleti na newTo (start + trajanje)

        RerouteOverride(model.Airport newTo, long startMin, double startX, double startY, long arriveMin) {
            this.newTo = newTo;
            this.startMin = startMin;
            this.startX = startX;
            this.startY = startY;
            this.arriveMin = arriveMin;
        }
    }
    // flight -> override
    private final Map<Flight, RerouteOverride> overrides = new HashMap<>();*/

    // vreme
    private static final long TICK_MS = 200L;
    private static final int  MIN_PER_TICK = 2;
    private static final int  DAY_MIN = 24*60;

    private java.util.Timer timer;
    private volatile boolean running = false;
    private volatile boolean finished = false;

    // simulaciono vreme se računa linearno
    private volatile long simMinutes = 0;

    public SimulationEngine(DataBase store) { this.db = store; }

    public synchronized void addListener(SimulationListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }
    public synchronized void removeListener(SimulationListener l) { listeners.remove(l); }

    public synchronized boolean isRunning() { return running; }
    public synchronized long getSimMinutes() { return simMinutes; }

    // === PUBLIC helper-i za PhaseC ===

   /* /** Otkazuje zadate letove iz rasporeda (da stvarno ne poleću / ne sleću).
    public synchronized void cancelFlights(java.util.Set<Flight> toRemove) {
        if (toRemove == null || toRemove.isEmpty()) return;
        plans.removeIf(p -> toRemove.contains(p.f));
        overrides.keySet().removeIf(toRemove::contains);
    }

    /* Vrši realni reroute jednog leta ka newTo, od trenutne (currX,currY) i trenutka nowSimMin. 
    public synchronized void rerouteFlight(Flight f, model.Airport newTo, long nowSimMin, double currX, double currY) {
        if (f == null || newTo == null) return;

        // brzina = (originalna distanca / originalno trajanje)
        double origDx = f.getTo().getX() - f.getFrom().getX();
        double origDy = f.getTo().getY() - f.getFrom().getY();
        double origDist = Math.sqrt(origDx*origDx + origDy*origDy);
        double speed = origDist / Math.max(1, f.getDurationMin()); // jedinica/min

        // vreme do novog aerodroma (distanca / speed)
        double dx = newTo.getX() - currX;
        double dy = newTo.getY() - currY;
        double distToNew = Math.sqrt(dx*dx + dy*dy);
        long newDur = Math.max(1L, Math.round(distToNew / Math.max(1e-9, speed)));

        long arriveAt = nowSimMin + newDur;

        overrides.put(f, new RerouteOverride(newTo, nowSimMin, currX, currY, arriveAt));
    }

     Trenutna destinacija (originalni TO ili override TO). 
    public synchronized model.Airport currentDestination(Flight f) {
        RerouteOverride ov = overrides.get(f);
        return (ov != null) ? ov.newTo : f.getTo();
    }*/

    // kontrola
    public synchronized void start() {
        if (running) return;
        if (finished) { finished = false; }
        if (plans.isEmpty()) buildScheduleSlots();

        running = true;
        timer = new java.util.Timer("SimTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { tick(); }
        }, 0L, TICK_MS);
    }

    public synchronized void pause() {
        if (!running) return;
        running = false;
        if (timer != null) { timer.cancel(); timer = null; }
    }

    public synchronized void reset() {
        pause();
        simMinutes = 0;
        finished = false;
        plans.clear();
        //overrides.clear(); // <<< važno: očisti sve reroute override-e
        fireTick(Collections.emptyList());
    }

    // raspored sa slotovima 10 min
    private void buildScheduleSlots() {
        plans.clear();

        List<Flight> req = new ArrayList<>(db.getFlights());
        req.sort(Comparator.comparingInt(f -> parseHHmmToMinutes(f.getDepartureHHmm())));

        Map<String, Integer> nextFreeSlot = new HashMap<>();

        for (Flight f : req) {
            int reqMin = parseHHmmToMinutes(f.getDepartureHHmm());
            int requestedSlot = reqMin / 10;

            int free = Math.max(requestedSlot,
                    nextFreeSlot.getOrDefault(f.getFrom().getCode(), requestedSlot));

            int scheduledDepartMin = free * 10;
            nextFreeSlot.put(f.getFrom().getCode(), free + 1);

            plans.add(new FlightPlan(f, scheduledDepartMin));
        }

        plans.sort(Comparator.comparingInt(p -> p.departMin));
    }

    private static int parseHHmmToMinutes(String hhmm) {
        try {
            String[] t = hhmm.split(":");
            int h = Integer.parseInt(t[0].trim());
            int m = Integer.parseInt(t[1].trim());
            return h*60 + m;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Pogrešan HH:mm: " + hhmm);
        }
    }

    /*// simulacioni korak
    private void tick() {
        long now;
        synchronized (this) {
            if (!running) return;
            simMinutes += MIN_PER_TICK;
            now = simMinutes;
        }

        List<AirborneState> airborne = new ArrayList<>();
        boolean anyPotentialLeft = false;

        for (FlightPlan p : plans) {
            if (p.departMin < DAY_MIN) {
                if (now < p.departMin) anyPotentialLeft = true;
            }

            // override, ako postoji
            RerouteOverride ov;
            synchronized (this) { ov = overrides.get(p.f); }

            long effectiveArriveMin = (ov != null) ? ov.arriveMin : p.arriveMin;

            // Avion je u vazduhu između depart i ACTUAL arrive (original ili override)
            if (p.departMin < DAY_MIN && now >= p.departMin && now < effectiveArriveMin) {
                double x, y;

                if (ov == null || now < ov.startMin) {
                    // pre reroute-a ili ga nema → klasično FROM→TO
                    double t = (now - p.departMin) / (double)(p.arriveMin - p.departMin);
                    t = clamp01(t);
                    x = lerp(p.f.getFrom().getX(), p.f.getTo().getX(), t);
                    y = lerp(p.f.getFrom().getY(), p.f.getTo().getY(), t);
                } else {
                    // posle reroute-a → od startX,startY ka newTo
                    double t = (now - ov.startMin) / (double)(ov.arriveMin - ov.startMin);
                    t = clamp01(t);
                    x = lerp(ov.startX, ov.newTo.getX(), t);
                    y = lerp(ov.startY, ov.newTo.getY(), t);
                }

                airborne.add(new AirborneState(p.f, x, y));
            }
        }

        fireTick(airborne);

        // kraj simulacije kad istekne dan i nema više aviona/potencijala
        if (now >= DAY_MIN && airborne.isEmpty() && !anyPotentialLeft) {
            synchronized (this) {
                if (running) {
                    running = false;
                    finished = true;
                    if (timer != null) { timer.cancel(); timer = null; }
                }
            }
            fireFinished();
        }
    }*/
  //simulacioni korak
    private void tick() {
        long now;
        synchronized (this) {
            if (!running) return;
            simMinutes += MIN_PER_TICK;
            now = simMinutes;
        }

        // nove polaske ne računamo ako departMin >= DAY_MIN
        // ali let koji je poletio pre DAY_MIN sme da završi i posle 24h
        List<AirborneState> airborne = new ArrayList<>();
        boolean anyPotentialLeft = false;

        for (FlightPlan p : plans) {
            if (p.departMin < DAY_MIN) {
                if (now < p.departMin) anyPotentialLeft = true;
            }
            // avioni u vazduhu:
            //   slučaj A: polazak < DAY_MIN i departMin <= now < arriveMin
            //   slučaj B: (ako bi postojao depart >= DAY_MIN) -> IGNORE (zabranjeni posle 24h)
            if (p.departMin < DAY_MIN && now >= p.departMin && now < p.arriveMin) {
                double t = (now - p.departMin) / (double)(p.arriveMin - p.departMin);
                double x = lerp(p.f.getFrom().getX(), p.f.getTo().getX(), t);
                double y = lerp(p.f.getFrom().getY(), p.f.getTo().getY(), t);
                airborne.add(new AirborneState(p.f, x, y));
            }
            // ako je let poletio pre DAY_MIN ali se završava posle 24h : i dalje gore dok ne sleti
            if (p.departMin < DAY_MIN && now < p.arriveMin) {
            }
        }

        fireTick(airborne);

        // ako je pređen kraj dana i NEMA više aviona u vazduhu i ne čekaju novi pre-24h polasci: gotovo.
        if (now >= DAY_MIN && airborne.isEmpty() && !anyPotentialLeft) {
            synchronized (this) {
                if (running) {
                    running = false;
                    finished = true;
                    if (timer != null) { timer.cancel(); timer = null; }
                }
            }
            fireFinished();
        }
    }


    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double clamp01(double v) { return (v < 0) ? 0 : (v > 1 ? 1 : v); }

    private void fireTick(List<AirborneState> states) {
        List<SimulationListener> copy;
        long minutes;
        synchronized (this) { copy = new ArrayList<>(listeners); minutes = simMinutes; }
        for (SimulationListener l : copy) {
            try { l.onTick(minutes, states); } catch (Exception ignored) {}
        }
    }

    private void fireFinished() {
        List<SimulationListener> copy;
        synchronized (this) { copy = new ArrayList<>(listeners); }
        for (SimulationListener l : copy) {
            try { l.onFinished(); } catch (Exception ignored) {}
        }
    }
}