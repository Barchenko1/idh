package in.demon.helper.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Microphone implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Microphone.class);
    private TargetDataLine microphone;

    private void createMicrophone(AudioFormat format) {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                LOGGER.error("🚫 Microphone not supported.");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        microphone.start();
    }

    public void stop() {
        microphone.stop();
    }

    private ByteArrayOutputStream readChunk() {
        byte[] buffer = new byte[16384];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) {
            int count = microphone.read(buffer, 0, buffer.length);
            if (count > 0) {
                out.write(buffer, 0, count);
            }
        }
        return out;
    }

    public void transcribeToFile(String fileName) {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        createMicrophone(format);
        microphone.start();
        ByteArrayOutputStream out = readChunk();
        microphone.stop();
        microphone.close();
        writeToFile(format, out, fileName);
    }

    private void writeToFile(AudioFormat format, ByteArrayOutputStream out, String fileName) {
        byte[] audioBytes = out.toByteArray();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
             AudioInputStream audioStream = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize())) {
            File wavFile = new File(fileName);
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        microphone.close();
    }
}
