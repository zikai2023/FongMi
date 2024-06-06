package com.fongmi.android.tv.player;

import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;

public class DynamicVolumeAudioProcessor extends BaseAudioProcessor {
    private short targetVolume = 100;

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
            gain = (double) targetVolume / currentVolume;
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


        double avg = 0.0;
        for (int i = 0; i < numSamples; i++) {
            short sample = inputBuffer.getShort();
            avg += ((double) Math.abs(sample) / numSamples);
        }
        inputBuffer.position(position);
        inputBuffer.limit(limit);
        return avg;
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
            short oriSample = inputBuffer.getShort();
            short newSample = (short) ((double) oriSample * gain);
            outputBuffer.putShort(newSample);
        }
        inputBuffer.position(limit);
        outputBuffer.flip();
    }

}