import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Taschenrechner mit grafischer Oberflaeche (GUI).
 * Kann plus (+), minus (-), mal (*) und geteilt (/) rechnen.
 *
 * Starten:  Doppelklick auf die fertige .jar Datei
 *   oder:   java -jar Taschenrechner.jar
 */
public class Main {

    public static void main(String[] args) {
        // Headless-Selbsttest (fuer CI): rechnet ohne GUI und beendet mit Exit-Code.
        if (args.length > 0 && args[0].equals("--selftest")) {
            selbsttest();
            return;
        }
        // GUI immer im Event-Dispatch-Thread von Swing aufbauen
        SwingUtilities.invokeLater(Rechnerfenster::new);
    }

    /** Prueft die Rechenlogik ohne GUI und beendet mit Exit-Code 0 (ok) bzw. 1 (Fehler). */
    private static void selbsttest() {
        boolean ok = true;
        ok &= pruefe("5 + 3", 8, Rechnerfenster.rechne(5, 3, "+"));
        ok &= pruefe("5 - 3", 2, Rechnerfenster.rechne(5, 3, "-"));
        ok &= pruefe("5 * 3", 15, Rechnerfenster.rechne(5, 3, "*"));
        ok &= pruefe("5 / 2", 2.5, Rechnerfenster.rechne(5, 2, "/"));
        if (Rechnerfenster.rechne(5, 0, "/") != null) {   // Teilen durch 0 -> null
            System.out.println("  FEHLER: 5 / 0 haette null ergeben muessen");
            ok = false;
        }
        System.out.println(ok ? "Selbsttest OK" : "Selbsttest FEHLGESCHLAGEN");
        System.exit(ok ? 0 : 1);
    }

    private static boolean pruefe(String was, double erwartet, Double ist) {
        boolean ok = ist != null && Math.abs(ist - erwartet) < 1e-9;
        if (!ok) {
            System.out.println("  FEHLER: " + was + " = " + ist + " (erwartet " + erwartet + ")");
        }
        return ok;
    }
}

/**
 * Das Fenster des Taschenrechners inklusive Anzeige, Buttons und Rechenlogik.
 */
class Rechnerfenster {

    private final JTextField anzeige = new JTextField("0");

    // Rechen-Zustand
    private double gespeicherterWert = 0;
    private String offenerOperator = null;   // +, -, *, /
    private boolean neueZahlBeginnt = true;   // naechste Ziffer startet eine neue Zahl

    Rechnerfenster() {
        JFrame frame = new JFrame("Taschenrechner");
        Image icon = erzeugeIcon(128);
        frame.setIconImage(icon);
        setzeDockIcon(icon);                        // macOS Dock-Icon (best effort)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Anzeige oben ---
        anzeige.setEditable(false);
        anzeige.setHorizontalAlignment(JTextField.RIGHT);
        anzeige.setFont(new Font("SansSerif", Font.BOLD, 32));
        anzeige.setPreferredSize(new Dimension(320, 60));
        frame.add(anzeige, BorderLayout.NORTH);

        // --- Buttons im Raster ---
        JPanel panel = new JPanel(new GridLayout(5, 4, 6, 6));
        String[] beschriftungen = {
                "C", "±", "%", "/",
                "7", "8", "9", "*",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "=", ""
        };
        for (String text : beschriftungen) {
            if (text.isEmpty()) {
                panel.add(new JLabel());           // leere Zelle
            } else {
                panel.add(erzeugeButton(text));
            }
        }
        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);          // Fenster zentrieren
        frame.setVisible(true);
    }

    /** Zeichnet das Taschenrechner-Icon (Gehaeuse mit Verlauf, Display, Tasten). */
    static BufferedImage erzeugeIcon(int groesse) {
        BufferedImage bild = new BufferedImage(groesse, groesse, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bild.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rand = groesse / 12;
        int b = groesse - 2 * rand;
        int ecke = groesse / 6;

        // Gehaeuse mit vertikalem Farbverlauf
        g.setPaint(new GradientPaint(0, rand, new Color(0x3B, 0x4B, 0x63),
                                     0, rand + b, new Color(0x22, 0x2D, 0x3C)));
        g.fillRoundRect(rand, rand, b, b, ecke, ecke);

        int innen = b / 10;
        int dispX = rand + innen;
        int dispY = rand + innen;
        int dispB = b - 2 * innen;
        int dispH = b / 4;

        // Display mit leichtem Verlauf
        g.setPaint(new GradientPaint(0, dispY, new Color(0xAD, 0xF0, 0xD2),
                                     0, dispY + dispH, new Color(0x79, 0xD6, 0xB2)));
        g.fillRoundRect(dispX, dispY, dispB, dispH, groesse / 20, groesse / 20);

        // zwei "Ziffern" rechtsbuendig im Display
        g.setColor(new Color(0x22, 0x2D, 0x3C));
        int zh = dispH / 2;
        int zw = Math.max(2, zh / 2);
        int zy = dispY + (dispH - zh) / 2;
        int zx2 = dispX + dispB - innen - zw;
        int zx1 = zx2 - 2 * zw;
        g.fillRoundRect(zx1, zy, zw, zh, zw / 2 + 1, zw / 2 + 1);
        g.fillRoundRect(zx2, zy, zw, zh, zw / 2 + 1, zw / 2 + 1);

        // Tasten (3x3), rechte Spalte als Akzent (Operatoren)
        int n = 3;
        int luecke = b / 14;
        int gridTop = dispY + dispH + luecke;
        int gridBottom = rand + b - innen;
        int gridW = dispB;
        int gridH = gridBottom - gridTop;
        int tasteGr = (Math.min(gridW, gridH) - (n - 1) * luecke) / n;   // passt in Breite UND Hoehe
        int belegt = n * tasteGr + (n - 1) * luecke;
        int offX = dispX + (gridW - belegt) / 2;
        int offY = gridTop + (gridH - belegt) / 2;
        int radius = tasteGr / 3;
        for (int zeile = 0; zeile < n; zeile++) {
            for (int spalte = 0; spalte < n; spalte++) {
                int x = offX + spalte * (tasteGr + luecke);
                int y = offY + zeile * (tasteGr + luecke);
                g.setColor(spalte == n - 1
                        ? new Color(0xFF, 0x9F, 0x43)     // Operatoren-Spalte
                        : new Color(0xEC, 0xF0, 0xF4));    // Zifferntasten
                g.fillRoundRect(x, y, tasteGr, tasteGr, radius, radius);
            }
        }

        g.dispose();
        return bild;
    }

    /** Setzt das Dock-Icon unter macOS, falls unterstuetzt. */
    private static void setzeDockIcon(Image icon) {
        try {
            Taskbar.getTaskbar().setIconImage(icon);
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Auf dieser Plattform nicht verfuegbar - kein Problem
        }
    }

    private JButton erzeugeButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 22));
        button.setFocusPainted(false);
        button.addActionListener((ActionEvent e) -> knopfGedrueckt(text));
        return button;
    }

    /** Reagiert auf jeden Knopfdruck. */
    private void knopfGedrueckt(String text) {
        switch (text) {
            case "C" -> zuruecksetzen();
            case "±" -> vorzeichenWechseln();
            case "%" -> prozent();
            case "+", "-", "*", "/" -> operatorGewaehlt(text);
            case "=" -> gleich();
            case "." -> kommaEingeben();
            default -> zifferEingeben(text);       // 0-9
        }
    }

    private void zifferEingeben(String ziffer) {
        if (neueZahlBeginnt || anzeige.getText().equals("0")) {
            anzeige.setText(ziffer);
            neueZahlBeginnt = false;
        } else {
            anzeige.setText(anzeige.getText() + ziffer);
        }
    }

    private void kommaEingeben() {
        if (neueZahlBeginnt) {
            anzeige.setText("0.");
            neueZahlBeginnt = false;
        } else if (!anzeige.getText().contains(".")) {
            anzeige.setText(anzeige.getText() + ".");
        }
    }

    private void operatorGewaehlt(String operator) {
        // Falls schon ein Operator offen ist, zuerst das Zwischenergebnis berechnen
        if (offenerOperator != null && !neueZahlBeginnt) {
            gleich();
        } else {
            gespeicherterWert = leseAnzeige();
        }
        offenerOperator = operator;
        neueZahlBeginnt = true;
    }

    private void gleich() {
        if (offenerOperator == null) {
            return;
        }
        double aktuellerWert = leseAnzeige();
        Double ergebnis = rechne(gespeicherterWert, aktuellerWert, offenerOperator);

        if (ergebnis == null) {
            anzeige.setText("Fehler");
        } else {
            zeigeErgebnis(ergebnis);
            gespeicherterWert = ergebnis;
        }
        offenerOperator = null;
        neueZahlBeginnt = true;
    }

    private void prozent() {
        zeigeErgebnis(leseAnzeige() / 100.0);
        neueZahlBeginnt = true;
    }

    private void vorzeichenWechseln() {
        zeigeErgebnis(leseAnzeige() * -1);
    }

    private void zuruecksetzen() {
        anzeige.setText("0");
        gespeicherterWert = 0;
        offenerOperator = null;
        neueZahlBeginnt = true;
    }

    /** Die eigentliche Rechnung. Gibt null bei Teilen durch Null zurueck. */
    static Double rechne(double a, double b, String operator) {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b == 0 ? null : a / b;
            default -> null;
        };
    }

    private double leseAnzeige() {
        try {
            return Double.parseDouble(anzeige.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Zeigt das Ergebnis an, ganze Zahlen ohne ".0". */
    private void zeigeErgebnis(double wert) {
        if (wert == Math.floor(wert) && !Double.isInfinite(wert)) {
            anzeige.setText(String.valueOf((long) wert));
        } else {
            anzeige.setText(String.valueOf(wert));
        }
    }
}
