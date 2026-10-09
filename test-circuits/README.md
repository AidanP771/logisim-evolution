# Regression test circuits

`scripts/regression.sh` runs every `.circ` file in this directory tree through the stock
logisim-evolution release and through this fork, and fails if the simulation output or the saved
XML differs. See the comment at the top of the script for details.

- `coursework/`: real coursework circuits. Drop `.circ` files here.
- `examples/`: small hand-written circuits that cover specific features:
  - `combinational.circ`: gates and wire junctions.
  - `multibit-adder.circ`: 4-bit buses through an Adder.
  - `subcircuit-custom-appearance.circ`: a full adder built from two copies of a half-adder
    subcircuit that has a custom appearance.
  - `clocked-counter.circ`: a clocked 4-bit counter. Its output pin labelled `halt` stops the TTY
    run, which prints one row per clock tick.
  - `legacy-2.7-format.circ`: written in original Logisim 2.7.1 format and loaded through the legacy
    compatibility path. It was written by hand, not saved by Logisim 2.7.

For `--tty table` to produce output, a circuit's main circuit needs input and output pins. A
sequential circuit also needs an output pin labelled `halt` that goes high to end the run.
