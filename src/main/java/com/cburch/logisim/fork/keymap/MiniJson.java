/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for {@code keymap.json}, so the fork needs no extra dependency.
 *
 * <p>Produces {@code Map<String, Object>} for objects (keeping key order), {@code List<Object>} for
 * arrays, {@code String}, {@code Double}, {@code Boolean} and {@code null}. Errors throw
 * {@link IllegalArgumentException} with the line and column.
 */
final class MiniJson {
  private final String text;
  private int pos;

  private MiniJson(String text) {
    this.text = text;
  }

  static Object parse(String text) {
    final var parser = new MiniJson(text);
    parser.skipWhitespace();
    final var value = parser.readValue();
    parser.skipWhitespace();
    if (parser.pos != text.length()) throw parser.error("unexpected text after the JSON value");
    return value;
  }

  private Object readValue() {
    if (pos >= text.length()) throw error("unexpected end of input");
    final var c = text.charAt(pos);
    return switch (c) {
      case '{' -> readObject();
      case '[' -> readArray();
      case '"' -> readString();
      case 't' -> readLiteral("true", Boolean.TRUE);
      case 'f' -> readLiteral("false", Boolean.FALSE);
      case 'n' -> readLiteral("null", null);
      default -> {
        if (c == '-' || (c >= '0' && c <= '9')) yield readNumber();
        throw error("unexpected character '" + c + "'");
      }
    };
  }

  private Map<String, Object> readObject() {
    final var map = new LinkedHashMap<String, Object>();
    pos++; // {
    skipWhitespace();
    if (peek('}')) {
      pos++;
      return map;
    }
    while (true) {
      skipWhitespace();
      if (!peek('"')) throw error("expected a quoted key");
      final var key = readString();
      skipWhitespace();
      expect(':');
      skipWhitespace();
      map.put(key, readValue());
      skipWhitespace();
      if (peek(',')) {
        pos++;
        continue;
      }
      expect('}');
      return map;
    }
  }

  private List<Object> readArray() {
    final var list = new ArrayList<Object>();
    pos++; // [
    skipWhitespace();
    if (peek(']')) {
      pos++;
      return list;
    }
    while (true) {
      skipWhitespace();
      list.add(readValue());
      skipWhitespace();
      if (peek(',')) {
        pos++;
        continue;
      }
      expect(']');
      return list;
    }
  }

  private String readString() {
    pos++; // opening quote
    final var out = new StringBuilder();
    while (pos < text.length()) {
      final var c = text.charAt(pos++);
      if (c == '"') return out.toString();
      if (c != '\\') {
        out.append(c);
        continue;
      }
      if (pos >= text.length()) break;
      final var esc = text.charAt(pos++);
      switch (esc) {
        case '"', '\\', '/' -> out.append(esc);
        case 'b' -> out.append('\b');
        case 'f' -> out.append('\f');
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 't' -> out.append('\t');
        case 'u' -> {
          if (pos + 4 > text.length()) throw error("bad \\u escape");
          try {
            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
          } catch (NumberFormatException e) {
            throw error("bad \\u escape");
          }
          pos += 4;
        }
        default -> throw error("bad escape \\" + esc);
      }
    }
    throw error("unterminated string");
  }

  private Double readNumber() {
    final var start = pos;
    while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
    try {
      return Double.valueOf(text.substring(start, pos));
    } catch (NumberFormatException e) {
      pos = start;
      throw error("bad number");
    }
  }

  private Object readLiteral(String word, Object value) {
    if (!text.startsWith(word, pos)) throw error("unexpected token");
    pos += word.length();
    return value;
  }

  private void skipWhitespace() {
    while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
  }

  private boolean peek(char c) {
    return pos < text.length() && text.charAt(pos) == c;
  }

  private void expect(char c) {
    if (!peek(c)) throw error("expected '" + c + "'");
    pos++;
  }

  private IllegalArgumentException error(String message) {
    var line = 1;
    var column = 1;
    for (var i = 0; i < pos && i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }
    return new IllegalArgumentException(message + " at line " + line + ", column " + column);
  }
}
