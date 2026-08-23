#!/usr/bin/env bash
# Keyforge device smoke test — run AFTER installing a build on the phone.
# Catches the silent-failure class (e.g. logd serving only self logs despite
# READ_LOGS) that unit tests cannot see. Requires USB debugging + the
# debuggable CI APK. No root needed.
#
# Usage: scripts/device-smoke.sh   (override device binary with ADB=/path/to/adb)
set -u
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PKG="com.nordairemapper"
FAIL=0

ok()  { echo "PASS  $1"; }
bad() { echo "FAIL  $1"; FAIL=1; }
warn(){ echo "WARN  $1"; }

sh() { "$ADB" shell "$1" 2>/dev/null | tr -d '\r'; }

if [ "$("$ADB" get-state 2>/dev/null | tr -d '\r')" != "device" ]; then
  echo "No device connected (USB debugging off? tethering mode?)."
  exit 2
fi

# 1 · Installed + fresh
UPD=$(sh "dumpsys package $PKG" | grep lastUpdateTime | tail -1)
[ -n "$UPD" ] && ok "installed (${UPD#lastUpdateTime=})" || bad "app not installed"

# 2 · Accessibility listed in secure settings
case "$(sh "settings get secure enabled_accessibility_services")" in
  *$PKG*) ok "accessibility service listed" ;;
  *) bad "accessibility service NOT in enabled_accessibility_services" ;;
esac

# 3 · Accessibility actually bound (enabled != bound)
if sh "dumpsys accessibility" | grep -F 'Bound services' >/dev/null &&
   sh "dumpsys accessibility" | sed -n '/Bound services/,/Binding services/p' | grep -q "$PKG"; then
  ok "accessibility service bound"
else
  bad "accessibility service not bound (check dumpsys accessibility)"
fi

# 4 · Grants on user 0 (ignore clone-profile lines)
P="$(sh "dumpsys package $PKG")"
echo "$P" | grep -q 'READ_LOGS: granted=true' \
  && ok "READ_LOGS granted" || bad "READ_LOGS missing — run Unlock"
echo "$P" | grep -q 'WRITE_SECURE_SETTINGS' \
  && ok "WRITE_SECURE_SETTINGS requested/granted block found" \
  || warn "WRITE_SECURE_SETTINGS absent (banking Auto-Pause needs Unlock)"

# 5 · THE BIG ONE — logd actually delivers other apps' logs to our uid.
#     Probes the exact view the watcher's spawned `logcat` gets.
SYS_PID=$(sh "pidof system_server" | awk '{print $1}')
if [ -n "${SYS_PID:-}" ]; then
  VIS=$(sh "run-as $PKG logcat -d -b main -v brief -t 3000" | grep -c "( *${SYS_PID})")
  if [ "${VIS:-0}" -gt 0 ]; then
    ok "logd visibility OK ($VIS system_server lines visible to app uid)"
  else
    bad "logd BLIND: app uid sees none of system_server's logs — READ_LOGS not honored. Fix: reboot phone; if persists enable 'USB debugging (Security settings)', reboot, re-run Unlock. See docs/changes/2026-08-23-readlogs-logd-blind/"
  fi
else
  warn "could not resolve system_server pid; skipped visibility probe"
fi

# 6 · Watcher FGS alive + foreground
REC=$(sh "dumpsys activity services $PKG" | sed -n '/LogcatWatcherService/,/^  \* /p')
echo "$REC" | grep -q 'isForeground=true' \
  && ok "LogcatWatcherService running as FGS" || bad "watcher not running as FGS"

# 7 · Advisory: has any gesture been classified since last data wipe?
if sh "run-as $PKG cat files/datastore/settings.preferences_pb" | strings | grep -q last_plus_key_seen; then
  ok "detection health: at least one gesture recorded"
else
  warn "no gesture recorded yet — press the Plus Key once, re-run this script"
fi

echo
if [ "$FAIL" -eq 0 ]; then echo "SMOKE: ALL GREEN"; exit 0; else echo "SMOKE: FAILURES ABOVE"; exit 1; fi
