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
