package com.fongmi.android.tv.player;

import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class DynamicVolumeAudioProcessor extends BaseAudioProcessor {
    private double targetVolume = 3000;

    AudioFormat audioFormat;


    protected AudioFormat onConfigure(AudioFormat inputAudioFormat) {
        audioFormat = inputAudioFormat;
        return audioFormat;
    }

    //
    @Override
    public void queueInput(ByteBuffer inputBuffer) {

        double currentVolume = calculateVolume(inputBuffer);
        double gain = 1;
        if (currentVolume != 0) {
            gain = targetVolume / currentVolume;
        }

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