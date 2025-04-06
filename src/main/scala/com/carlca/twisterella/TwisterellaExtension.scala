package com.carlca.twisterella

import scala.collection.mutable.ListBuffer
import scala.util.Random
import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*
import com.carlca.bitwiglibrary.Tracks
import com.carlca.utils.MathUtil
import com.carlca.logger.Log
import com.carlca.bitwiglibrary.ExtensionSettings
import com.carlca.bitwiglibrary.ExtensionSettings.SettingsCapability
import com.bitwig.extension.api.Color
import com.bitwig.extension.callback.ColorValueChangedCallback
import com.carlca.twisterella.TwisterMidiHelper.TwisterController

class TwisterellaExtension(definition: TwisterellaExtensionDefinition, host: ControllerHost)
  extends ControllerExtension(definition, host):

  private val bwList = new ListBuffer[String]
  private val twList = new ListBuffer[String]
  private val STATUS_RANGE = (0xB0 to 0xB0 + 15)
  private val CC_RANGE = (0 to 15)

  object MidiChannel:
    val ENCODER: Int = 0
    val BUTTON: Int = 1
    val RGB_ANIMATION: Int = 2
    val SIDE_BUTTON: Int = 3
    val SYSTEM: Int = 3
    val SHIFT: Int = 4
    val RING_ANIMATION: Int = 5
    val SEQUENCER: Int = 7

  object Bank:
    val NUM_KNOBS: Int = 16

  object DEBUG:
    val enabled: Boolean = true

  var midiIn: MidiIn = null
  var midiOut: MidiOut = null
  var hardwareSurface: HardwareSurface = null

  var twisterController: TwisterController = null

  override def init(): Unit =
    midiIn = host.getMidiInPort(0)
    midiOut = host.getMidiOutPort(0)

    hardwareSurface = host.createHardwareSurface()
    ExtensionSettings.settingsCapabilities += SettingsCapability.`Track Mapping Behaviour`
    ExtensionSettings.init(host)
    Tracks.init(host)

    twisterController = new TwisterController(new BitwigMidiOutput(midiOut))

    initEvents
    registerTrackVolumeObservers
    twisterController.enterNativeMode
    // createTrackColorObservers
    testSysExTrackColors

  def createTrackVolumeObserver(trackIndex: Int): Unit =
    Tracks.getVolumeParam(trackIndex).fold(println(s"Warning: No volume parameter found for track $trackIndex"))(
      parameter =>
        parameter.value().addValueObserver(volume => twisterController.sendMidiToTwister(0, trackIndex, (volume * 127).toInt))
        twisterController.sendMidiToTwister(0, trackIndex, Tracks.getVolumeLevel(trackIndex))
    )

  def getTwisterColor(red: Float, green: Float, blue: Float): Int =
    TwisterColors.findTwisterColorRGB(Color.fromRGB(red, green, blue))

  def setKnobColor(knob: Int, colorIndex: Int): Unit =
    midiOut.sendMidi(0xB0 + 1, knob, colorIndex)
    Thread.sleep(10)

  def createTrackColorObservers: Unit =
    (0 until Bank.NUM_KNOBS).foreach: track =>
      val colorValue: ColorValue = Tracks.getColorValue(track)
      colorValue.addValueObserver(new ColorValueChangedCallback {
        override def valueChanged(red: Float, green: Float, blue: Float): Unit =
          if DEBUG.enabled then
            val bwColor = Color.fromRGB(red, green, blue)
            val (r1, g1, b1) = TwisterColors.unpackColor(bwColor)
            val bwBlock = Log.colorString("██████", r1, g1, b1)
            bwList.append(bwBlock + " ")

            val twCol = getTwisterColor(red, green, blue)
            val (r2, g2, b2) = TwisterColors.unpackColor(TwisterColors.ALL(twCol))
            val twBlock = Log.colorString("██████", r2, g2, b2)
            twList.append(twBlock + " ")

            if (bwList.size == 4) && (twList.size == 4) then
              Log.send("  Bitwig: " + bwList.take(4).mkString(" "))
              Log.send(" Twister: " + twList.take(4).mkString(" "))
              Log.blank
              bwList.clear
              twList.clear
            twisterController.setKnobRGBColor(track, r2, g2, b2)
            // setKnobColor(track, twCol)
          else
            val bwColor = Color.fromRGB(red, green, blue)
            val (r1, g1, b1) = TwisterColors.unpackColor(bwColor)
            twisterController.setKnobRGBColor(track, r1, g1, b1)
            // setKnobColor(track, getTwisterColor(red, green, blue))
      })

  def testSysExTrackColors: Unit =
    (1 until 127).foreach: index =>
      val (r, g, b) = TwisterColors.unpackColor(TwisterColors.ALL(index))
      (0 to 15).foreach: track =>
        twisterController.setKnobRGBColor(track, r, g, b)
      Log.send(s"r: {$r}  g: {$g}  b: {$b}")
      Log.sendColor("████████████", r, g, b)
      Log.sendColor("████████████", r, g, b)
      Log.sendColor("████████████", r, g, b)
      Log.sendColor("████████████", r, g, b)
      Log.sendColor("████████████", r, g, b)
      Log.sendColor("████████████", r, g, b)
      Thread.sleep(2000)
      Log.cls

  def registerTrackVolumeObservers: Unit =
    (0 until 16).foreach(createTrackVolumeObserver)

  override def exit(): Unit = ()

  override def flush(): Unit =
    hardwareSurface.updateHardware()

  // private def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
  //   midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + channel, cc, value)

  private def initEvents: Unit =
    initOnMidiCallback
    initOnSysexCallback

  private def initOnMidiCallback: Unit =
    midiIn.setMidiCallback((status: Int, data1: Int, data2: Int) =>
      onMidi0(ShortMidiMessage(status, data1, data2))
    )

  private def initOnSysexCallback: Unit =
    midiIn.setSysexCallback(onSysex0(_))

  private def unpackMsg(msg: ShortMidiMessage, doLog: Boolean = false): (Int, Int, Int, Int) =
    val status = msg.getStatusByte()
    val channel = status & 0x0F
    val cc = msg.getData1()
    val data2 = msg.getData2()
    if doLog then Log.send(s"unpackMsg: status=$status, channel=$channel, cc=$cc, data2=$data2")
    (status, channel, cc, data2)

  private def onMidi0(msg: ShortMidiMessage): Unit =
    val (status, channel, cc, data2) = unpackMsg(msg, false)
    if (STATUS_RANGE contains status) && (CC_RANGE contains cc) then
      processMsg(channel, cc, data2)

  private def processMsg(channel: Int, cc: Int, data2: Int): Unit =
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
    Tracks.setVolume(track, (newVolume * 127).toInt)
    twisterController.sendMidiToTwister(channel, cc, (newVolume * 127).toInt)

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()
