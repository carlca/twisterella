package com.carlca
package twisterella

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*
import com.carlca.config.Config
import com.carlca.logger.Log
import com.carlca.twisterella.TwisterellaSettings
import com.carlca.utils.MathUtil

class TwisterellaExtension(definition: TwisterellaExtensionDefinition, host: ControllerHost)
    extends ControllerExtension(definition, host):
  private val APP_NAME                    = "com.carlca.Twisterella"
  private var last_data1: Int = 0

  private var track1: Track = null
  private var track1VolumeKnob: RelativeHardwareKnob = null

  private val track1KnobChannel = 0 // MIDI Channel for Track 1 Volume Knob
  private val track1KnobCC = 0 // CC number for Track 1 Volume Knob (Now Matching Twister Output - CC 20)
  private val track1LedRingChannel = 0
  private val track1LedRingCC = 0
  private val fullRotation = 127
  // private val sensitivityFactor = 25.0 // Adjust for knob sensitivity
  private val sensitivityFactor = 50.0 // Adjust for knob sensitivity

  var midiIn: MidiIn = null
  var midiOut: MidiOut = null
  var hardwareSurface: HardwareSurface = null

  private def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + channel, cc, value)

  override def init(): Unit =
    Config.init(APP_NAME)
    Log.send("init started")
    val host = getHost

    midiIn = host.getMidiInPort(0) // Now Initialized in class body
    midiOut = host.getMidiOutPort(0)
    hardwareSurface = host.createHardwareSurface()

    // MidiMixLights.init(host)
    TwisterellaSettings.init(host)
    // Tracks.init(host)
    initEvents(host)

    val transport = host.createTransport()
    val trackBank = host.createTrackBank(1, 0, 0) // Showing first track only
    track1 = trackBank.getItemAt(0)
    track1VolumeKnob = hardwareSurface.createRelativeHardwareKnob("Track1VolumeKnob") //USE HardwareSurface
    //track1VolumeKnob.setAdjustValueMatcher(midiIn.createRelativeBinOffsetCCValueMatcher(track1KnobChannel, track1KnobCC, fullRotation)) // REMOVED - Not needed for ENC 3FH/41H
    //track1.volume().value().addBinding(track1VolumeKnob) // Moved binding to onMidi0 - for relative adjust

    track1.volume().value().addValueObserver((volume: Double) => {
        val midiValue = (volume * 127).toInt
        sendMidiToTwister(track1LedRingChannel, track1LedRingCC, midiValue)
    })

    trackBank.followCursorTrack(host.createCursorTrack(8,0))
    Log.send("init finished")
  override def exit(): Unit = ()

  override def flush(): Unit =
    last_data1 = -1
    hardwareSurface.updateHardware()
  end flush

  private def initEvents(host: ControllerHost): Unit =
    initOnMidiCallback(host)
    initOnSysexCallback(host)
  end initEvents

  private def initOnMidiCallback(host: ControllerHost): Unit =
    midiIn.setMidiCallback((status: Int, data1: Int, data2: Int) => onMidi0(ShortMidiMessage(status, data1, data2)))

  private def initOnSysexCallback(host: ControllerHost): Unit =
    midiIn.setSysexCallback(onSysex0(_))

  private def onMidi0(msg: ShortMidiMessage): Unit =
    Log.send(s"MIDI Received: Status=${msg.getStatusByte}, Channel=${msg.getChannel}, Data1=${msg.getData1}, Data2=${msg.getData2}")

    Log.send(s"IF Condition Evaluation - Start:") // ADD LOGGING - START OF IF EVALUATION
    Log.send(s"  msg.getStatusByte() == ShortMidiMessage.CONTROL_CHANGE + track1KnobChannel: ${msg.getStatusByte() == ShortMidiMessage.CONTROL_CHANGE + track1KnobChannel}") // ADD LOGGING - STATUS BYTE COMPARISON
    Log.send(s"  msg.getData1() == track1KnobCC: ${msg.getData1() == track1KnobCC}") // ADD LOGGING - DATA1 COMPARISON


    if msg.getStatusByte() == ShortMidiMessage.CONTROL_CHANGE + track1KnobChannel && msg.getData1() == track1KnobCC then
      Log.send(s"  IF CONDITION IS TRUE - Volume Change Calculation Block IS REACHED!") // ADD LOGGING - IF CONDITION IS TRUE (SHOULD SEE THIS IF CONDITION PASSES)
      val data2 = msg.getData2()
      var volumeChange = 0.0

      if data2 > 64 then // Clockwise rotation
        volumeChange = (data2 - 64.0) / sensitivityFactor // Positive increment
      else if data2 < 64 then // Counter-clockwise rotation
        volumeChange = -(64.0 - data2) / sensitivityFactor // Negative decrement
      else // data2 == 64 (or very close), no change
        volumeChange = 0.0

      val currentVolume = track1.volume().value().get()
      val newVolume = MathUtil.clamp(currentVolume + volumeChange, 0.0, 1.0) // Clamp to 0-1 range

      Log.send(s"Volume Change Calc: Data2=${data2}, volumeChange=${volumeChange}, currentVolume=${currentVolume}, newVolume=${newVolume}")
      track1.volume().value().set(newVolume) // SET VOLUME HERE
    else { // ADDED ELSE BLOCK WITH LOGGING
      Log.send(s"  IF CONDITION IS FALSE - Volume Change Calculation Block IS SKIPPED!") // ADD LOGGING - IF CONDITION IS FALSE (SHOULD SEE THIS IF CONDITION FAILS)
    } // ADDED ELSE BLOCK WITH LOGGING
    Log.send(s"IF Condition Evaluation - End:") // ADD LOGGING - END OF IF EVALUATION
  end onMidi0

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()
