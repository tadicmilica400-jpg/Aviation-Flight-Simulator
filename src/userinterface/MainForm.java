package userinterface;

import java.awt.*;
import java.awt.event.*;
import logic.AvioInfo;
import logic.InactivityWatch;

public class MainForm extends Frame {

    private static final long serialVersionUID = 1L;

    // --- modeli / niti ---
    private final InactivityWatch inactivity = new InactivityWatch();
    private Thread inactivityThread;

    // --- UI i faze ---
    private final CardLayout cards = new CardLayout();
    private final Panel cardPanel = new Panel(cards);
    private final AvioInfo info = new AvioInfo();

    private final PhaseA phaseA;
    private final PhaseB phaseB;
    private final PhaseC phaseC;

    public MainForm() {
        super("Aviation Simulation");
        setSize(750, 800);
        setLayout(new BorderLayout());

        // --- GORNJI DEO: 3 dugmeta ---
        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.setBackground(new Color(231, 84, 128));

        Button btnPhaseA = new Button("INPUT");
        Button btnPhaseB = new Button("MAP");
        Button btnPhaseC = new Button("SIMULATION");

        btnPhaseA.setBackground(Color.pink);
        btnPhaseB.setBackground(Color.pink);
        btnPhaseC.setBackground(Color.pink);

        btnPhaseA.setPreferredSize(new Dimension(120, 35));
        btnPhaseB.setPreferredSize(new Dimension(120, 35));
        btnPhaseC.setPreferredSize(new Dimension(120, 35));

        top.add(new Label("Select view:"));
        top.add(btnPhaseA);
        top.add(btnPhaseB);
        top.add(btnPhaseC);
        add(top, BorderLayout.NORTH);

        // --- Karte (paneli faza) ---
        phaseA = new PhaseA(info);
        phaseB = new PhaseB(info);
        phaseC = new PhaseC(info, inactivity);

        cardPanel.add(phaseA, "A");
        cardPanel.add(phaseB, "B");
        cardPanel.add(phaseC, "C");
        cardPanel.setBackground(Color.pink);
        add(cardPanel, BorderLayout.CENTER);

        // --- Klikom na dugme menjamo fazu (sa osvežavanjem) ---
        btnPhaseA.addActionListener(e -> cards.show(cardPanel, "A"));
        btnPhaseB.addActionListener(e -> {
            // povuci sveže aerodrome/vidljivost na mapu B
            phaseB.onDataChanged();
            cards.show(cardPanel, "B");
        });
        btnPhaseC.addActionListener(e -> {
            //phaseC.refreshCheckboxesAndAirports(); // <<< DODATO
        	phaseC.refreshAirports();
            cards.show(cardPanel, "C");
        });


        // --- Praćenje aktivnosti miša i tastature ---
        Toolkit.getDefaultToolkit().addAWTEventListener(ev -> {
            // po dogovoru: pauziramo reset kad PhaseB treperi/sluša duže akcije
            if (!userinterface.PhaseB.pausedInactivity) {
                inactivity.reset();
            }
        }, AWTEvent.MOUSE_EVENT_MASK
           | AWTEvent.MOUSE_MOTION_EVENT_MASK
           | AWTEvent.MOUSE_WHEEL_EVENT_MASK
           | AWTEvent.KEY_EVENT_MASK);

        // --- Pokretanje niti tajmera (daemon) ---
        inactivityThread = new Thread(inactivity, "InactivityWatch");
        inactivityThread.setDaemon(true);  // da ne blokira gašenje prozora
        inactivityThread.start();

        // --- Zatvaranje prozora (bez System.exit) ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // uredno ugasi faze koje imaju pozadinske poslove
                try { phaseB.shutdown(); } catch (Throwable ignore) {}
                try { phaseC.shutdown(); } catch (Throwable ignore) {}

                // ukloni UI i pusti JVM da se sam ugasi (nema ne-daemon niti)
                setVisible(false);
                dispose();
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        new MainForm();
    }
}
