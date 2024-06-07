package com.fongmi.android.tv.player;

import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class DynamicVolumeAudioProcessor extends BaseAudioProcessor {
    private static final double maxVolume = 4000;
    private static final double minVolume = 2000;
    private static final double threshold = 1000;

    AudioFormat audioFormat;
    double gain;

    protected AudioFormat onConfigure(AudioFormat inputAudioFormat) {
        gain = 1;
        audioFormat = inputAudioFormat;
        return audioFormat;
    }


    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        Double currentVolume = calculateVolume(inputBuffer);
        if (currentVolume != null && currentVolume != 0) {
            if (currentVolume > maxVolume) {
                gain = Math.min(gain, maxVolume / currentVolume);
            }
            if (currentVolume < minVolume) {
                gain = Math.max(gain, minVolume / currentVolume);
            }
        }
        applyGain(inputBuffer, gain);
    }

    private Double calculateVolume(ByteBuffer inputBuffer) {
        final int position = inputBuffer.position();
        final int limit = inputBuffer.limit();


        final int bytesPerFrame = audioFormat.bytesPerFrame;
        final int outputChannels = audioFormat.channelCount;
        final int bytesPerSample = bytesPerFrame / outputChannels;


        int numSamples = (limit - position) / (bytesPerSample);
        if (numSamples == 0) {
            return null;
        }

        int addCnt = 0;

        double sum = 0;
        for (int i = 0; i < numSamples; i++) {
            double sample = 0;
            if (bytesPerSample == 2) {
                sample = inputBuffer.getShort();
            } else if (bytesPerSample == 4) {
                sample = inputBuffer.getInt();
            } else if (bytesPerSample == 8) {
                sample = inputBuffer.getLong();
            }
            if (sample < threshold) {
                continue;
            }
            sum += sample * sample;
            addCnt++;
        }
        inputBuffer.position(position);
        inputBuffer.limit(limit);
        if (addCnt == 0 || (double) addCnt / numSamples < 0.5) {
            return null;
        }
        return Math.sqrt(sum / addCnt);
    }


    private void applyGain(ByteBuffer inputBuffer, double gain) {
        final int position = inputBuffer.position();
        final int limit = inputBuffer.limit();

        final int bytesPerFrame = audioFormat.bytesPerFrame;
        final int outputChannels = audioFormat.channelCount;
        final int bytesPerSample = bytesPerFrame / outputChannels;

        int numSamples = (limit - position) / (bytesPerSample);

        ByteBuffer outputBuffer = replaceOutputBuffer(limit - position);
        for (int i = 0; i < numSamples; i++) {
            if (bytesPerSample == 2) {
                outputBuffer.putShort((short) ((double) inputBuffer.getShort() * gain));
            } else if (bytesPerSample == 4) {
                outputBuffer.putInt((int) ((double) inputBuffer.getInt() * gain));
            } else if (bytesPerSample == 8) {
                outputBuffer.putLong((long) ((double) inputBuffer.getLong() * gain));
            }
        }
        inputBuffer.position(limit);
        outputBuffer.flip();
    }

}