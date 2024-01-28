#!/usr/bin/env bash
# Path to success: retry
ATTEMPTS=10
for ((i=1; i <= ATTEMPTS; i++)); do
  echo "Attempt #$i"
  ./gradlew :app:generateReleaseBaselineProfile -PdisableSplits

  exit_code=$?
  if [ $exit_code -eq 0 ]; then
    echo "Baseline profile generated on attempt #$i"
    break
  fi

done

if [ $exit_code -ne 0 ]; then
  echo "Script execution failed after $ATTEMPTS attempts."
  exit 1
fi