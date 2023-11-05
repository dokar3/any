#!/usr/bin/env bash
ATTEMPTS=5
for ((i=1; i <= ATTEMPTS; i++)); do
  echo "Attempt $i"
  ./gradlew macrobenchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=any.macrobenchmark.BaselineProfileGenerator \
  -Pandroid.testoptions.manageddevices.emulator.gpu="swiftshader_indirect" \
  -PdisableSplits

  exit_code=$?
  if [ $exit_code -eq 0 ]; then
    break
  fi

done

if [ $exit_code -ne 0 ]; then
  echo "Script execution failed after $ATTEMPTS attempts."
  exit 1
fi