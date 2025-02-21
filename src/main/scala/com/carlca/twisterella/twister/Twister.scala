package com.carlca.twisterella.twister

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.{ControllerHost, HardwareSurface, MidiIn, MidiOut}
import com.carlca.twisterella.TwisterellaExtension
import scala.collection.mutable

/**
 * Twister hardware.
 *
 * All the hardware available on the MIDI Fighter Twister is setup on construction and accessible
 * through this class.
 */
class Twister(extension: TwisterellaExtension):
  import Twister.MidiChannel
  import Twister.Bank

  private val midiOut: MidiOut = extension.midiOut
  private val hardwareSurface: HardwareSurface = extension.hardwareSurface
  private val host: ControllerHost = extension.getHost

  private var popupEnabled: Boolean = false
  private var activeBank: Int = -1

  val banks: Array[Bank] = Array.ofDim[Bank](Twister.NUM_BANKS)

  createHardware(extension)
  createSequencerInput(extension.midiIn)
  createBankSwitchListener(extension.midiIn)

  /**
   * Sets the active bank to the next available bank.
   *
   * If the currently active bank is the last bank then this does nothing.
   */
  def nextBank(): Unit =
    val nextBank = activeBank + 1
    if nextBank >= Twister.NUM_BANKS then return
    setActiveBank(nextBank)

  /**
   * Sets the active bank to the previous available bank.
   *
   * If the currently active bank is the first bank then this does nothing.
   */
  def previousBank(): Unit =
    val previousBank = activeBank - 1
    if previousBank < 0 then return
    setActiveBank(previousBank)

  /**
   * Sets the active bank to the desired index.
   *
   * @param index Desired bank index.
   */
  def setActiveBank(index: Int): Unit =
    assert(index >= 0 && index < Twister.NUM_BANKS, "index is invalid")

    if activeBank == index then return // already active, do nothing

    activeBank = index
    midiOut.sendMidi(ShortMidiMessage.CONTROL_CHANGE + MidiChannel.SYSTEM, activeBank, 127)
    showBankChangePopup(activeBank)

  /**
   * Enables/disables any popup notifications related to Twister activity.
   *
   * @param enabled True to enable, false to disable.
   */
  def setPopupEnabled(enabled: Boolean): Unit =
    popupEnabled = enabled

  /** Turns off all lights on the Twister. */
  def lightsOff(): Unit =
    // Helps with "stuck" lights when quitting Bitwig
    hardwareSurface.updateHardware()

    banks.foreach(_.knobs.foreach(_.lightsOff()))

  /** Creates all the hardware for the Twister. */
  private def createHardware(extension: TwisterellaExtension): Unit =
    val sideButtonsFirstLeftCC = 8
    val sideButtonsFirstRightCC = sideButtonsFirstLeftCC + Twister.Bank.NUM_LEFT_SIDE_BUTTONS

    for bank <- 0 until banks.length do
      banks(bank) = new Bank()

      val hardwareKnobs = banks(bank).knobs
      val hardwareLSBs = banks(bank).leftSideButtons
      val hardwareRSBs = banks(bank).rightSideButtons

      val knobsBankOffset = Twister.Bank.NUM_KNOBS * bank
      val sideButtonsBankOffset = Twister.Bank.NUM_SIDE_BUTTONS * bank

      for knob <- 0 until hardwareKnobs.length do
        val cc = knob + knobsBankOffset
        val encoder = MidiInfo(MidiChannel.ENCODER, cc)
        val button = MidiInfo(MidiChannel.BUTTON, cc)
        val rgbLight = LightMidiInfo(MidiChannel.BUTTON, MidiChannel.RGB_ANIMATION, cc)
        val ringLight = LightMidiInfo(MidiChannel.ENCODER, MidiChannel.RING_ANIMATION, cc)
        val knobInfo = KnobMidiInfo(encoder, button, rgbLight, ringLight)

        hardwareKnobs(knob) = TwisterKnob(extension, knobInfo)

      for button <- 0 until hardwareLSBs.length do
        val cc = sideButtonsFirstLeftCC + button + sideButtonsBankOffset
        val midiInfo = MidiInfo(MidiChannel.SIDE_BUTTON, cc)

        hardwareLSBs(button) = TwisterButton(extension, midiInfo, "Side")

      for button <- 0 until hardwareRSBs.length do
        val cc = sideButtonsFirstRightCC + button + sideButtonsBankOffset
        val midiInfo = MidiInfo(MidiChannel.SIDE_BUTTON, cc)

        hardwareRSBs(button) = TwisterButton(extension, midiInfo, "Side")

  /**
   * Create a listener for bank switch messages.
   *
   * Updates the internal active bank index and shows a popup notification if popups are enabled.
   */
  private def createBankSwitchListener(midiIn: MidiIn): Unit =
    midiIn.setMidiCallback((status, data1, data2) =>
      // Filter everything except bank change messages
      if status != 0xB3 || data1 > 3 || data2 != 0x7F then return

      if activeBank != data1 then
        activeBank = data1
        showBankChangePopup(data1)
    )

  /** Creates a note input for the sequencer channel on the Twister. */
  private def createSequencerInput(midiIn: MidiIn): Unit =
    midiIn.createNoteInput("Sequencer", "?7????")

  /** Shows a bank change popup notification if notifications are enabled. */
  private def showBankChangePopup(index: Int): Unit =
    if popupEnabled then
      host.showPopupNotification(s"Twister Bank ${index + 1}")

object Twister:
  val NUM_BANKS: Int = 4

  /** Twister MIDI channels. Zero indexed. */
  object MidiChannel:
    val ENCODER: Int = 0
    val BUTTON: Int = 1
    val RGB_ANIMATION: Int = 2
    val SIDE_BUTTON: Int = 3
    val SYSTEM: Int = 3
    val SHIFT: Int = 4
    val RING_ANIMATION: Int = 5
    val SEQUENCER: Int = 7

  /** A single bank of Twister hardware. */
  class Bank:
    import Bank._
    val knobs: Array[TwisterKnob] = Array.ofDim[TwisterKnob](NUM_KNOBS)
    val leftSideButtons: Array[TwisterButton] = Array.ofDim[TwisterButton](NUM_LEFT_SIDE_BUTTONS)
    val rightSideButtons: Array[TwisterButton] = Array.ofDim[TwisterButton](NUM_RIGHT_SIDE_BUTTONS)

  object Bank:
    val NUM_KNOBS: Int = 16
    val NUM_LEFT_SIDE_BUTTONS: Int = 3
    val NUM_RIGHT_SIDE_BUTTONS: Int = 3
    val NUM_SIDE_BUTTONS: Int = NUM_LEFT_SIDE_BUTTONS + NUM_RIGHT_SIDE_BUTTONS
