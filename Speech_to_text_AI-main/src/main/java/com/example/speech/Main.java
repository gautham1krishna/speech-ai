package com.example.speech;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.lemur.requests.LemurTaskParams;

import com.assemblyai.api.RealtimeTranscriber;

import javax.sound.sampled.*;
import java.io.IOException;

import static java.lang.Thread.interrupted;

public final class Main {

    public static void main(String[] args) throws IOException {
        Thread thread = new Thread(() -> {
            try {
                RealtimeTranscriber realtimeTranscriber = RealtimeTranscriber.builder()
                        .apiKey("9ec6816034a64928a3322726a0c60f3e")
                        .sampleRate(16_000)
                        .onSessionBegins(sessionBegins -> System.out.println(
                                "Session opened with ID: " + sessionBegins.getSessionId()))
                        .disablePartialTranscripts()
                        .endUtteranceSilenceThreshold(1000)
                        .onFinalTranscript(transcript -> {
                            System.out.println("User: " + transcript.getText());
                            String aiResponse = getAIResponse(transcript.getText());
                            System.out.println("AI Response: " + aiResponse);
                        })
                        .onError(err -> System.out.println("Error: " + err.getMessage()))
                        .build();

                System.out.println("Connecting to real-time transcript service");
                realtimeTranscriber.connect();

                System.out.println("Starting recording");
                AudioFormat format = new AudioFormat(16_000, 16, 1, true, false);
                // `line` is your microphone
                TargetDataLine line = AudioSystem.getTargetDataLine(format);
                line.open(format);
                byte[] data = new byte[line.getBufferSize()];
                line.start();
                while (!interrupted()) {
                    // Read the next chunk of data from the TargetDataLine.
                    line.read(data, 0, data.length);
                    realtimeTranscriber.sendAudio(data);
                }

                System.out.println("Stopping recording");
                line.close();

                System.out.println("Closing real-time transcript connection");
                realtimeTranscriber.close();
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();

        System.out.println("Press ENTER key to stop...");
        System.in.read();
        thread.interrupt();
        System.exit(0);
    }

    private static String getAIResponse(String text) {
        try {
            AssemblyAI client = AssemblyAI.builder()
                    .apiKey("9ec6816034a64928a3322726a0c60f3e")
                    .build();

            var response = client.lemur().task(LemurTaskParams.builder()
                    .prompt("")
                    .inputText(text)
                    .build());

            return response.getResponse().replaceAll("<text>|</text>","").replaceAll("\\s+", " ");
        } catch (Exception e) {
            System.err.println("Error calling API: " + e.getMessage());
            e.printStackTrace();

            return "Error generating AI response";
        }
    }
}
