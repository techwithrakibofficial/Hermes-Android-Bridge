# Device acceptance test — milestone 1

1. Install the APK from a verified successful GitHub Actions development release.
2. Open Hermes Bridge. Confirm the screen says Hermes is not connected.
3. Tap **Test voice output**. Record whether speech is audible. A completed callback alone does not prove audible sound.
4. Read the Accessibility disclosure, then choose **Continue to settings**. Locate **Hermes Bridge • Manual action tests** and manually enable it if normal Android settings permit.
5. If Android or the EMI policy prevents enabling the service, report the exact message. Do not unlock Developer Options, use root/ADB, or bypass the restriction.
6. Return to the app. Confirm **Service connected** appears.
7. Tap **Test Home**. Confirm the launcher actually opens. Reopen Hermes Bridge.
8. Tap **Test Back**. Observe Android navigation. `accepted` only means Android accepted the request; it does not prove the visible result.
9. Disable the service in normal Settings and return. Verify the status becomes disconnected and test buttons are disabled.
10. Confirm the existing Telegram bot still answers as before. Do not modify its configuration.

Report:
```text
APK release/run:
Install/open:
TTS audible:
Accessibility enable or exact block message:
Home visible result:
Back visible result:
Disable reflected in app:
Existing Telegram still works:
```

## 0.1.1 speech patch acceptance
Install dev-3 / version 0.1.1. Confirm speed defaults to 0.70 on first use. Test speech; if too fast choose 0.50 and test again. Set another speed, fully close/reopen the app and verify the selected value persists. Tap Reset speed and verify 0.70 returns and persists. Record audible behavior; CI cannot confirm speech speed. Development builds can have different signing certificates: if Android rejects an in-place update, uninstall only Hermes Bridge and install the new APK, then enable its Accessibility service normally again. Do not uninstall or clear Termux/Telegram or Hermes data.
