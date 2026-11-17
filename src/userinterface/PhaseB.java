package userinterface;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import model.Airport;
import logic.AvioInfo;

public class PhaseB extends Panel {
    private static final long serialVersionUID = 1L;
    private final AvioInfo controller;

    private final AirportMap map = new AirportMap();
    private final Panel listPanel = new Panel(new GridLayout(0,1,0,4));
    private final ScrollPane listScroll = new ScrollPane();

    private final java.util.Map<Airport, Checkbox> visibility = new LinkedHashMap<>();

    private Airport selected = null;
    private boolean blinkOn = true;
    private java.util.Timer blinkTimer;

    public static boolean pausedInactivity = false;

    public PhaseB(AvioInfo controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8,8));

        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        Button btnBuild = new Button("Napravi mapu");
        btnBuild.setBackground(new Color(231,84,128));
        top.add(btnBuild);
        add(top, BorderLayout.NORTH);
        btnBuild.addActionListener(e -> buildMapNow());

        listScroll.add(listPanel);
        Panel left = new Panel(new BorderLayout());
        left.setPreferredSize(new Dimension(300, 400));
        left.add(new Label("Airports:"), BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        map.setBackground(new Color(155,211,221));
        map.setPreferredSize(new Dimension(650, 650));
        add(map, BorderLayout.CENTER);

        map.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Airport clicked = map.findAirportAt(e.getPoint());
                if (clicked == null) return;
                if (clicked.equals(selected)) setSelected(null);
                else setSelected(clicked);
            }
        });
    }

    /** Pozovi kad se baza promeni (ako želiš auto refresh). */
    public void onDataChanged() {
        if (!visibility.isEmpty()) reloadList();
        map.setAirports(controller.store().getAirports());
        pushVisibleToMap();
        map.repaint();
    }

    private void buildMapNow() {
        if (controller.store().getAirports().isEmpty()) {
            showMessage("Nema podataka","Nema aerodroma u bazi. Dodaj ili učitaj CSV pa probaj ponovo.",false);
            return;
        }
        reloadList();
        map.setAirports(controller.store().getAirports());
        pushVisibleToMap();
        map.repaint();
    }

    private void reloadList() {
        listPanel.removeAll();
        visibility.clear();
        setSelected(null);

        java.util.List<Airport> list = new ArrayList<>();
        for (Airport a : controller.store().getAirports()) list.add(a);
        list.sort(Comparator.comparing(Airport::getCode));

        if (list.isEmpty()) {
            listPanel.add(new Label(" (no airports) "));
        } else {
            for (Airport a : list) {
                Checkbox cb = new Checkbox(a.getCode()+" - "+a.getName()+"  ("+a.getX()+", "+a.getY()+")", true);
                Panel row = new Panel(new BorderLayout());
                row.add(cb, BorderLayout.CENTER);
                cb.setPreferredSize(new Dimension(20, 20));
                row.setBackground(new Color(250,250,250));
                row.setPreferredSize(new Dimension(320, 15));
                visibility.put(a, cb);
                cb.addItemListener(ev -> {
                    if (!cb.getState() && a.equals(selected)) setSelected(null);
                    pushVisibleToMap();
                });
                listPanel.add(row);
            }
        }

        listPanel.validate(); listPanel.repaint();
        listScroll.validate(); listScroll.repaint();
        this.validate(); this.repaint();

        pushVisibleToMap();
        map.repaint();
    }

    private void pushVisibleToMap() {
        java.util.Set<Airport> vis = new java.util.HashSet<>();
        for (Map.Entry<Airport, Checkbox> e : visibility.entrySet())
            if (e.getValue().getState()) vis.add(e.getKey());
        map.setVisibleAirports(vis);
    }

    // --- selekcija/treptanje ---
    private void setSelected(Airport a) {
        selected = a;
        map.setSelected(a);
        if (selected != null) startBlink();
        else stopBlink();
        map.repaint();
    }

    private void startBlink() {
        stopBlink();
        pausedInactivity = true;
        blinkOn = true;
        map.setBlinkOn(true);

        blinkTimer = new java.util.Timer(true);
        blinkTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                if (selected == null) return;
                blinkOn = !blinkOn;
                map.setBlinkOn(blinkOn);
                EventQueue.invokeLater(() -> {
                    Rectangle r = map.boundsOf(selected);
                    if (r != null) map.repaint(r.x, r.y, r.width, r.height);
                    else map.repaint();
                });
            }
        }, 0, 400);
    }

    private void stopBlink() {
        pausedInactivity = false;
        if (blinkTimer != null) { blinkTimer.cancel(); blinkTimer = null; }
        blinkOn = true;
        map.setBlinkOn(true);
    }

    private void showMessage(String title, String message, boolean error) {
        final Dialog d = new Dialog((Frame) null, title, true);
        d.setLayout(new BorderLayout(10,10));
        d.setBackground(new Color(231,84,128));

        TextArea ta = new TextArea(message, 5, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        ta.setEditable(false);
        ta.setBackground(Color.pink);
        d.add(ta, BorderLayout.CENTER);

        Panel south = new Panel(new FlowLayout(FlowLayout.RIGHT));
        Button ok = new Button("OK");
        ok.setBackground(new Color(231,84,128));
        south.add(ok);
        d.add(south, BorderLayout.SOUTH);

        if (error) Toolkit.getDefaultToolkit().beep();

        ok.addActionListener(e -> d.dispose());
        d.pack();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        d.setLocation((scr.width - d.getWidth())/2, (scr.height - d.getHeight())/2);
        d.setVisible(true);
    }
 // uredno gašenje treptanja pri zatvaranju prozora
    public void shutdown() {
        try { stopBlink(); } catch (Throwable ignore) {}
    }

}
