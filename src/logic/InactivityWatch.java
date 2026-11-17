package logic;

import java.awt.*;

public class InactivityWatch implements Runnable {

    private volatile long lastActionMillis = System.currentTimeMillis();
    private volatile boolean running = true;
    private volatile boolean countdownLocked = false; // prikaži countdown samo jednom po neaktivnosti

    private volatile boolean externallyPaused = false;
    public void setExternallyPaused(boolean paused) {
        externallyPaused = paused;
        lastActionMillis = System.currentTimeMillis();
    }

    public void reset() {
        if (!countdownLocked && !externallyPaused)
            lastActionMillis = System.currentTimeMillis();
    }

    public void stop() { running = false; }

    @Override public void run() {
        while (running) {
            if (externallyPaused) {                // ← dok je pauziran (npr. simulacija), ništa se ne dešava
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                continue;
            }

            long idleSec = (System.currentTimeMillis() - lastActionMillis) / 1000L;

            if (idleSec >= 55 && !countdownLocked) {
                countdownLocked = true;
                boolean continued = showCountdownDialogAWT(); // tvoj postojeći dijalog
                if (continued) {
                    lastActionMillis = System.currentTimeMillis();
                    countdownLocked = false;
                } else System.exit(0);
            }

            if (idleSec >= 60)
                System.exit(0);

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
    }

    private boolean showCountdownDialogAWT() {
        final boolean[] continued = { false };

        // Modalni dijalog bez Swing-a
        final Dialog dlg = new Dialog((Frame) null, "Upozorenje", true);
        dlg.setLayout(new BorderLayout(10,10));

        final Label lbl = new Label("", Label.CENTER);
        Panel south = new Panel(new FlowLayout(FlowLayout.CENTER));
        final Button btn = new Button("Continue");
        south.add(btn);
        btn.setBackground(new Color(231,84,128));

        dlg.add(lbl, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setSize(380, 150);
        dlg.setBackground(Color.pink);

        // centriranje na ekran (AWT varijanta)
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setLocation((scr.width - dlg.getWidth()) / 2, (scr.height - dlg.getHeight()) / 2);

        // klik na Continue → zatvaramo i signaliziramo nastavak
        btn.addActionListener(e -> { continued[0] = true; dlg.dispose(); });

        // odbrojavanje 5..1 (UI update kroz AWT EventQueue)
        Thread t = new Thread(() -> {
            for (int sec = 5; sec >= 1; sec--) {
                final int s = sec;
                EventQueue.invokeLater(() ->
                    lbl.setText("Program će se zatvoriti za " + s + " sek.")
                );
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            if (dlg.isShowing()) dlg.dispose(); // bez klika → zatvori; run() će završiti app
        }, "InactivityCountdown");
        t.setDaemon(true);
        t.start();

        dlg.setVisible(true); // modalno blokira dok se ne zatvori
        return continued[0];
    }
}