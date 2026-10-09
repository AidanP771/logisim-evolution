/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.util.MacCompatibility;
import java.lang.reflect.Proxy;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trackpad pinch-to-zoom on macOS.
 *
 * <p>The JDK only reports magnify gestures through the internal {@code com.apple.eawt.event} API.
 * It is reached by reflection so the fork still compiles and runs everywhere, and it needs
 * {@code --add-exports java.desktop/com.apple.eawt.event=ALL-UNNAMED} (set in the jar manifest, the
 * macOS app package and {@code gradle run}). If the API is missing or not exported, pinch is
 * silently unavailable and everything else keeps working.
 */
final class PinchZoom {
  private static final Logger logger = LoggerFactory.getLogger(PinchZoom.class);
  private static boolean warned;

  private PinchZoom() {}

  /** Starts listening for pinch gestures on {@code canvas}; returns whether that worked. */
  static boolean install(Canvas canvas) {
    if (!MacCompatibility.isRunningOnMac()) return false;
    try {
      final var utilities = Class.forName("com.apple.eawt.event.GestureUtilities");
      final var listenerType = Class.forName("com.apple.eawt.event.MagnificationListener");
      final var gestureType = Class.forName("com.apple.eawt.event.GestureListener");
      final var getMagnification =
          Class.forName("com.apple.eawt.event.MagnificationEvent").getMethod("getMagnification");
      final var listener =
          Proxy.newProxyInstance(
              PinchZoom.class.getClassLoader(),
              new Class<?>[] {listenerType},
              (proxy, method, args) -> {
                switch (method.getName()) {
                  case "magnify" -> {
                    final var amount = (Double) getMagnification.invoke(args[0]);
                    onMagnify(canvas, amount);
                    return null;
                  }
                  case "hashCode" -> {
                    return System.identityHashCode(proxy);
                  }
                  case "equals" -> {
                    return proxy == args[0];
                  }
                  case "toString" -> {
                    return "PinchZoom listener";
                  }
                  default -> {
                    return null;
                  }
                }
              });
      utilities
          .getMethod("addGestureListenerTo", JComponent.class, gestureType)
          .invoke(null, canvas, listener);
      return true;
    } catch (ReflectiveOperationException | RuntimeException e) {
      if (!warned) {
        warned = true;
        logger.info("Trackpad pinch zoom unavailable: {}", e.toString());
      }
      return false;
    }
  }

  private static void onMagnify(Canvas canvas, double amount) {
    if (!ForkPreferences.PINCH_ZOOM.isEnabled()) return;
    final Runnable zoom = () -> ViewActions.zoomBy(canvas, 1.0 + amount, canvas.getMousePosition());
    if (SwingUtilities.isEventDispatchThread()) {
      zoom.run();
    } else {
      SwingUtilities.invokeLater(zoom);
    }
  }
}
