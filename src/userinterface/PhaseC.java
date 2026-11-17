package userinterface;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import logic.SimulationEngine;
import logic.SimulationEngine.AirborneState;
import logic.AvioInfo;
import logic.InactivityWatch;

public class PhaseC extends Panel implements SimulationEngine.SimulationListener {

    private static final long serialVersionUID = 1L;

    private final AvioInfo controller;
    private final SimulationEngine engine;
    private final InactivityWatch inactivity;

    private final ControlBar controls = new ControlBar();
    private final SimMap map = new SimMap();

    private volatile long simMinutes = 0;
    private volatile List<AirborneState> lastStates = new ArrayList<>();
    /*private volatile SimulationEngine.AirborneState picked = null;
    private volatile boolean pickedLocked = false;
    private volatile model.Flight pickedFlight = null;

    // STORM (ostavljeno kao i ranije)
    private volatile boolean stormActive = false;
    private volatile boolean stormGrowing = false;
    private volatile double  stormCx = 0, stormCy = 0;
    private volatile double  stormR  = 0;
    private java.util.Timer  stormTimer = null;

    // UI RETURN preko oluje (ostavljeno)
    private static final class UIReturnLeg {
        final double startX, startY;
        final long   startMin;
        final long   endMin;
        UIReturnLeg(double sx, double sy, long sMin, long eMin) {
            this.startX = sx; this.startY = sy; this.startMin = sMin; this.endMin = eMin;
        }
    }
    private final java.util.Map<model.Flight, UIReturnLeg> uiReturns = new java.util.HashMap<>();

    // AERODROME CHECKBOXES
    private final Panel airportPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final java.util.Map<model.Airport, Checkbox> airportChecks = new java.util.HashMap<>();
    private final java.util.Set<model.Airport> disabledAirports = new java.util.HashSet<>();*/

    public PhaseC(AvioInfo controller, InactivityWatch inactivity) {
        this.controller = controller;
        this.inactivity = inactivity;
        this.engine = new SimulationEngine(controller.store());
        this.engine.addListener(this);

        setLayout(new BorderLayout(8,8));
        add(controls, BorderLayout.NORTH);
        add(map, BorderLayout.CENTER);
        //add(airportPanel, BorderLayout.SOUTH);

        map.setBackground(new Color(155,211,221));
        //airportPanel.setBackground(new Color(240, 240, 240));

        //refreshAirportCheckboxes();
        refreshAirports();
        map.repaint();
    }

    @Override
    public void onTick(long simMinutes, List<AirborneState> aircraft) {
        this.simMinutes = simMinutes;

        /*// Kopija liste za UI
        if (aircraft == null) aircraft = new ArrayList<>();
        else aircraft = new ArrayList<>(aircraft);

        // 1) BLOCK polazaka sa ugašenog FROM samo ako su na rampi
        final double FROM_EPS = 0.75;
        java.util.Iterator<AirborneState> itBlock = aircraft.iterator();
        while (itBlock.hasNext()) {
            AirborneState s = itBlock.next();
            if (s == null || s.flight == null) continue;

            model.Airport from = s.flight.getFrom();
            if (from != null && disabledAirports.contains(from)) {
                if (dist(s.x, s.y, from.getX(), from.getY()) <= FROM_EPS) {
                    itBlock.remove();
                    if (pickedFlight == s.flight) {
                        picked = null;
                        pickedFlight = null;
                        pickedLocked = false;
                    }
                }
            }
        }

        // 2) REROUTE letova koji idu ka ugašenom TO → NAJBLIŽI OD TRENUTNE POZICIJE
        for (int i = 0; i < aircraft.size(); i++) {
            AirborneState s = aircraft.get(i);
            if (s == null || s.flight == null) continue;

            model.Airport to = s.flight.getTo();
            if (to != null && disabledAirports.contains(to)) {
                model.Airport newDest = findNearestEnabledAirportFromPoint(s.x, s.y);
                if (newDest != null && newDest != to) {
                    engine.rerouteFlight(s.flight, newDest, simMinutes, s.x, s.y);
                }
            }
        }

        // 3) Oluja → UI povratak (ostavljeno)
        if (stormActive && stormR > 0 && !aircraft.isEmpty()) {
            double cx = stormCx, cy = stormCy, rr = stormR;
            for (SimulationEngine.AirborneState s : aircraft) {
                if (s == null || s.flight == null) continue;
                if (uiReturns.containsKey(s.flight)) continue;

                double dx = s.x - cx, dy = s.y - cy;
                if (Math.sqrt(dx*dx + dy*dy) <= rr) {
                    int departMin = parseHHmmToMinutes(s.flight.getDepartureHHmm());
                    long timeSpent = Math.max(1, simMinutes - departMin);
                    long startMin = simMinutes;
                    long endMin   = startMin + timeSpent;

                    uiReturns.put(s.flight, new UIReturnLeg(s.x, s.y, startMin, endMin));
                }
            }
        }*/

        this.lastStates = aircraft;

        /*// 4) Održavanje selekcije
        if (pickedFlight != null) {
            SimulationEngine.AirborneState current = null;
            for (SimulationEngine.AirborneState s : aircraft) {
                if (s.flight == pickedFlight) { current = s; break; }
            }
            if (current != null) picked = current;
            else { picked = null; pickedFlight = null; pickedLocked = false; }
        }*/

        EventQueue.invokeLater(map::repaint);
    }

    @Override
    public void onFinished() {
        if (inactivity != null) {
            inactivity.setExternallyPaused(false);
            inactivity.reset();
        }
        EventQueue.invokeLater(() -> showMessage(
            "Simulacija završena",
            "Dan je završen. Svi letovi koji su poleteli su sleteli.\nSimulacija je automatski zaustavljena.",
            false
        ));
    }

    private class ControlBar extends Panel {
        private static final long serialVersionUID = 1L;
        private final Label  lblTime  = new Label("Time: 00:00");
        private final Button btnStart = new Button("Start");
        private final Button btnPause = new Button("Pause");
        private final Button btnReset = new Button("Reset");

        ControlBar() {
            super(new FlowLayout(FlowLayout.LEFT, 8, 8));
            add(btnStart); add(btnPause); add(btnReset); add(lblTime);
            Color pink = new Color(231,84,128);
            btnStart.setBackground(pink);
            btnPause.setBackground(pink);
            btnReset.setBackground(pink);

            btnStart.addActionListener(e -> {
                try {
                    int ac = controller.store().getAirports().size();
                    int fc = controller.store().getFlights().size();
                    if (ac < 2) { showMessage("Nema aerodroma","Potrebna su najmanje DVA aerodroma.",false); return; }
                    if (fc == 0) { showMessage("Nema letova","Dodaj letove ili učitaj CSV.",false); return; }

                    engine.start();
                    if (inactivity != null) inactivity.setExternallyPaused(true);
                } catch (Exception ex) { showMessage("Greška", safeMsg(ex), true); }
            });

            btnPause.addActionListener(e -> {
                try { engine.pause(); } catch (Exception ex) { showMessage("Greška", safeMsg(ex), true); }
            });

            btnReset.addActionListener(e -> {
                try {
                    engine.reset();
                    if (inactivity != null) {
                        inactivity.setExternallyPaused(false);
                        inactivity.reset();
                    }
                    lblTime.setText("Time: 00:00");
                    simMinutes = 0;
                    lastStates = new ArrayList<>();
                    /*picked = null;
                    pickedFlight = null;
                    pickedLocked = false;

                    stormActive = false;
                    stormGrowing = false;
                    stormR = 0;
                    if (stormTimer != null) {
                        try { stormTimer.cancel(); } catch (Throwable ignore) {}
                        stormTimer = null;
                    }

                    disabledAirports.clear();
                    uiReturns.clear();
                    refreshAirportCheckboxes();*/

                    map.repaint();
                } catch (Exception ex) { showMessage("Greška", safeMsg(ex), true); }
            });
        }

        void updateTime(long minutes) {
            int hh = (int)(minutes / 60L) % 24;
            int mm = (int)(minutes % 60L);
            lblTime.setText(String.format("Time: %02d:%02d", hh, mm));
        }
    }

    private class SimMap extends AirportMap {
        private static final long serialVersionUID = 1L;
        private final int PLANE_DIAM = 10;

        @Override public Dimension getPreferredSize() { return new Dimension(650, 650); }
        @Override public void update(Graphics g) { paint(g); }

        /*SimMap() {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    AirborneState hit = findPlaneAt(e.getPoint(), 8);

                    if (hit != null) {
                        picked = hit;
                        pickedFlight = hit.flight;
                        pickedLocked = true;
                        repaint();

                        if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                            String msg = "Let: " + depString(hit)
                                       + "\nX = " + String.format("%.2f", hit.x)
                                       + "\nY = " + String.format("%.2f", hit.y);
                            showMessage("Info o letu", msg, false);
                        }
                    } else {
                        picked = null;
                        pickedFlight = null;
                        pickedLocked = false;
                    }
                }
            });

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                    if (pickedLocked) return;
                    AirborneState hit = findPlaneAt(e.getPoint(), 12);
                    if (hit != null) {
                        picked = hit;
                        pickedFlight = hit.flight;
                    } else {
                        picked = null;
                        pickedFlight = null;
                    }
                }
            });

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    if (pickedLocked) return;
                    picked = null;
                    pickedFlight = null;
                }

                @Override public void mousePressed(java.awt.event.MouseEvent e) {
                    int w = getWidth(), h = getHeight();
                    double[] world = screenToWorld(e.getX(), e.getY(), w, h);
                    stormCx = world[0];
                    stormCy = world[1];

                    stormActive  = true;
                    stormGrowing = true;
                    stormR = 0;

                    if (stormTimer != null) {
                        try { stormTimer.cancel(); } catch (Throwable ignore) {}
                    }
                    stormTimer = new java.util.Timer(true);
                    stormTimer.scheduleAtFixedRate(new java.util.TimerTask() {
                        @Override public void run() {
                            if (!stormGrowing) return;
                            stormR += 0.6;
                            EventQueue.invokeLater(() -> repaint());
                        }
                    }, 0, 100);
                }

                @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                    stormGrowing = false;
                    if (stormTimer != null) {
                        try { stormTimer.cancel(); } catch (Throwable ignore) {}
                        stormTimer = null;
                    }
                    repaint();
                }
            });

            repaint();
        }*/

        /*@Override
        public void paint(Graphics g0) {
            super.paint(g0);

            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // Avioni – engine sada vraća već rerutirane pozicije
            java.util.List<AirborneState> curStates = lastStates;
            g.setColor(Color.blue);
            for (AirborneState s : curStates) {
                Point p = worldToScreen(s.x, s.y, w, h);
                g.fillOval(p.x - PLANE_DIAM/2, p.y - PLANE_DIAM/2, PLANE_DIAM, PLANE_DIAM);
            }

            // Avioni 30 min pre polaska (na FROM) — NE crtamo ako je ijedan aerodrom isključen
            if (disabledAirports.isEmpty()) {
                long minsDay = simMinutes % (24 * 60);
                java.util.List<model.Flight> flightsAll = controller.store().getFlights();
                if (flightsAll != null) {
                    for (model.Flight f : flightsAll) {
                        if (f.getFrom() != null && disabledAirports.contains(f.getFrom())) continue;

                        int depart = parseHHmmToMinutes(f.getDepartureHHmm());
                        int appearFrom = depart - 30;

                        boolean show;
                        if (appearFrom >= 0) show = (minsDay >= appearFrom && minsDay < depart);
                        else                 show = (minsDay < depart);

                        if (show && f.getFrom() != null) {
                            Point pFrom = worldToScreen(f.getFrom().getX(), f.getFrom().getY(), w, h);
                            int d = 8;
                            g.setColor(new Color(30, 144, 255));
                            g.fillOval(pFrom.x - d/2, pFrom.y - d/2, d, d);
                            g.setColor(Color.DARK_GRAY);
                            g.drawOval(pFrom.x - d/2, pFrom.y - d/2, d, d);
                        }
                    }
                }
            }

            // Planirane i pređene putanje
            if (curStates != null) {
                Stroke oldStroke = g.getStroke();
                BasicStroke solid = new BasicStroke(2.0f);

                for (AirborneState s : curStates) {
                    if (s == null || s.flight == null || s.flight.getFrom() == null || s.flight.getTo() == null) continue;

                    Point pFrom = worldToScreen(s.flight.getFrom().getX(), s.flight.getFrom().getY(), w, h);

                    // Trenutni cilj (može biti originalni TO ili override TO u engine-u)
                    model.Airport destA = engine.currentDestination(s.flight);
                    Point pTo   = worldToScreen(destA.getX(), destA.getY(), w, h);
                    Point pNow  = worldToScreen(s.x, s.y, w, h);

                    g.setColor(new Color(120,120,120));
                    g.setStroke(solid);
                    g.drawLine(pFrom.x, pFrom.y, pTo.x, pTo.y);

                    g.setColor(Color.blue);
                    g.setStroke(solid);
                    g.drawLine(pFrom.x, pFrom.y, pNow.x, pNow.y);
                }

                g.setStroke(oldStroke);
            }

            // Vreme
            long mins = simMinutes % (24 * 60);
            controls.updateTime(mins);

            // Info balon (računamo remaining prema trenutnom cilju iz engine-a)
            AirborneState sel = (picked != null)
                    ? picked
                    : (lastStates != null && !lastStates.isEmpty() ? lastStates.get(0) : null);
            if (sel != null) {
                Point p = worldToScreen(sel.x, sel.y, w, h);

                int remainingMin = 0;
                try {
                    int durMin = Math.max(1, sel.flight.getDurationMin());
                    double fx = sel.flight.getFrom().getX(), fy = sel.flight.getFrom().getY();
                    model.Airport destL = engine.currentDestination(sel.flight);
                    double tx = destL.getX(), ty = destL.getY();
                    double total = Math.max(1e-9, dist(fx, fy, tx, ty));
                    double prog  = clamp01(dist(fx, fy, sel.x, sel.y) / total);
                    double leftFrac = 1.0 - prog;
                    remainingMin = (int)Math.round(leftFrac * durMin);
                } catch (Throwable ignore) {
                    remainingMin = 0;
                }

                String text = String.format("dep %s | x=%.2f y=%.2f | mins %s",
                        depString(sel), sel.x, sel.y, remainingMin);

                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(text) + 10;
                int th = fm.getHeight() + 6;

                int bx = p.x + 12;
                int by = p.y - th - 6;
                if (bx + tw > w) bx = Math.max(4, w - tw - 4);
                if (by < 0)      by = p.y + 14;

                g.setColor(new Color(255, 255, 230));
                g.fillRect(bx, by, tw, th);
                g.setColor(Color.GRAY);
                g.drawRect(bx, by, tw, th);

                g.setColor(Color.BLACK);
                g.drawString(text, bx + 5, by + th - 6);
            }

            // STORM crveni krug (vizuelno)
            if (stormActive && stormR > 0) {
                Point c = worldToScreen(stormCx, stormCy, w, h);
                int rPx = (int)Math.round(stormR * (w - 1) / 180.0);

                Graphics2D g2 = (Graphics2D) g0;
                java.awt.Composite oldC = g2.getComposite();
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(Color.RED);
                g2.fillOval(c.x - rPx, c.y - rPx, 2*rPx, 2*rPx);
                g2.setComposite(oldC);

                g2.setColor(Color.RED);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(c.x - rPx, c.y - rPx, 2*rPx, 2*rPx);
            }

            // Ugašeni aerodromi (crni kvadrat)
            if (!disabledAirports.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g0;
                for (model.Airport a : disabledAirports) {
                    Point p = worldToScreen(a.getX(), a.getY(), w, h);
                    int s = 10;
                    g2.setColor(Color.BLACK);
                    g2.fillRect(p.x - s/2, p.y - s/2, s, s);
                }
            }
        }*/
        
        @Override
        public void paint(Graphics g0) {
            super.paint(g0); // prvo AirportMap: pozadina + aerodromi

            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // avioni
            g.setColor(Color.blue);
            List<AirborneState> states = lastStates;
            for (AirborneState s : states) {
                Point p = worldToScreen(s.x, s.y, w, h);
                g.fillOval(p.x - PLANE_DIAM/2, p.y - PLANE_DIAM/2, PLANE_DIAM, PLANE_DIAM);
            }

            // vreme (mod 24h)
            long mins = simMinutes % (24 * 60);
            controls.updateTime(mins);
        }


        private Point worldToScreen(double X, double Y, int w, int h) {
            double sx = (X + 90.0) * (w - 1) / 180.0;
            double sy = (90.0 - Y) * (h - 1) / 180.0;
            return new Point((int)Math.round(sx), (int)Math.round(sy));
        }

        /*private AirborneState findPlaneAt(Point p, int radiusPx) {
            int w = getWidth(), h = getHeight();
            AirborneState best = null;
            double bestD2 = Double.MAX_VALUE;
            for (AirborneState s : lastStates) {
                Point sp = worldToScreen(s.x, s.y, w, h);
                double dx = sp.x - p.x, dy = sp.y - p.y, d2 = dx*dx + dy*dy;
                if (d2 <= radiusPx * radiusPx && d2 < bestD2) {
                    bestD2 = d2; best = s;
                }
            }
            return best;
        }

        private String depString(AirborneState s) {
            try {
                if (s.flight != null && s.flight.getDepartureHHmm() != null) {
                    return s.flight.getDepartureHHmm();
                }
            } catch (Throwable ignore) {}
            return "??:??";
        }*/
    }

    public void refreshAirports() {
        java.util.Collection<model.Airport> all = controller.store().getAirports();
        map.setAirports(all);
        map.setVisibleAirports(all);
        map.repaint();
    }

    /*private void refreshAirportCheckboxes() {
        airportPanel.removeAll();
        airportChecks.clear();

        for (model.Airport a : controller.store().getAirports()) {
            Checkbox cb = new Checkbox(a.getCode(), !disabledAirports.contains(a));
            cb.setBackground(new Color(240, 240, 240));
            cb.addItemListener(e -> {
                boolean enabled = cb.getState();
                if (enabled) {
                    disabledAirports.remove(a);
                } else {
                    disabledAirports.add(a);

                    // 1) trajno ukloni letove sa/ka ovom aerodromu iz STORE-a
                    java.util.List<model.Flight> all = controller.store().getFlights();
                    java.util.Set<model.Flight> toRemove = new java.util.HashSet<>();
                    for (model.Flight f : new java.util.ArrayList<>(all)) {
                        if (f == null) continue;
                        if (f.getFrom() == a || f.getTo() == a) toRemove.add(f);
                    }
                    if (!toRemove.isEmpty()) {
                        all.removeAll(toRemove);
                        try { engine.cancelFlights(toRemove); } catch (Throwable ignore) {}
                    }

                    // 2) Preusmeri ODMAH sve avione u vazduhu koji lete KA ovom aerodromu → najbliži od trenutne tačke
                    for (AirborneState s : lastStates) {
                        if (s != null && s.flight != null && s.flight.getTo() == a) {
                            model.Airport newDest = findNearestEnabledAirportFromPoint(s.x, s.y);
                            if (newDest != null) {
                                engine.rerouteFlight(s.flight, newDest, simMinutes, s.x, s.y);
                            }
                        }
                    }

                    // 3) Ukloni iz prikaza one koji polaze SA ugašenog (ako su još na rampi)
                    java.util.Iterator<AirborneState> it = lastStates.iterator();
                    final double FROM_EPS = 0.75;
                    while (it.hasNext()) {
                        AirborneState s = it.next();
                        if (s != null && s.flight != null && s.flight.getFrom() == a) {
                            if (dist(s.x, s.y, a.getX(), a.getY()) <= FROM_EPS) it.remove();
                        }
                    }
                }
                map.repaint();
            });
            airportChecks.put(a, cb);
            airportPanel.add(cb);
        }

        airportPanel.doLayout();
        airportPanel.validate();
        airportPanel.repaint();
        PhaseC.this.validate();
        PhaseC.this.repaint();
    }*/

    public void shutdown() {
        try { engine.pause(); } catch (Throwable ignore) {}
        try { engine.reset(); } catch (Throwable ignore) {}
        if (inactivity != null) {
            inactivity.setExternallyPaused(false);
            inactivity.reset();
        }
    }

    private static void showMessage(String title, String message, boolean error) {
        final Dialog d = new Dialog((Frame) null, title, true);
        d.setLayout(new BorderLayout(10,10));
        d.setBackground(new Color(231,84,128));

        TextArea ta = new TextArea(message, 5, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        ta.setEditable(false);
        ta.setBackground(Color.pink);
        d.add(ta, BorderLayout.CENTER);

        Panel south = new Panel(new FlowLayout(FlowLayout.RIGHT));
        Button ok = new Button("OK"); ok.setBackground(new Color(231,84,128));
        south.add(ok); d.add(south, BorderLayout.SOUTH);

        if (error) Toolkit.getDefaultToolkit().beep();

        ok.addActionListener(e -> d.dispose());
        d.pack();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        d.setLocation((scr.width - d.getWidth())/2, (scr.height - d.getHeight())/2);
        d.setVisible(true);
    }

    private static String safeMsg(Exception ex) {
        String m = ex.getMessage();
        return (m == null || m.trim().isEmpty()) ? "Neočekivana greška." : m;
    }

    /*private static int parseHHmmToMinutes(String hhmm) {
        try {
            String[] t = hhmm.split(":");
            int h = Integer.parseInt(t[0].trim());
            int m = Integer.parseInt(t[1].trim());
            return h * 60 + m;
        } catch (Exception ex) {
            return 0;
        }
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx*dx + dy*dy);
    }

    private static double clamp01(double v) {
        return (v < 0) ? 0 : (v > 1 ? 1 : v);
    }

    private double[] screenToWorld(int px, int py, int w, int h) {
        double X = px * 180.0 / (w - 1) - 90.0;
        double Y = 90.0 - py * 180.0 / (h - 1);
        return new double[]{X, Y};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private model.Airport findNearestEnabledAirport(model.Airport ref) {
        double bestD = Double.MAX_VALUE;
        model.Airport best = null;
        for (model.Airport a : controller.store().getAirports()) {
            if (a == ref) continue;
            if (disabledAirports.contains(a)) continue;
            double dx = a.getX() - ref.getX();
            double dy = a.getY() - ref.getY();
            double d2 = dx*dx + dy*dy;
            if (d2 < bestD) { bestD = d2; best = a; }
        }
        return best;
    }

    // najbliži UKLJUČEN aerodrom u odnosu na trenutnu poziciju (x,y)
    private model.Airport findNearestEnabledAirportFromPoint(double x, double y) {
        double bestD2 = Double.MAX_VALUE;
        model.Airport best = null;
        for (model.Airport a : controller.store().getAirports()) {
            if (disabledAirports.contains(a)) continue;
            double dx = a.getX() - x, dy = a.getY() - y;
            double d2 = dx*dx + dy*dy;
            if (d2 < bestD2) { bestD2 = d2; best = a; }
        }
        return best;
    }

    public void refreshCheckboxesAndAirports() {
        refreshAirportCheckboxes();
        refreshAirports();
        map.repaint();
    }*/
}