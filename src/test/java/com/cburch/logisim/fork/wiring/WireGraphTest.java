/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.wiring.WireGraph.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class WireGraphTest {
  private static Location at(int x, int y) {
    return Location.create(x, y, false);
  }

  private static Segment<String> seg(String name, int x0, int y0, int x1, int y1) {
    return new Segment<>(name, at(x0, y0), at(x1, y1));
  }

  /**
   * Pin A at (0,0) wired to pin B at (40,20) through a bend at (20,0)-(20,20); a T-junction at
   * (20,10) feeding pin C at (40,10) is NOT present; a stub with a bend hangs off (20,20).
   */
  private static WireGraph<String> sample() {
    final var segs = List.of(
        seg("a1", 0, 0, 20, 0),
        seg("a2", 20, 0, 20, 20),
        seg("a3", 20, 20, 40, 20),
        seg("stub1", 20, 20, 20, 40),
        seg("stub2", 20, 40, 30, 40),
        seg("lonely", 100, 100, 120, 100));
    return new WireGraph<>(segs, List.of(at(0, 0), at(40, 20), at(200, 200)));
  }

  @Test
  public void danglingAndUnconnected() {
    final var g = sample();
    assertTrue(g.isDanglingEnd(at(30, 40)));
    assertFalse(g.isDanglingEnd(at(20, 20))); // junction of three wires
    assertFalse(g.isDanglingEnd(at(0, 0))); // wire meets a pin
    assertTrue(g.isUnconnectedPin(at(200, 200)));
    assertFalse(g.isUnconnectedPin(at(40, 20)));
  }

  @Test
  public void chainStopsAtJunctionsAndPins() {
    final var g = sample();
    // a1-a2 run from pin A through a bend to the junction at (20,20).
    assertEquals(Set.of("a1", "a2"), Set.copyOf(g.chainThrough("a1")));
    assertEquals(Set.of("a1", "a2"), Set.copyOf(g.chainThrough("a2")));
    assertEquals(Set.of("a3"), Set.copyOf(g.chainThrough("a3")));
    assertEquals(Set.of("stub1", "stub2"), Set.copyOf(g.chainThrough("stub2")));
  }

  @Test
  public void danglingStubsGoCompletelyAndConnectionsStay() {
    final var g = sample();
    assertEquals(Set.of("stub1", "stub2", "lonely"), Set.copyOf(g.danglingStubs()));
    // Removing them leaves the A-B connection intact.
    final var after = g.without(g.danglingStubs());
    assertEquals(g.pinNets(), after.pinNets());
    assertTrue(after.danglingStubs().isEmpty());
  }

  @Test
  public void pinNetsReflectConnectivity() {
    final var g = sample();
    final var nets = g.pinNets();
    assertEquals(Set.of(at(0, 0), at(40, 20)), nets.get(at(0, 0)));
    assertEquals(Set.of(at(200, 200)), nets.get(at(200, 200)));
    // Taking out a1 disconnects A from B.
    assertFalse(g.pinNets().equals(g.without(List.of("a1")).pinNets()));
  }

  @Test
  public void closedLoopDoesNotHang() {
    final var loop = new ArrayList<Segment<String>>();
    loop.add(seg("t", 0, 0, 10, 0));
    loop.add(seg("r", 10, 0, 10, 10));
    loop.add(seg("b", 10, 10, 0, 10));
    loop.add(seg("l", 0, 10, 0, 0));
    final var g = new WireGraph<>(loop, List.<Location>of());
    assertEquals(4, g.chainThrough("t").size());
    assertTrue(g.danglingStubs().isEmpty());
  }
}
