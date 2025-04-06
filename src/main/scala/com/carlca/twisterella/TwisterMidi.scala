package com.carlca.twisterella

// import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.SysexMessage

object TwisterMidiHelper:

  trait MidiOutput:
    def sendMidi(message: MidiMessage): Unit
    def sendMidi(status: Int, data1: Int, data2: Int): Unit // Existing function

  // Assuming midiOut is an instance of MidiOutput (or adapted to work like one)
  // For example, if midiOut is actually a Receiver:
  class ReceiverMidiOutput(receiver: Receiver) extends MidiOutput:
    override def sendMidi(message: MidiMessage): Unit = receiver.send(message, -1) // -1 for immediate send
    override def sendMidi(status: Int, data1: Int, data2: Int): Unit =
      val shortMessage = new ShortMessage()
      shortMessage.setMessage(status, data1, data2)
      receiver.send(shortMessage, -1)

  // Example usage (replace with your actual midiOut setup)
  // val midiDeviceInfo = ... // Get MidiDeviceInfo for your output device
  // val midiDevice = MidiSystem.getMidiDevice(midiDeviceInfo)
  // midiDevice.open()
  // val receiver = midiDevice.getReceiver
  // val midiOut = new ReceiverMidiOutput(receiver)

  class TwisterController(midiOut: MidiOutput):

    private val sysexStart: Byte = 0xF0.toByte
    private val manufacturerId: Array[Byte] = Array(0x00, 0x01, 0x79).map(_.toByte)
    private val nativeMode: Byte = 0x05.toByte
    private val sysexEnd: Byte = 0xF7.toByte

    private object CommandIds:
      val setNativeModeActive = Array(0x00.toByte)
      val setKnobIndicatorConfig = Array(0x01.toByte, 0x00.toByte)
      val setKnobLedColor = Array(0x01.toByte, 0x01.toByte)

    private object ContentValues:
      val nativeModeInactive = 0x00.toByte
      val nativeModeActive = 0x01.toByte

    private object IndicatorTypes:
      val dot = 0x00.toByte
      val bar = 0x01.toByte
      val blendedBar = 0x02.toByte
      val blendedDot = 0x03.toByte

    private object DetentValues:
      val noDetent = 0x00.toByte
      val hasDetent = 0x01.toByte


    private def createSysexMessage(commandId: Array[Byte], content: Array[Byte]): SysexMessage =
      val data = Array.concat(manufacturerId, Array(nativeMode), commandId, content)
      val message = new SysexMessage()
      message.setMessage(SysexMessage.SYSTEM_EXCLUSIVE, data, data.length)
      message


    private def sendSysExMessage(message: SysexMessage): Unit =
      val fullMessageData = Array.concat(Array(sysexStart), message.getData, Array(sysexEnd))
      val fullSysexMessage = new SysexMessage()
      fullSysexMessage.setMessage(SysexMessage.SYSTEM_EXCLUSIVE, fullMessageData, fullMessageData.length)
      midiOut.sendMidi(fullSysexMessage)

    // Existing function for Control Change
    def sendMidiToTwister(channel: Int, cc: Int, value: Int): Unit =
      midiOut.sendMidi(ShortMessage.CONTROL_CHANGE + channel, cc, value)

    // Native Mode Commands

    def enterNativeMode: Unit =
      val content = Array(ContentValues.nativeModeActive)
      val message = createSysexMessage(CommandIds.setNativeModeActive, content)
      sendSysExMessage(message)


    def leaveNativeMode: Unit =
      val content = Array(ContentValues.nativeModeInactive)
      val message = createSysexMessage(CommandIds.setNativeModeActive, content)
      sendSysExMessage(message)


    def setKnobIndicatorConfig(knobIndex: Int, indicatorType: Int, hasDetent: Boolean, detentColor: Int): Unit =
      require(knobIndex >= 0 && knobIndex <= 15, "Knob index must be between 0 and 15")
      require(indicatorType >= 0 && indicatorType <= 3, "Indicator type must be between 0 and 3")
      require(detentColor >= 0 && detentColor <= 127, "Detent color must be between 0 and 127")

      val detentByte = if hasDetent then DetentValues.hasDetent else DetentValues.noDetent
      val content = Array(
        knobIndex.toByte,
        indicatorType.toByte,
        detentByte,
        detentColor.toByte
      )
      val message = createSysexMessage(CommandIds.setKnobIndicatorConfig, content)
      sendSysExMessage(message)


    def setKnobLedColor(knobIndex: Int, red: Int, green: Int, blue: Int): Unit =
      require(knobIndex >= 0 && knobIndex <= 15, "Knob index must be between 0 and 15")
      require(red >= 0 && red <= 127, "Red component must be between 0 and 127")
      require(green >= 0 && green <= 127, "Green component must be between 0 and 127")
      require(blue >= 0 && blue <= 127, "Blue component must be between 0 and 127")

      val content = Array(
        knobIndex.toByte,
        red.toByte,
        green.toByte,
        blue.toByte
      )
      val message = createSysexMessage(CommandIds.setKnobLedColor, content)
      sendSysExMessage(message)

    def setKnobRGBColor(knobIndex: Int, red: Int, green: Int, blue: Int): Unit =
      require(knobIndex >= 0 && knobIndex <= 15, "Knob index must be between 0 and 15")
      require(red >= 0 && red <= 255, "Red component must be between 0 and 255")
      require(green >= 0 && green <= 255, "Green component must be between 0 and 255")
      require(blue >= 0 && blue <= 255, "Blue component must be between 0 and 255")
      setKnobLedColor(knobIndex, red >> 1, green >> 1, blue >> 1)
