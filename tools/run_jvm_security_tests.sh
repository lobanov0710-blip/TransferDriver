#!/usr/bin/env bash
# Focused production-Java tests; Android Gradle checks must also pass before release.
set -euo pipefail
task_lib_dir=${1:?Usage: bash tools/run_jvm_security_tests.sh /path/to/test-jars}
task_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
task_classes=$(mktemp -d "${TMPDIR:-/tmp}/transferdriver-tests.XXXXXX")
trap 'rm -rf -- "$task_classes"' EXIT
task_cp=""
for task_jar in junit-4.13.2.jar hamcrest-core-1.3.jar okhttp-4.12.0.jar okio-jvm-3.6.0.jar kotlin-stdlib-1.9.10.jar; do
    test -f "$task_lib_dir/$task_jar"
    task_cp="${task_cp:+$task_cp:}$task_lib_dir/$task_jar"
done
cd -- "$task_root"
task_sources=app/src/main/java/ru/transferservis/driver
"${JAVA_COMPILER:-javac}" --release 11 -cp "$task_cp" -d "$task_classes" \
    "$task_sources/security/ApiEndpointPolicy.java" \
    "$task_sources/security/UuidValidator.java" \
    "$task_sources/data/session/DriverSession.java" \
    "$task_sources/data/api/SecureHttpClient.java" \
    app/src/test/java/ru/transferservis/driver/security/*.java
"${JAVA_RUNNER:-java}" -cp "$task_classes:$task_cp" org.junit.runner.JUnitCore \
    ru.transferservis.driver.security.ApiEndpointPolicyTest \
    ru.transferservis.driver.security.UuidValidatorTest \
    ru.transferservis.driver.security.DriverSessionSecurityTest \
    ru.transferservis.driver.security.SecureHttpClientTest
