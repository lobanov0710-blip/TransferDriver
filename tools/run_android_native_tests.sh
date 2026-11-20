#!/usr/bin/env bash
# Keep both independent native reports while preserving every failure and gate.
set -eu
mkdir -p registration-evidence/native-diagnostics
rm -f registration-evidence/.validated registration-evidence/.driver-validated
python3 tools/check_android_emulator_ready.py
driver_status=0
admin_status=0
run_driver() {
  set -e
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --no-daemon --stacktrace
  mkdir -p registration-evidence
  for image in profile-light profile-dark-large vehicle-light vehicle-dark-large vehicle-class-light vehicle-class-dark-large availability-offline-light availability-online-light availability-offline-dark-large availability-online-dark-large; do adb exec-out run-as ru.transferservis.driver cat files/registration-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_registration_evidence.py registration-evidence
  touch registration-evidence/.driver-validated
}
run_admin() {
  set -e
  ./gradlew :app:installDebug --no-daemon --stacktrace
  ./gradlew :admin:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --no-daemon --stacktrace
  for image in assignment-orders-light assignment-options-light assignment-form-light assignment-confirm-light assignment-uncertain-light assignment-orders-dark-large assignment-options-dark-large assignment-form-dark-large assignment-confirm-dark-large assignment-uncertain-dark-large; do adb exec-out run-as ru.transferservis.admin cat files/assignment-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_assignment_evidence.py registration-evidence
  for image in photo-vehicles-light photo-checks-light photo-form-light photo-confirm-light photo-uncertain-light photo-vehicles-dark-large photo-checks-dark-large photo-form-dark-large photo-confirm-dark-large photo-uncertain-dark-large; do adb exec-out run-as ru.transferservis.admin cat files/photo-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_photo_control_evidence.py registration-evidence
  for image in classification-current-light classification-history-light classification-unverified-light classification-current-dark-large classification-history-dark-large classification-unverified-dark-large; do adb exec-out run-as ru.transferservis.admin cat files/classification-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_classification_evidence.py registration-evidence
  for image in assessment-form-light assessment-conditions-light assessment-confirm-light assessment-uncertain-light assessment-recorded-light assessment-form-dark-large assessment-conditions-dark-large assessment-confirm-dark-large assessment-uncertain-dark-large assessment-recorded-dark-large; do adb exec-out run-as ru.transferservis.admin cat files/assessment-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_assessment_evidence.py registration-evidence
  for image in rule-editor-form-light rule-editor-conditions-light rule-editor-confirm-light rule-editor-uncertain-light rule-editor-recorded-light rule-editor-form-dark-large rule-editor-conditions-dark-large rule-editor-confirm-dark-large rule-editor-uncertain-dark-large rule-editor-recorded-dark-large; do adb exec-out run-as ru.transferservis.admin cat files/rule-editor-evidence/$image.png > registration-evidence/$image.png; done
  python3 tools/check_rule_editor_evidence.py registration-evidence
}
# Independent shells retain errexit while the parent records each nonzero status.
bash -e -c "$(declare -f run_driver); run_driver" || driver_status=$?
bash -e -c "$(declare -f run_admin); run_admin" || admin_status=$?
if [[ "$driver_status" -ne 0 || "$admin_status" -ne 0 ]]; then
  python3 -c 'from tools.check_android_emulator_ready import diagnostic; diagnostic()' || true
  adb exec-out run-as ru.transferservis.driver cat files/availability-window-focus-diagnostic.txt > registration-evidence/native-diagnostics/availability-focus.txt 2>/dev/null || true
  adb exec-out run-as ru.transferservis.admin tar -cf - files/assessment-diagnostics > registration-evidence/native-diagnostics/assessment.tar 2>/dev/null || true
  adb exec-out run-as ru.transferservis.admin tar -cf - files/rule-editor-diagnostics > registration-evidence/native-diagnostics/rule-editor.tar 2>/dev/null || true
  exit 1
fi
touch registration-evidence/.validated
