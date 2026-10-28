package io.github.techwithrakibofficial.hermesbridge;

/** Platform-free turn lifecycle; no timer or error can restart the microphone. */
public final class VoiceTurnGate {
    public enum State { IDLE, LISTENING, WAITING, SPEAKING, PAUSED }
    private State state = State.IDLE;
    private long generation;
    private boolean automatic;
    public State state() { return state; }
    public long ticket() { return generation; }
    public boolean automatic() { return automatic; }
    public void setAutomatic(boolean value) { automatic = value; }
    public long listen() {
        if (state != State.IDLE && state != State.PAUSED) return -1;
        state = State.LISTENING;
        return ++generation;
    }
    public boolean finalResult(long ticket) {
        if (ticket != generation || state != State.LISTENING) return false;
        state = State.WAITING;
        return true;
    }
    public long request() { state = State.WAITING; return ++generation; }
    public boolean speak(long ticket) {
        if (ticket != generation || state != State.WAITING) return false;
        state = State.SPEAKING;
        return true;
    }
    public boolean speechDone(long ticket, boolean success) {
        if (ticket != generation || state != State.SPEAKING) return false;
        state = success ? State.IDLE : State.PAUSED;
        if (!success) automatic = false;
        return success && automatic;
    }
    public void ready(long ticket) { if (ticket == generation) state = State.IDLE; }
    public void pause() { generation++; automatic = false; state = State.PAUSED; }
    public void stop() { generation++; automatic = false; state = State.IDLE; }
}
