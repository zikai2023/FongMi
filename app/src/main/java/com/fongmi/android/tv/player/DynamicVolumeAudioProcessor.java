package com.fongmi.android.tv.player;

import android.util.Log;

import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class DynamicVolumeAudioProcessor extends BaseAudioProcessor {
    private final double maxVolume = 3500;
    private final double minVolume = 1000;
    private final double threshold = 600;
    private final double maxGain = 1.5;

    AudioFormat audioFormat;

    double gain = 1;


    protected AudioFormat onConfigure(AudioFormat inputAudioFormat) {
        gain = 1;
        audioFormat = inputAudioFormat;
        return audioFormat;
    }

    //
    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        double currentVolume = calculateVolume(inputBuffer);
        if (currentVolume > maxVolume) {
            gain = Math.min(gain, maxVolume / currentVolume);
        }
        if (currentVolume > threshold && currentVolume < minVolume) {
            gain = Math.min(maxGain, Math.max(gain, minVolume / currentVolume));
        }
        Log.i("gain", String.valueOf(gain));
        Log.i("currentVolume", String.valueOf(currentVolume));
        applyGain(inputBuffer, gain);
    }

    private double calculateVolume(ByteBuffer inputBuffer) {
        final int position = inputBuffer.position();
        final int limit = inputBuffer.limit();


        final int bytesPerFrame = audioFormat.bytesPerFrame;
        final int outputChannels = audioFormat.channelCount;
        final int bytesPerSample = bytesPerFrame / outputChannels;


        int numSamples = (limit - position) / (bytesPerSample);
        if (numSamples == 0) {
            return 1;
        }

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
            sum += sample * sample;
        }
        inputBuffer.position(position);
        inputBuffer.limit(limit);
        return Math.sqrt(sum / numSamples);
    }

    //
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