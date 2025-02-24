package com.carlca.twisterella

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*
import com.carlca.bitwigutils.Tracks
import com.carlca.config.Config
import com.carlca.utils.MathUtil
import com.carlca.logger.Log

class TwisterellaExtension(
    definition: TwisterellaExtensionDefinition,
    host: ControllerHost
) extends ControllerExtension(definition, host):
  private val APP_NAME = "com.carlca.Twisterella"

  var midiIn: MidiIn = null
  var midiOut: MidiOut = null
  var hardwareSurface: HardwareSurface = null

  override def init(): Unit =
    Config.init(APP_NAME)
    val host = getHost
    midiIn = host.getMidiInPort(0)
    midiOut = host.getMidiOutPort(0)
    hardwareSurface = host.createHardwareSurface()
    TwisterellaSettings.init(host)
    Tracks.init(host)
    initEvents
    registerTrackVolumeObservers
  end init

  def createTrackVolumeObserver(trackIndex: Int): Unit =
    Tracks.getVolumeParam(trackIndex).fold(println(s"Warning: No volume parameter found for track $trackIndex"))(
      parameter => {
        parameter.value().addValueObserver(volume => sendMidiToTwister(0, trackIndex, (volume * 127).toInt))
        sendMidiToTwister(0, trackIndex, Tracks.getVolumeLevel(trackIndex))
      }
    )

  def registerTrackVolumeObservers: Unit =
    (0 until 16).foreach(createTrackVolumeObserver)

  override def exit(): Unit = ()

  override def flush(): Unit =
    hardwareSurface.updateHardware()
  end flush

  private def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + channel, cc, value)

  private def initEvents: Unit =
    initOnMidiCallback
    initOnSysexCallback
  end initEvents

  private def initOnMidiCallback: Unit =
    midiIn.setMidiCallback((status: Int, data1: Int, data2: Int) =>
      onMidi0(ShortMidiMessage(status, data1, data2))
    )

  private def initOnSysexCallback: Unit =
    midiIn.setSysexCallback(onSysex0(_))

  // private def onMidi0(msg: ShortMidiMessage): Unit =
  //   //                                                          track1KnobChannel      track1KnobCC
  //   if msg.getStatusByte() == ShortMidiMessage.CONTROL_CHANGE + 0 && msg
  //       .getData1() == 0
  //   then
  //     val data2 = msg.getData2()
  //     var volumeChange = 0.0

  //     // val sensitivity = Settings.sensitivity
  //     val sensitivity = 50.0

  //     if data2 > 64 then // Clockwise rotation
  //       volumeChange = (data2 - 64.0) / sensitivity // Positive increment
  //     else if data2 < 64 then // Counter-clockwise rotation
  //       volumeChange = -(64.0 - data2) / sensitivity // Negative decrement
  //     else // data2 == 64 (or very close), no change
  //       volumeChange = 0.0

  //     val currentVolume = Tracks.getVolumeLevel(0) / 127.0
  //     val newVolume = MathUtil.clamp(
  //       currentVolume + volumeChange,
  //       0.0,
  //       1.0
  //     ) // Clamp to 0-1 range
  //     Tracks.setVolume(0, (newVolume * 127).toInt) // SET VOLUME HERE
  // end onMidi0

  private def onMidi0(msg: ShortMidiMessage): Unit =
    val status = msg.getStatusByte()
    val channel = status & 0x0F // Extract the MIDI channel (lower 4 bits)
    val cc = msg.getData1()

    if (status >= ShortMidiMessage.CONTROL_CHANGE && status <= (ShortMidiMessage.CONTROL_CHANGE + 15)) then
      val data2 = msg.getData2()
      var volumeChange = 0.0

      // val sensitivity = Settings.sensitivity
      val sensitivity = 50.0

      if data2 > 64 then // Clockwise rotation
        volumeChange = (data2 - 64.0) / sensitivity // Positive increment
      else if data2 < 64 then // Counter-clockwise rotation
        volumeChange = -(64.0 - data2) / sensitivity // Negative decrement
      else // data2 == 64 (or very close), no change
        volumeChange = 0.0

      val track = cc // Use the CC value directly as the track index
      val currentVolume = Tracks.getVolumeLevel(track) / 127.0
      val newVolume = MathUtil.clamp(currentVolume + volumeChange, 0.0, 1.0) // Clamp to 0-1 range
      Tracks.setVolume(track, (newVolume * 127).toInt) // SET VOLUME HERE
      sendMidiToTwister(channel, cc, (newVolume * 127).toInt)
    end if
  end onMidi0

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()
