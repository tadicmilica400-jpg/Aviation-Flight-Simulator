package userinterface;

import java.awt.*;
import logic.AvioInfo;
import logic.ValidationException;
import model.Airport;
import model.Flight;

public class PhaseA extends Panel {

    private static final long serialVersionUID = 1L;
    private final AvioInfo controller;

    private final TextArea airportsArea = new TextArea("", 12, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final TextArea flightsArea  = new TextArea("", 12, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final TextField tfFilename = new TextField(24);

    public PhaseA(AvioInfo controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8,8));

        // GORE: forme
        Panel inputPanel = new Panel(new GridLayout(1, 2, 10, 10));
        inputPanel.add(createAirportForm());
        inputPanel.add(createFlightForm());
        add(inputPanel, BorderLayout.NORTH);
        inputPanel.setBackground(Color.pink);

        // CENTAR: "tabele" kao TextArea
        airportsArea.setEditable(false);
        flightsArea.setEditable(false);

        Panel tables = new Panel(new GridLayout(1,2,10,10));
        tables.add(wrapWithTitled("Airports", airportsArea));
        tables.add(wrapWithTitled("Flights", flightsArea));
        add(tables, BorderLayout.CENTER);

        // DOLE: fajl + Save/Load
        Panel bottom = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bottom.add(new Label("File name:"));
        bottom.add(tfFilename);
        Button btnSave = new Button("Save");
        btnSave.setBackground(new Color(231,84,128));
        Button btnLoad = new Button("Load");
        btnLoad.setBackground(new Color(231,84,128));
        bottom.add(btnSave);
        bottom.add(btnLoad);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> saveCsv());
        btnLoad.addActionListener(e -> loadCsv());

        refreshAreas();
    }

    // ---------- Airport forma ----------
    private Panel createAirportForm() {
        Panel p = new Panel(new GridLayout(5,2,5,5));

        p.add(new Label("Airport name:"));
        TextField tfName = new TextField(); p.add(tfName);

        p.add(new Label("Code (3 letters):"));
        TextField tfCode = new TextField(); p.add(tfCode);

        p.add(new Label("X coordinate:"));
        TextField tfX = new TextField(); p.add(tfX);

        p.add(new Label("Y coordinate:"));
        TextField tfY = new TextField(); p.add(tfY);

        Button btnAdd = new Button("Add Airport");
        p.add(btnAdd);
        btnAdd.setBackground(new Color(231,84,128));

        btnAdd.addActionListener(e -> {
            try {
                String name = tfName.getText().trim();
                String code = tfCode.getText().trim();
                double x = Double.parseDouble(tfX.getText().trim());
                double y = Double.parseDouble(tfY.getText().trim());

                controller.addAirport(name, code, x, y);
                refreshAreas();
                clear(tfName, tfCode, tfX, tfY);
            } catch (ValidationException ex) {
                showMessage("Greška", ex.getMessage(), true);
            } catch (Exception ex) {
                showMessage("Greška", "Pogrešan format unosa.", true);
            }
        });

        return p;
    }

    // Flight forma
    private Panel createFlightForm() {
        Panel p = new Panel(new GridLayout(5,2,5,5));

        p.add(new Label("From (code):"));
        TextField tfFrom = new TextField(); p.add(tfFrom);

        p.add(new Label("To (code):"));
        TextField tfTo = new TextField(); p.add(tfTo);

        p.add(new Label("Departure (HH:MM):"));
        TextField tfTime = new TextField(); p.add(tfTime);

        p.add(new Label("Duration (min):"));
        TextField tfDur = new TextField(); p.add(tfDur);

        Button btnAdd = new Button("Add Flight");
        p.add(btnAdd);
        btnAdd.setBackground(new Color(231,84,128));

        btnAdd.addActionListener(e -> {
            try {
                String from = tfFrom.getText().trim();
                String to   = tfTo.getText().trim();
                String time = tfTime.getText().trim();
                int dur     = Integer.parseInt(tfDur.getText().trim());

                controller.addFlight(from, to, time, dur);
                refreshAreas();
                clear(tfFrom, tfTo, tfTime, tfDur);
            } catch (ValidationException ex) {
                showMessage("Greška", ex.getMessage(), true);
            } catch (Exception ex) {
                showMessage("Greška", "Pogrešan format unosa.", true);
            }
        });

        return p;
    }

    //save
    private void saveCsv() {
        String fileName = tfFilename.getText().trim();
        if (fileName.isEmpty()) {
            showMessage("Greška", "Unesite ime fajla za čuvanje.", true);
            return;
        }
        try {
            controller.saveToFile(fileName);
            showMessage("Sačuvano", "Podaci su sačuvani u fajl:\n" + fileName, false);
        } catch (Exception ex) {
            showMessage("Greška pri čuvanju", ex.getMessage(), true);
        }
    }

    // load
    private void loadCsv() {
        String fileName = tfFilename.getText().trim();
        if (fileName.isEmpty()) {
            showMessage("Greška", "Unesite ime fajla za učitavanje.", true);
            return;
        }
        try {
            controller.loadFromFile(fileName);
            refreshAreas();
            showMessage("Učitano", "Podaci su učitani iz fajla:\n" + fileName, false);
        } catch (Exception ex) {
            showMessage("Greška pri učitavanju", ex.getMessage(), true);
        }
    }

    // refresh
    private void refreshAreas() {
        StringBuilder ab = new StringBuilder();
        ab.append(String.format("%-20s %-8s %8s %8s%n", "Name", "Code", "X", "Y"));
        for (Airport a : controller.store().getAirports())
            ab.append(String.format("%-20s %-8s %8.2f %.2f%n", a.getName(), a.getCode(), a.getX(), a.getY()));
        airportsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        airportsArea.setText(ab.toString());

        StringBuilder fb = new StringBuilder();
        fb.append(String.format("%-10s %-10s %-10s %10s%n", "From", "To", "Dep", "Dur"));
        for (Flight f : controller.store().getFlights())
            fb.append(String.format("%-10s %-10s %-10s %10d%n", 
                    f.getFrom().getCode(), f.getTo().getCode(),
                    f.getDepartureHHmm(), f.getDurationMin()));
        flightsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        flightsArea.setText(fb.toString());
    }
    private static Panel wrapWithTitled(String title, Component c) {
        Panel outer = new Panel(new BorderLayout());
        outer.add(new Label(title), BorderLayout.NORTH);
        outer.add(c, BorderLayout.CENTER);
        return outer;
    }

    private static void clear(TextField... fields) {
        for (TextField tf : fields) tf.setText("");
    }

    private void showMessage(String title, String message, boolean error) {
        final Dialog d = new Dialog((Frame) null, title, true);
        d.setLayout(new BorderLayout(10,10));
        d.setBackground(new Color(231,84,128));

        TextArea ta = new TextArea(message, 5, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        ta.setEditable(false);
        d.add(ta, BorderLayout.CENTER);
        ta.setBackground(Color.pink);

        Panel south = new Panel(new FlowLayout(FlowLayout.RIGHT));
        Button ok = new Button("OK");
        south.add(ok);
        ok.setBackground(new Color(231,84,128));
        d.add(south, BorderLayout.SOUTH);

        if (error) Toolkit.getDefaultToolkit().beep();

        ok.addActionListener(e -> d.dispose());
        d.pack();
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }
}