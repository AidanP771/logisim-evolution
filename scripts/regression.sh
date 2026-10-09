#!/usr/bin/env bash
#
# Regression safety net for the UX fork (see UX_PLAN.md, phase 0.4).
#
# For every .circ file under test-circuits/ this script:
#   1. runs the STOCK release jar with `--tty table` and keeps the output as the expected result;
#   2. runs the FORK jar the same way and diffs against the expected result;
#   3. re-saves the file with the FORK (open + save, see scripts/ResaveCircuit.java), runs the STOCK
#      jar on the re-saved file and diffs against the expected result again;
#   4. re-saves the file with the STOCK jar too and diffs the two saved XML files (ignoring the
#      version string in the header), which catches any change to what the fork writes to disk.
#
# Usage: scripts/regression.sh [--no-build] [name-filter]
#   --no-build   use the existing fork jar in build/libs instead of running `./gradlew shadowJar`
#   name-filter  only test files whose path contains this string
#
# Outputs go to build/regression/. Exit status is 0 only if every check passes.

set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STOCK_VERSION="${STOCK_VERSION:-5.0.0}"
STOCK_JAR="$ROOT/build/stock/logisim-evolution-$STOCK_VERSION-all.jar"
STOCK_URL="https://github.com/logisim-evolution/logisim-evolution/releases/download/v$STOCK_VERSION/logisim-evolution-$STOCK_VERSION-all.jar"
OUT="$ROOT/build/regression"
JAVA_TIMEOUT="${JAVA_TIMEOUT:-120}"

build=1
filter=""
for arg in "$@"; do
  case "$arg" in
    --no-build) build=0 ;;
    -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
    *) filter="$arg" ;;
  esac
done

# Run a command with a timeout when a timeout tool is available.
run_limited() {
  if command -v timeout >/dev/null 2>&1; then
    timeout "$JAVA_TIMEOUT" "$@"
  elif command -v gtimeout >/dev/null 2>&1; then
    gtimeout "$JAVA_TIMEOUT" "$@"
  else
    "$@"
  fi
}

# Print the truth table / simulation trace of a circuit file (stdout only).
tty_table() { # jar file out
  run_limited java -Djava.awt.headless=true -jar "$1" --tty table "$2" >"$3" 2>"$3.log"
}

# Open a circuit file and write it out again, headless (the built-in `-n` option needs a GUI,
# blocks on modal dialogs and touches the recent-files list).
resave() { # jar in out
  run_limited java -Djava.awt.headless=true -cp "$1" "$ROOT/scripts/ResaveCircuit.java" "$2" "$3" \
    >"$3.log" 2>&1 && [ -s "$3" ]
}

# Drop the version stamps that legitimately differ between releases.
normalize_xml() {
  sed -E -e 's/source="[^"]*"/source="VERSION"/' \
         -e 's/Logisim-evolution v[^(]*\(/Logisim-evolution vVERSION(/' "$1"
}

# --- jars --------------------------------------------------------------------------------------

if [ ! -f "$STOCK_JAR" ]; then
  echo "Downloading stock jar v$STOCK_VERSION ..."
  mkdir -p "$(dirname "$STOCK_JAR")"
  curl -fL --progress-bar -o "$STOCK_JAR.part" "$STOCK_URL" && mv "$STOCK_JAR.part" "$STOCK_JAR" || {
    echo "ERROR: could not download $STOCK_URL" >&2
    exit 2
  }
fi

if [ "$build" -eq 1 ]; then
  echo "Building fork jar ..."
  (cd "$ROOT" && ./gradlew -q shadowJar) || { echo "ERROR: build failed" >&2; exit 2; }
fi
FORK_JAR="$(ls -t "$ROOT"/build/libs/logisim-evolution-*-all.jar 2>/dev/null | head -n 1)"
if [ -z "$FORK_JAR" ]; then
  echo "ERROR: no fork jar in build/libs (run without --no-build)" >&2
  exit 2
fi

echo "Stock: $STOCK_JAR"
echo "Fork:  $FORK_JAR"
echo

# --- tests -------------------------------------------------------------------------------------

rm -rf "$OUT"
mkdir -p "$OUT"
pass=0
fail=0
files=0

while IFS= read -r circ; do
  rel="${circ#"$ROOT"/}"
  if [ -n "$filter" ] && [[ "$rel" != *"$filter"* ]]; then
    continue
  fi
  files=$((files + 1))
  dir="$OUT/${rel%.circ}"
  mkdir -p "$dir"
  results=""

  check() { # name status
    results="$results $1:$2"
    if [ "$2" = "ok" ]; then pass=$((pass + 1)); else fail=$((fail + 1)); fi
  }

  # 1. expected output from the stock jar
  if tty_table "$STOCK_JAR" "$circ" "$dir/expected.txt" && [ -s "$dir/expected.txt" ]; then
    have_expected=1
  else
    have_expected=0
    check "stock-tty" "FAIL(see $dir/expected.txt.log)"
  fi

  # 2. fork simulation matches stock
  if [ "$have_expected" -eq 1 ]; then
    tty_table "$FORK_JAR" "$circ" "$dir/fork.txt"
    if diff -u "$dir/expected.txt" "$dir/fork.txt" >"$dir/fork.diff"; then
      check "fork-sim" "ok"
    else
      check "fork-sim" "FAIL"
    fi
  fi

  # 3. fork re-save, simulated by stock, matches stock
  if resave "$FORK_JAR" "$circ" "$dir/fork-resaved.circ"; then
    if [ "$have_expected" -eq 1 ]; then
      tty_table "$STOCK_JAR" "$dir/fork-resaved.circ" "$dir/resaved.txt"
      if diff -u "$dir/expected.txt" "$dir/resaved.txt" >"$dir/resaved.diff"; then
        check "resave-sim" "ok"
      else
        check "resave-sim" "FAIL"
      fi
    fi

    # 4. fork writes the same XML as stock
    if resave "$STOCK_JAR" "$circ" "$dir/stock-resaved.circ"; then
      if diff -u <(normalize_xml "$dir/stock-resaved.circ") <(normalize_xml "$dir/fork-resaved.circ") \
          >"$dir/xml.diff"; then
        check "xml" "ok"
      else
        check "xml" "FAIL"
      fi
    else
      check "xml" "FAIL(stock re-save)"
    fi
  else
    check "resave" "FAIL(see $dir/fork-resaved.circ.log)"
  fi

  echo "$rel:$results"
done < <(find "$ROOT/test-circuits" -name '*.circ' -type f | sort)

echo
if [ "$files" -eq 0 ]; then
  echo "No .circ files found under test-circuits/"
  exit 2
fi
echo "$files files, $pass checks passed, $fail failed. Details: build/regression/"
[ "$fail" -eq 0 ]
