# Hermes Bridge 0.3.0 update — ফোন থেকেই

এই version-এ Settings এবং WhatsApp/Facebook/Messenger/Telegram/Chrome/Termux খোলা যাবে। Screen পড়া, app-এর ভেতরে click/type করা বা message/call এখনও যুক্ত হয়নি। পুরো phone-operating assistant-এর লক্ষ্য docs/PHONE_AGENT_GOAL.md-তে আছে।

## ১. বর্তমান gateway বন্ধ করুন
Termux-এ CTRL তারপর C চাপুন। Gateway সাময়িক বন্ধ হবে; configuration মুছবে না।

## ২. নতুন APK install করুন
[Hermes-Bridge-0.3.0.apk](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-11/Hermes-Bridge-0.3.0.apk)

Update certificate mismatch হলে শুধু Hermes Bridge app reinstall করে Accessibility service সাধারণ Settings থেকে enable করুন। Termux/Telegram/Hermes মুছবেন না। Reinstall করলে app-এর voice preference আবার বেছে নিতে হতে পারে।

## ৩. নতুন bridge session pair করুন
App-এ Start local bridge চাপুন, disclosure পড়ে confirm করুন। Copy Termux pairing command চাপুন। Native Termux-এর খালি $ prompt-এ সেটি আলাদা করে paste ও Enter দিন। Secret token পাঠাবেন না।

## ৪. Adapter update করুন
নিচের পুরো এক লাইন copy/paste করুন। এটি existing private Python/SDK ব্যবহার করে adapter update ও pairing করবে; পাঁচটি tool যাচাই হলে gateway চালাবে। SDK আগে থেকেই আছে বলে সাধারণত পুনরায় package install প্রয়োজন হবে না।

```bash
proot-distro login ubuntu -- env HERMES_BRIDGE_TOKEN="$HERMES_BRIDGE_TOKEN" python3 -c 'import hashlib,os,sys,urllib.request; from pathlib import Path; d=urllib.request.urlopen("https://raw.githubusercontent.com/techwithrakibofficial/Hermes-Android-Bridge/b86c29673939df243f113832efe42af92c71c64b/integrations/hermes_bridge/setup_android_bridge.py",timeout=30).read(); hashlib.sha256(d).hexdigest()=="381b4a1aea4ba8768b30e052dc3dd5dc7353f14657b657f0e81cfe2b1d22efaa" or sys.exit("Download verification failed"); p=Path.home()/".local/share/hermes-android-bridge/setup_android_bridge.py"; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(d); p.chmod(0o600); os.execv(sys.executable,[sys.executable,str(p),"--install-sdk"])'
```

যদি tool-enable prompt আসে, সব পাঁচটি tool enable করতে Y দিন। STOP এলে শুধু সেই line জানান। Bridge local session চালু রাখুন; restart করলে token বদলাবে।

## ৫. Telegram থেকে পরীক্ষা করুন
আগে /new পাঠান। তারপর:

```text
android_bridge_status tool ব্যবহার করে bridge পরীক্ষা করো। allowed_actions ও supported_apps জানাও।
```

ফল ready হলে:

```text
android_open_settings tool ব্যবহার করে আমার ফোনের Settings খোলো।
```

Settings সত্যি খুলেছে কি না দেখুন। তারপর Telegram-এ ফিরে:

```text
android_open_app tool-এ app="whatsapp" দিয়ে WhatsApp খোলো।
```

আপনার ফোনে normal WhatsApp install থাকলে তার screen খোলার কথা। WhatsApp Business বা অন্য variant এই initial alias-এর বিকল্প হিসেবে স্বয়ংক্রিয়ভাবে বাছা হয় না। App না থাকলে বা Android launch আটকালেও agent সফল বলবে না; দৃশ্যমান ফল পরীক্ষা করুন।

## Verified build evidence
- Android compile/lint +30 loopback checks passed: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40637536913
-51 adapter/recovery/setup tests passed on Python3.12 and3.14: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40637536937
-New Settings/app-launch results on the actual phone: pending.
-Private restart launcher: proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
