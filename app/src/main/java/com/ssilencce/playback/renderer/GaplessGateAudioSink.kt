package com.ssilencce.playback.renderer

import android.media.AudioDeviceInfo
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioSink
import java.nio.ByteBuffer

/**
 * Delegating audio sink that can turn off gapless trimming. ExoPlayer trims the encoder
 * delay and padding reported in [Format]; zeroing those fields makes the sink play the
 * untrimmed audio, which is the audible opposite of gapless playback.
 */
@UnstableApi
class GaplessGateAudioSink(
    private val delegate: AudioSink,
    private val isGaplessEnabled: () -> Boolean,
) : AudioSink {
    override fun setListener(listener: AudioSink.Listener) = delegate.setListener(listener)

    override fun setPlayerId(playerId: PlayerId?) = delegate.setPlayerId(playerId)

    override fun setClock(clock: Clock) = delegate.setClock(clock)

    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun getFormatSupport(format: Format): Int = delegate.getFormatSupport(format)

    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport = delegate.getFormatOffloadSupport(format)

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long = delegate.getCurrentPositionUs(sourceEnded)

    override fun configure(
        format: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?,
    ) = delegate.configure(formatForPlayback(format), specifiedBufferSize, outputChannels)

    override fun play() = delegate.play()

    override fun handleDiscontinuity() = delegate.handleDiscontinuity()

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean = delegate.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)

    override fun playToEndOfStream() = delegate.playToEndOfStream()

    override fun isEnded(): Boolean = delegate.isEnded

    override fun hasPendingData(): Boolean = delegate.hasPendingData()

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) = delegate.setPlaybackParameters(playbackParameters)

    override fun getPlaybackParameters(): PlaybackParameters = delegate.playbackParameters

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) = delegate.setSkipSilenceEnabled(skipSilenceEnabled)

    override fun getSkipSilenceEnabled(): Boolean = delegate.skipSilenceEnabled

    override fun setAudioAttributes(audioAttributes: AudioAttributes) = delegate.setAudioAttributes(audioAttributes)

    override fun getAudioAttributes(): AudioAttributes = delegate.audioAttributes ?: AudioAttributes.DEFAULT

    override fun getAudioCapabilities(): AudioCapabilities = delegate.audioCapabilities ?: AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES

    override fun setAudioSessionId(audioSessionId: Int) = delegate.setAudioSessionId(audioSessionId)

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) = delegate.setAuxEffectInfo(auxEffectInfo)

    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) {
        delegate.setPreferredDevice(audioDeviceInfo)
    }

    override fun setVirtualDeviceId(virtualDeviceId: Int) = delegate.setVirtualDeviceId(virtualDeviceId)

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) = delegate.setOutputStreamOffsetUs(outputStreamOffsetUs)

    override fun getAudioTrackBufferSizeUs(): Long = delegate.audioTrackBufferSizeUs

    override fun enableTunnelingV21() = delegate.enableTunnelingV21()

    override fun disableTunneling() = delegate.disableTunneling()

    @RequiresApi(29)
    override fun setOffloadMode(offloadMode: Int) = delegate.setOffloadMode(offloadMode)

    @RequiresApi(29)
    override fun setOffloadDelayPadding(
        encoderDelay: Int,
        encoderPadding: Int,
    ) {
        if (isGaplessEnabled()) {
            delegate.setOffloadDelayPadding(encoderDelay, encoderPadding)
        } else {
            delegate.setOffloadDelayPadding(0, 0)
        }
    }

    override fun setAudioOutputProvider(audioOutputProvider: AudioOutputProvider) = delegate.setAudioOutputProvider(audioOutputProvider)

    override fun setVolume(volume: Float) = delegate.setVolume(volume)

    override fun pause() = delegate.pause()

    override fun flush() = delegate.flush()

    override fun reset() = delegate.reset()

    override fun release() = delegate.release()

    private fun formatForPlayback(format: Format): Format =
        if (isGaplessEnabled() || (format.encoderDelay == 0 && format.encoderPadding == 0)) {
            format
        } else {
            format
                .buildUpon()
                .setEncoderDelay(0)
                .setEncoderPadding(0)
                .build()
        }
}
