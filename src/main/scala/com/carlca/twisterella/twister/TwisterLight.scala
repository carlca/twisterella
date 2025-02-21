package com.carlca.twisterella.twister

import com.bitwig.extension.controller.api.MidiOut
import com.carlca.twisterella.TwisterellaExtension
import com.carlca.utils.MathUtil
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** Twister light base class. */
abstract class TwisterLight(extension: TwisterellaExtension, animationMidiInfo: MidiInfo, animationStartValue: Int, brightnessStartValue: Int):
  import TwisterLight.AnimationState

  protected val midiOut: MidiOut = extension.midiOut

  private val midiInfo: MidiInfo = animationMidiInfo
  private val animationMap: Map[AnimationState, Int] = TwisterLight.createAnimationMap(animationStartValue)
  private val _brightnessStartValue: Int = brightnessStartValue

  /**
   * Sets the desired animation state.
   *
   * @param state Animation state.
   */
  def setAnimationState(state: AnimationState): Unit =
    assert(animationMap.contains(state), "Invalid state")
    sendAnimation(animationMap(state))

  /**
   * Overrides the brightness setting with a new brightness value.
   *
   * @param brightness The desired brightness.
   */
  def overrideBrightness(brightness: Double): Unit =
    val value = MathUtil.clamp(brightness, 0.0, 1.0)
    sendAnimation(((value * 30.0) + _brightnessStartValue).toInt)

  /** Resets the brightness override. */
  def resetBrightness(): Unit =
    sendAnimation(0)

  /** Turns the light off and resets all animations. */
  def lightOff(): Unit

  /**
   * Sends the raw animation MIDI value to the device.
   *
   * @param value The raw MIDI value.
   */
  private def sendAnimation(value: Int): Unit =
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, value)

object TwisterLight:

  /** The available animation states of the light. */
  enum AnimationState(val optionString: String):
    case OFF extends AnimationState("Off") // animation off
    case STROBE_8_1 extends AnimationState("Strobe 8/1") //
    case STROBE_4_1 extends AnimationState("Strobe 4/1") //
    case STROBE_2_1 extends AnimationState("Strobe 2/1") //
    case STROBE_1_1 extends AnimationState("Strobe 1/1") //
    case STROBE_1_2 extends AnimationState("Strobe 1/2") //
    case STROBE_1_4 extends AnimationState("Strobe 1/4") //
    case STROBE_1_8 extends AnimationState("Strobe 1/8") //
    case STROBE_1_16 extends AnimationState("Strobe 1/16") //
    case PULSE_8_1 extends AnimationState("Pulse 8/1") //
    case PULSE_4_1 extends AnimationState("Pulse 4/1") //
    case PULSE_2_1 extends AnimationState("Pulse 2/1") //
    case PULSE_1_1 extends AnimationState("Pulse 1/1") //
    case PULSE_1_2 extends AnimationState("Pulse 1/2") //
    case PULSE_1_4 extends AnimationState("Pulse 1/4") //
    case PULSE_1_8 extends AnimationState("Pulse 1/8") //
    case PULSE_1_16 extends AnimationState("Pulse 1/16") //
    case RAINBOW extends AnimationState("Rainbow")

  object AnimationState:
    private val optionMap: Map[String, AnimationState] = values.map(state => (state.optionString, state)).toMap

    /**
     * Gets the enum constant with the specific option string.
     *
     * @param optionString The animation state's option string.
     * @return The enum constant with the specific option string.
     * @throws IllegalArgumentException If the option string is invalid.
     */
    def valueOfOptionString(optionString: String): AnimationState =
      optionMap.getOrElse(optionString, throw new IllegalArgumentException("Unknown AnimationState option"))

    /**
     * @return An array of all the option strings for this enum.
     */
    def optionStrings(): Array[String] =
      values.map(_.optionString).toArray

  /**
   * Generates the animation value map based on the start value.
   *
   * The ring light and RGB light have different starting MIDI values for the animations. By
   * providing the starting offset value, the sub class will create the correct value map for
   * itself.
   *
   * @param startValue Starting value of the animation range.
   * @return A populated animation value map.
   */
  private def createAnimationMap(startValue: Int): Map[AnimationState, Int] =
    val map = mutable.Map[AnimationState, Int]()

    map += (AnimationState.OFF -> 0)
    map += (AnimationState.RAINBOW -> 127)

    var i = startValue
    AnimationState.values.foreach: state =>
      if state != AnimationState.OFF && state != AnimationState.RAINBOW then
        map += (state -> i)
        i += 1
    map.toMap
