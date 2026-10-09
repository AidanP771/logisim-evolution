/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.std.gates.GateAttributes;
import com.cburch.logisim.std.memory.Register;
import com.cburch.logisim.std.plexers.Multiplexer;
import com.cburch.logisim.std.plexers.PlexersLibrary;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.tools.AddTool;
import org.junit.jupiter.api.Test;

public class ParameterRuleTest {
  private static AttributeSet andGate() {
    return ((AddTool) new GatesLibrary().getTool("AND Gate")).getFactory().createAttributeSet();
  }

  @Test
  public void gatesTakeTheNumberOfInputs() {
    final var attrs = andGate();
    assertNotNull(ParameterRule.apply(attrs, 3));
    assertEquals(3, attrs.getValue(GateAttributes.ATTR_INPUTS));
    assertEquals(BitWidth.ONE, attrs.getValue(StdAttr.WIDTH)); // width untouched
  }

  @Test
  public void multiplexersDeriveSelectBits() {
    final var attrs = new Multiplexer().createAttributeSet();
    ParameterRule.apply(attrs, 4);
    assertEquals(2, attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth());
    ParameterRule.apply(attrs, 5);
    assertEquals(3, attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth());
  }

  @Test
  public void registersTakeTheWidth() {
    final var attrs = new Register().createAttributeSet();
    ParameterRule.apply(attrs, 8);
    assertEquals(8, attrs.getValue(StdAttr.WIDTH).getWidth());
  }

  @Test
  public void numbersAComponentCannotTakeAreIgnored() {
    final var gate = andGate();
    final var before = gate.getValue(GateAttributes.ATTR_INPUTS);
    // 1 input is below the gate's minimum (2), and widths stop at 64.
    assertNull(ParameterRule.apply(gate, 1));
    assertEquals(before, gate.getValue(GateAttributes.ATTR_INPUTS));
    assertNull(ParameterRule.apply(new Clock().createAttributeSet(), 4));
  }

  @Test
  public void selectBits() {
    assertEquals(1, ParameterRule.selectBitsFor(1));
    assertEquals(1, ParameterRule.selectBitsFor(2));
    assertEquals(2, ParameterRule.selectBitsFor(3));
    assertEquals(3, ParameterRule.selectBitsFor(8));
    assertEquals(4, ParameterRule.selectBitsFor(9));
  }
}
