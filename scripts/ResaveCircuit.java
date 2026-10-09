/*
 * Headless "open and save again" for scripts/regression.sh.
 *
 * Usage: java -Djava.awt.headless=true -cp <logisim jar> scripts/ResaveCircuit.java <in.circ> <out.circ>
 *
 * The built-in `-n` option does the same job but needs a GUI, blocks on modal dialogs (for example
 * the "old file format" warning) and adds the file to the user's recent-files list. This helper uses
 * the same Loader / LogisimFile.write path without any of that, and works with both the stock jar and
 * the fork jar. It is test tooling only and is not part of the application.
 */

import com.cburch.logisim.Main;
import com.cburch.logisim.file.Loader;
import java.io.File;
import java.io.FileOutputStream;

public class ResaveCircuit {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("usage: ResaveCircuit <in.circ> <out.circ>");
      System.exit(2);
    }
    // Makes OptionPane log messages instead of opening dialogs.
    Main.headless = true;
    final var loader = new Loader(null);
    final var file = loader.openLogisimFile(new File(args[0]));
    final var dest = new File(args[1]);
    try (var out = new FileOutputStream(dest)) {
      file.write(out, loader, dest);
    }
    // Simulator/UI helper threads may still be alive; do not wait for them.
    System.exit(0);
  }
}
