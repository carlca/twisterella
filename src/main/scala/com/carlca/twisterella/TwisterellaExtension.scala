package com.carlca.twisterella

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

class TwisterellaExtension(definition: TwisterellaExtensionDefinition, host: ControllerHost)
  extends ControllerExtension(definition, host):
  private val STATUS_RANGE = ShortMidiMessage.CONTROL_CHANGE to (ShortMidiMessage.CONTROL_CHANGE + 15)
  private val CC_RANGE = 0 to 15

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

  var midiIn: MidiIn = null
  var midiOut: MidiOut = null
  var hardwareSurface: HardwareSurface = null
  // var twister: Twister = null

  override def init(): Unit =
    midiIn = host.getMidiInPort(0)
    midiOut = host.getMidiOutPort(0)
    hardwareSurface = host.createHardwareSurface()

    // twister = Twister(this)(using hardwareSurface)

    ExtensionSettings.settingsCapabilities += SettingsCapability.`Track Mapping Behaviour`
    ExtensionSettings.init(host)
    Tracks.init(host)
    initEvents
    registerTrackVolumeObservers
    setKnobColor
    listTrackColors
    testColors
    listTwisterColors

  def testColors: Unit =
    Log.time
    Log.sendColor("██████", 255, 0, 0)
    Log.sendColor("██████", 0, 255, 0)
    Log.sendColor("██████", 0, 0, 255)

  def listTwisterColors: Unit =
    for (i <- 0 until TwisterColors.ALL.size) do
      val twisterColor = TwisterColors.ALL(i)
      Log.send(s"Twister Color $i ~").sendColor("██████", twisterColor.getRed255, twisterColor.getGreen255, twisterColor.getBlue255)

  def setKnobColor: Unit =
    val color: Int = 10 // blue
    val colorChannel: Int = 1
    for (knobIndex <- 0 to 7) do
      midiOut.sendMidi(0xB0 + colorChannel, knobIndex, color)
      Thread.sleep(10)

  def createTrackVolumeObserver(trackIndex: Int): Unit =
    Tracks.getVolumeParam(trackIndex).fold(println(s"Warning: No volume parameter found for track $trackIndex"))(
      parameter =>
        parameter.value().addValueObserver(volume => sendMidiToTwister(0, trackIndex, (volume * 127).toInt))
        sendMidiToTwister(0, trackIndex, Tracks.getVolumeLevel(trackIndex))
    )

  def listTrackColors: Unit =
    for (t <- 0 until Bank.NUM_KNOBS) do
      val colorValue: ColorValue = Tracks.getColorValue(t)
      colorValue.addValueObserver(new ColorValueChangedCallback {
        override def valueChanged(red: Float, green: Float, blue: Float): Unit =
          val bwRGB = Color.fromRGB(red, green, blue)
          Log.send(s"Track $t ${Tracks.getTrackName(t)} ~").sendColor("██████", bwRGB.getRed255, bwRGB.getGreen255, bwRGB.getBlue255)
          val twCol = TwisterColors.findTwisterColorRGB(bwRGB)
          // val twCol = TwisterColors.findTwisterColorLab(bwRGB)
          val twRGB = TwisterColors.ALL(twCol)
          val (r, g, b) = TwisterColors.unpackColor(twRGB)
          Log.sendColor("██████", r, g, b)
          Log.send(s"twCol: $twCol  r: $r  g: $g  b $g")
      })

  def registerTrackVolumeObservers: Unit =
    (0 until 16).foreach(createTrackVolumeObserver)

  // New function to observe track colors
  // def observeTrackColors: Unit =
  //   val trackBank = Tracks.getTrackBank
  //   for (i <- 0 until Bank.NUM_KNOBS) do
  //     val track = trackBank.getItemAt(i)
  //     if track != null then
  //       val trackIndex = i
  //       val colorValue: ColorValue = track.color()
  //       colorValue.addValueObserver(new ColorValueChangedCallback {
  //         override def valueChanged(red: Float, green: Float, blue: Float): Unit =
  //           val bitwigColor = Color.fromRGB(red, green, blue)
  //           val twisterColorIndex = findTwisterColor(bitwigColor) //Convert the Bitwig color to twist color
  //           Log.send(s"Track $trackIndex Color: Bitwig(${bitwigColor.getRed255},${bitwigColor.getGreen255},${bitwigColor.getBlue255}), TwisterIndex=$twisterColorIndex") //Logging
  //           setKnobRgbLight(trackIndex, 0, twisterColorIndex) //Set new RGB Light
  //       })
  //       track.exists().addValueObserver(exists =>
  //         if (!exists) then
  //           setKnobRgbLight(i, 0, 0)
  //       )

  // def updateTrackColor(trackIndex: Int, r: Double, g: Double, b: Double): Unit =
  //   val bitwigColor = Color.fromRGB(r,g,b) //get and generate color using new API's
  //   val twisterColorIndex = findTwisterColor(bitwigColor) //Convert the Bitwig color to twist color
  //   Log.send(s"Track $trackIndex Color: Bitwig(${bitwigColor.getRed255},${bitwigColor.getGreen255},${bitwigColor.getBlue255}), TwisterIndex=$twisterColorIndex") //Logging
  //   setKnobRgbLight(trackIndex, 0, twisterColorIndex) //Set new RGB Light

  override def exit(): Unit = ()

  override def flush(): Unit =
    hardwareSurface.updateHardware()

  private def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + channel, cc, value)

  // private def setKnobRgbLight(knobIndex: Int, bankIndex: Int, colorValue: Int): Unit =
  //   val cc = knobIndex + (Bank.NUM_KNOBS * bankIndex)
  //   sendMidiToTwister(MidiChannel.RGB_ANIMATION, cc, colorValue)

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
    val (status, channel, cc, data2) = unpackMsg(msg, true)
    // Check status in range 176 to 191 (0xB0 to 0xBF) and CC in range 0 to 15
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
    sendMidiToTwister(channel, cc, (newVolume * 127).toInt)

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()
