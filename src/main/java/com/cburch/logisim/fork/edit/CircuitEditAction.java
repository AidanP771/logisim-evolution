/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.edit;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;

/**
 * An undoable circuit edit built the same way as Logisim's own selection actions: the mutation is
 * worked out when the action first runs (so it sees the circuit as left by earlier actions in a
 * {@code JoinedAction}), replayed on redo and reversed on undo.
 */
public abstract class CircuitEditAction extends Action {
  private final StringGetter name;
  private CircuitTransaction forward;
  private CircuitTransaction reverse;
  private boolean done;

  protected CircuitEditAction(StringGetter name) {
    this.name = name;
  }

  /** Fills {@code xn} with the edit; called once, the first time the action runs. */
  protected abstract void build(Circuit circuit, CircuitMutation xn);

  /** The circuit to edit. */
  protected abstract Circuit circuit(Project proj);

  /** Called after every forward execution (first run and redo), e.g. to update the selection. */
  protected void afterForward(Project proj) {
    // Nothing by default.
  }

  @Override
  public void doIt(Project proj) {
    if (!done) {
      final var circuit = circuit(proj);
      final var xn = new CircuitMutation(circuit);
      build(circuit, xn);
      forward = xn;
      reverse = xn.execute().getReverseTransaction();
      done = true;
    } else if (forward != null) {
      forward.execute();
    }
    afterForward(proj);
  }

  @Override
  public void undo(Project proj) {
    if (reverse != null) reverse.execute();
  }

  @Override
  public String getName() {
    return name.toString();
  }
}
