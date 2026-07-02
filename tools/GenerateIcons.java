import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Build-Hilfsprogramm (nicht Teil der App): erzeugt aus dem Icon-Zeichencode von
 * Rechnerfenster.erzeugeIcon() die Dateien icon/calculator.png und icon/calculator.ico.
 *
 * Aufruf (lokal): siehe tools/generate-icons.sh
 */
public class GenerateIcons {

    public static void main(String[] args) throws Exception {
        File dir = new File("icon");
        dir.mkdirs();

        // Master-PNG (fuer MSIX-Kacheln als Vorlage)
        BufferedImage master = Rechnerfenster.erzeugeIcon(512);
        ImageIO.write(master, "png", new File(dir, "calculator.png"));

        // .ico mit mehreren Groessen (Windows/jpackage)
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<byte[]> pngs = new ArrayList<>();
        for (int s : sizes) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(Rechnerfenster.erzeugeIcon(s), "png", bos);
            pngs.add(bos.toByteArray());
        }
        schreibeIco(new File(dir, "calculator.ico"), sizes, pngs);

        System.out.println("Erzeugt: icon/calculator.png und icon/calculator.ico");
    }

    /** Schreibt ein ICO, das die Bilder als PNG einbettet (von Windows Vista+ unterstuetzt). */
    private static void schreibeIco(File out, int[] sizes, List<byte[]> pngs) throws Exception {
        int n = sizes.length;
        try (OutputStream fos = new FileOutputStream(out);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // ICONDIR
            writeShortLE(dos, 0);      // reserved
            writeShortLE(dos, 1);      // type = icon
            writeShortLE(dos, n);      // Anzahl Bilder

            // ICONDIRENTRYs
            int offset = 6 + n * 16;
            for (int i = 0; i < n; i++) {
                int s = sizes[i];
                byte[] data = pngs.get(i);
                dos.writeByte(s >= 256 ? 0 : s);   // Breite (0 = 256)
                dos.writeByte(s >= 256 ? 0 : s);   // Hoehe  (0 = 256)
                dos.writeByte(0);                  // Farbanzahl
                dos.writeByte(0);                  // reserved
                writeShortLE(dos, 1);              // Farbebenen
                writeShortLE(dos, 32);             // Bits pro Pixel
                writeIntLE(dos, data.length);      // Groesse der Bilddaten
                writeIntLE(dos, offset);           // Offset der Bilddaten
                offset += data.length;
            }

            // Bilddaten (PNG)
            for (byte[] data : pngs) {
                dos.write(data);
            }
        }
    }

    private static void writeShortLE(DataOutputStream dos, int v) throws Exception {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }

    private static void writeIntLE(DataOutputStream dos, int v) throws Exception {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
        dos.writeByte((v >> 16) & 0xFF);
        dos.writeByte((v >> 24) & 0xFF);
    }
}
