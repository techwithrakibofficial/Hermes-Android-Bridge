import io.github.techwithrakibofficial.hermesbridge.VoiceTurnGate;
import io.github.techwithrakibofficial.hermesbridge.SpeechChunks;
public final class VoiceTurnGateTest {
    private static int n;
    private static void check(boolean pass, String why) { if (!pass) throw new AssertionError(why); n++; }
    public static void main(String[] args) {
        VoiceTurnGate gate = new VoiceTurnGate();
        long first = gate.listen();
        check(first > 0, "user start accepted");
        check(gate.listen() == -1 && !gate.acceptsMicTap(), "double tap cannot overlap recognizers");
        check(!gate.finalResult(first - 1), "old recognition callback ignored");
        check(gate.finalResult(first), "one final recognized turn accepted");
        check(!gate.finalResult(first), "duplicate final does not submit twice");
        check(gate.listen() == -1 && !gate.acceptsMicTap(), "no listening while request is running");
        gate.setAutomatic(true);
        check(gate.speak(first), "reply begins speech");
        check(gate.listen() == -1 && gate.acceptsMicTap(), "speaker blocks automatic listen but allows explicit interruption");
        check(gate.speechDone(first, true), "successful opted-in turn can rearm once");
        check(!gate.speechDone(first, true), "duplicate TTS callback cannot rearm twice");
        long second = gate.listen();
        gate.pause();
        check(!gate.automatic(), "silence/error pauses automatic conversation");
        check(!gate.finalResult(second), "recognizer callback after pause ignored");
        check(!gate.speechDone(first, true), "late output cannot reopen mic after pause");
        long third = gate.request(); gate.setAutomatic(true); gate.speak(third); gate.stop();
        check(!gate.speechDone(third, true) && !gate.automatic(), "Stop cannot be undone by late TTS callback");
        long last = gate.request(); gate.setAutomatic(true); gate.speak(last);
        check(!gate.speechDone(last, false) && !gate.automatic(), "TTS failure has no retry loop");
        gate.stop(); long manual = gate.request(); gate.speak(manual);
        check(!gate.speechDone(manual, true), "manual voice never starts an unsolicited next turn");
        String text = "বাংলা কথা ".repeat(900) + "🙂".repeat(2000);
        var chunks = SpeechChunks.split(text, 1800);
        check(chunks.size() > 2, "long output is chunked");
        for (String chunk : chunks) {
            check(chunk.length() <= 1800 && !chunk.isBlank(), "chunks bounded/nonblank");
            check(!Character.isHighSurrogate(chunk.charAt(chunk.length()-1)) && !Character.isLowSurrogate(chunk.charAt(0)), "emoji surrogate pair not split");
        }
        check(SpeechChunks.split("   ",1800).isEmpty(), "blank speech does not loop");
        System.out.println("PASS: " + n + " voice/chunk lifecycle checks. Real mic/TTS not tested.");
    }
}
