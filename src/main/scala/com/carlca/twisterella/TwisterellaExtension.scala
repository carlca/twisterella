package com.carlca.twisterella

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*
import com.carlca.bitwigutils.Tracks
import com.carlca.config.Config
// import com.carlca.logger.Log
import com.carlca.utils.MathUtil

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
    initEvents(host)

    Tracks.getTrackBank
      .getItemAt(0)
      .volume()
      .value()
      .addValueObserver((volume: Double) => {
        val midiValue = (volume * 127).toInt
        sendMidiToTwister(0, 0, midiValue)
      })

    val initialVolume = Tracks.getVolume(0)
    sendMidiToTwister(0, 0, initialVolume)

  end init

  override def exit(): Unit = ()

  override def flush(): Unit =
    hardwareSurface.updateHardware()
  end flush

  private def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + channel, cc, value)

  private def initEvents(host: ControllerHost): Unit =
    initOnMidiCallback(host)
    initOnSysexCallback(host)
  end initEvents

  private def initOnMidiCallback(host: ControllerHost): Unit =
    midiIn.setMidiCallback((status: Int, data1: Int, data2: Int) =>
      onMidi0(ShortMidiMessage(status, data1, data2))
    )

  private def initOnSysexCallback(host: ControllerHost): Unit =
    midiIn.setSysexCallback(onSysex0(_))

  private def onMidi0(msg: ShortMidiMessage): Unit =
    //                                                          track1KnobChannel      track1KnobCC
    if msg.getStatusByte() == ShortMidiMessage.CONTROL_CHANGE + 0 && msg
        .getData1() == 0
    then
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

      val currentVolume = Tracks.getVolume(0) / 127.0
      val newVolume = MathUtil.clamp(
        currentVolume + volumeChange,
        0.0,
        1.0
      ) // Clamp to 0-1 range
      Tracks.setVolume(0, (newVolume * 127).toInt) // SET VOLUME HERE
  end onMidi0

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()
