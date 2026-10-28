package io.github.techwithrakibofficial.hermesbridge;
import java.util.*;
public final class SpeechChunks {
    private SpeechChunks() {}
    public static List<String> split(String input, int maximum) {
        if (maximum < 2) throw new IllegalArgumentException("chunk size");
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < input.length()) {
            int end = Math.min(input.length(), start + maximum);
            if (end < input.length() && Character.isHighSurrogate(input.charAt(end - 1))) end--;
            if (end < input.length()) {
                int space = input.lastIndexOf(' ', end - 1);
                if (space > start + maximum / 2) end = space;
            }
            String part = input.substring(start, end).trim();
            if (!part.isEmpty()) chunks.add(part);
            start = end;
            while (start < input.length() && Character.isWhitespace(input.charAt(start))) start++;
        }
        return chunks;
    }
}
