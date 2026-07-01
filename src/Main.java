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
        // GUI immer im Event-Dispatch-Thread von Swing aufbauen
        SwingUtilities.invokeLater(Rechnerfenster::new);
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

    /** Zeichnet ein einfaches Taschenrechner-Icon (Display + Tasten). */
    static BufferedImage erzeugeIcon(int groesse) {
        BufferedImage bild = new BufferedImage(groesse, groesse, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bild.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rand = groesse / 12;
        int b = groesse - 2 * rand;

        // Gehaeuse mit abgerundeten Ecken
        g.setColor(new Color(0x2D, 0x3A, 0x4A));
        g.fillRoundRect(rand, rand, b, b, groesse / 6, groesse / 6);

        // Display
        g.setColor(new Color(0x8E, 0xE6, 0xC1));
        int dispX = rand + b / 10;
        int dispY = rand + b / 10;
        int dispB = b - 2 * (b / 10);
        int dispH = b / 4;
        g.fillRoundRect(dispX, dispY, dispB, dispH, groesse / 16, groesse / 16);

        // Tasten (3x3 Raster)
        int startY = dispY + dispH + b / 12;
        int luecke = b / 12;
        int tasten = 3;
        int tasteGr = (dispB - (tasten - 1) * luecke) / tasten;
        for (int zeile = 0; zeile < tasten; zeile++) {
            for (int spalte = 0; spalte < tasten; spalte++) {
                int x = dispX + spalte * (tasteGr + luecke);
                int y = startY + zeile * (tasteGr + luecke);
                // untere rechte Taste in Akzentfarbe (wie "=")
                g.setColor(zeile == tasten - 1 && spalte == tasten - 1
                        ? new Color(0xFF, 0x9F, 0x43)
                        : new Color(0xE9, 0xED, 0xF1));
                g.fillRoundRect(x, y, tasteGr, tasteGr, tasteGr / 3, tasteGr / 3);
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
