#!/usr/bin/env bash
set -euo pipefail
# Flutter's own generator creates the full Android wrapper and platform scaffold.
if [ ! -f android/gradlew ] || [ ! -f android/gradle/wrapper/gradle-wrapper.jar ]; then
  flutter create --platforms=android --org com.radiovoz .
fi
