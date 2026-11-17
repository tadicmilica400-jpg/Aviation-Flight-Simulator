package userinterface;

import java.awt.*;
import java.util.*;
import model.Airport;

/**
 * Jednostavna AWT mapa: crta aerodrome kao kvadrate, podržava:
 * - setAirports(...)    : lista svih aerodroma
 * - setVisibleAirports(): podskup koji se prikazuje
 * - setSelected(...)    : trenutno selektovan aerodrom (treperi iz PhaseB)
 * - setBlinkOn(...)     : stanje treptanja (true/false)
 * - findAirportAt(p)    : hit-test po ekranu
 * - boundsOf(a)         : okvir za lokalni repaint
 */
public class AirportMap extends Canvas {

    private static final long serialVersionUID = 1L;

    private java.util.List<Airport> airports = new ArrayList<>();
    private java.util.Set<Airport> visible = new HashSet<>();

    private Airport selected = null;
    private boolean blinkOn = true;

    private static final int AIRPORT_SIZE = 10;
    private static final Color BG = new Color(155,211,221);

    public AirportMap() {
        setBackground(BG);
    }

    // --- API ---
 // AirportMap.java
    public void setAirports(Collection<Airport> all) {
        airports = (all == null) ? Collections.emptyList() : new ArrayList<>(all);

        // NOVO: ako još nemamo eksplicitno postavljen visible, prikaži sve
        if (visible == null || visible.isEmpty()) {
            visible = new HashSet<>(airports);
        }
        repaint();
    }

    public void setVisibleAirports(Collection<Airport> vis) {
        this.visible = (vis == null) ? new HashSet<>() : new HashSet<>(vis);
        repaint();
    }

    public void setSelected(Airport a) {
        this.selected = a;
        repaint();
    }

    public void setBlinkOn(boolean on) {
        this.blinkOn = on;
        repaint();
    }

    /** Hit-test na kvadrat aerodroma (samo za vidljive). */
    public Airport findAirportAt(Point m) {
        int w = getWidth(), h = getHeight();
        for (Airport a : airports) {
            if (!visible.contains(a)) continue;
            Point p = worldToScreen(a.getX(), a.getY(), w, h);
            Rectangle r = new Rectangle(p.x - AIRPORT_SIZE/2, p.y - AIRPORT_SIZE/2, AIRPORT_SIZE, AIRPORT_SIZE);
            if (r.contains(m)) return a;
        }
        return null;
    }

    /** Granice kvadrata (sa malim marg. za okvir) — korisno za parcijalni repaint. */
    public Rectangle boundsOf(Airport a) {
        if (a == null) return null;
        int w = getWidth(), h = getHeight();
        Point p = worldToScreen(a.getX(), a.getY(), w, h);
        int s = AIRPORT_SIZE;
        return new Rectangle(p.x - s/2 - 2, p.y - s/2 - 2, s + 4, s + 4);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(650, 650); }
    @Override public void update(Graphics g) { paint(g); } // smanji flicker

    @Override
    public void paint(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        g.setColor(getBackground());
        g.fillRect(0, 0, w, h);

        if (airports.isEmpty()) return;

        for (Airport a : airports) {
            if (!visible.contains(a)) continue;
            Point p = worldToScreen(a.getX(), a.getY(), w, h);

            boolean isSel = (selected != null && selected.equals(a));
            g.setColor(isSel && blinkOn ? Color.RED : Color.GRAY);
            g.fillRect(p.x - AIRPORT_SIZE/2, p.y - AIRPORT_SIZE/2, AIRPORT_SIZE, AIRPORT_SIZE);

            g.setColor(Color.DARK_GRAY);
            g.drawRect(p.x - AIRPORT_SIZE/2, p.y - AIRPORT_SIZE/2, AIRPORT_SIZE, AIRPORT_SIZE);

            g.setColor(Color.BLACK);
            g.drawString(a.getCode(), p.x + AIRPORT_SIZE/2 + 4, p.y + 5);
        }
    }

    // --- helpers ---

    private Point worldToScreen(double X, double Y, int w, int h) {
        double sx = (X + 90.0) * (w - 1) / 180.0;     // -90..90 -> 0..w-1
        double sy = (90.0 - Y) * (h - 1) / 180.0;     //  90..-90 -> 0..h-1 (gore je +90)
        return new Point((int)Math.round(sx), (int)Math.round(sy));
    }
    
}