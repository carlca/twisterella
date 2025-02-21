package com.carlca.twisterella.twister

import com.bitwig.extension.api.Color
import com.bitwig.extension.controller.api.{DoubleValue, HardwareBindable, RelativeHardwareControlBinding, RelativeHardwareKnob}
import com.carlca.twisterella.TwisterellaExtension

/** A twister knob, including encoder, shift encoder, button and lights. */
class TwisterKnob(extension: TwisterellaExtension, midiInfo: KnobMidiInfo, color: Color):
  import TwisterKnob._

  def this(extension: TwisterellaExtension, midiInfo: KnobMidiInfo) = this(extension, midiInfo, Color.nullColor())

  private val knob: RelativeHardwareKnob = extension.hardwareSurface.createRelativeHardwareKnob(s"Knob ${midiInfo.encoder.cc}")
  knob.setAdjustValueMatcher(extension.midiIn.createRelativeBinOffsetCCValueMatcher(midiInfo.encoder.channel, midiInfo.encoder.cc, FULL_ROTATION))

  private val _button: TwisterButton = TwisterButton(extension, midiInfo.button, "Knob")
  private val _rgbLight: TwisterRGBLight = TwisterRGBLight(extension, midiInfo.rgbLight, color)
  private val _ringLight: TwisterRingLight = TwisterRingLight(extension, midiInfo.ringLight)

  private val shiftChannel = Twister.MidiChannel.SHIFT

  private val shiftKnob: RelativeHardwareKnob = extension.hardwareSurface.createRelativeHardwareKnob(s"Shift Knob ${midiInfo.encoder.cc}")
  shiftKnob.setAdjustValueMatcher(extension.midiIn.createRelativeBinOffsetCCValueMatcher(shiftChannel, midiInfo.encoder.cc, FULL_ROTATION))
  private val _shiftRingLight: TwisterRingLight = TwisterRingLight(extension, LightMidiInfo(shiftChannel, midiInfo.ringLight.animation.channel, midiInfo.encoder.cc))

  private var isFineSensitivity: Boolean = false
  private var fineSensitivity: Double = 0.25
  private var sensitivity: Double = 1.0

  /** The associated button for this knob. */
  def button(): TwisterButton = _button

  /** The associated RGB light for this knob. */
  def rgbLight(): TwisterRGBLight = _rgbLight

  /** The associated ring light for this knob. */
  def ringLight(): TwisterRingLight = _ringLight

  /** The associated shift ring light for this knob. */
  def shiftRingLight(): TwisterRingLight = _shiftRingLight

  /** The value of the target that this knob has been bound to (0-1). */
  def targetValue(): DoubleValue = knob.targetValue()

  /** The value of the target that this shift knob has been bound to (0-1). */
  def shiftTargetValue(): DoubleValue = shiftKnob.targetValue()

  /**
   * Sets the regular sensitivity factor for the knob. If regular sensitivity is active it is
   * applied immediately.
   *
   * @param factor The sensitivity factor to apply.
   */
  def setSensitivity(factor: Double): Unit =
    sensitivity = factor
    if !isFineSensitivity then
      knob.setSensitivity(sensitivity)

  /**
   * Sets the fine sensitivity factor for the knob. If fine sensitivity is active it is applied
   * immediately.
   *
   * @param factor The sensitivity factor to apply.
   */
  def setFineSensitivity(factor: Double): Unit =
    fineSensitivity = factor
    if isFineSensitivity then
      knob.setSensitivity(fineSensitivity)

  /** Toggles between regular and fine sensitivity. */
  def toggleSensitivity(): Unit =
    isFineSensitivity = !isFineSensitivity
    knob.setSensitivity(if isFineSensitivity then fineSensitivity else sensitivity)

  /**
   * Binds the knob to the supplied target.
   *
   * @param target Target to bind.
   * @return The created binding.
   */
  def setBinding(target: HardwareBindable): RelativeHardwareControlBinding =
    knob.setBinding(target)

  /**
   * Binds the shift knob to the supplied target.
   *
   * @param target Target to bind.
   * @return The created binding.
   */
  def setShiftBinding(target: HardwareBindable): RelativeHardwareControlBinding =
    shiftKnob.setBinding(target)

  /** Resets all animations and turns off all lights. */
  def lightsOff(): Unit =
    ringLight.lightOff()
    shiftRingLight.lightOff()
    rgbLight.lightOff()

object TwisterKnob:
  private val FULL_ROTATION: Int = 127
