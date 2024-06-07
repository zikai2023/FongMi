package com.fongmi.android.tv.player;

import android.util.Log;

import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class DynamicVolumeAudioProcessor extends BaseAudioProcessor {
    private static final double targetVolume = 2000;
    private static final double threshold = 400;
    private static final double maxGain = 2;
    private static final double minGain = 0.25;

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
            if (currentVolume > targetVolume) {
                // 变小
                gain = Math.max(gain * 0.95, targetVolume / currentVolume);
                gain = Math.max(gain, minGain);
            }
            if (currentVolume > threshold && currentVolume < targetVolume) {
                // 变大
                gain = Math.min(gain * 1.05, targetVolume / currentVolume);
                gain = Math.min(gain, maxGain);
            }
        }
        Log.i("gain", String.valueOf(gain));
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