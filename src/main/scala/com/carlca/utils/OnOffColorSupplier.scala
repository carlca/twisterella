package com.carlca.utils

import com.bitwig.extension.api.Color
import java.util.function.Supplier

/** A color supplier that can toggle between two color states, an "on" color and "off" color. */
class OnOffColorSupplier(onColorInit: Color, offColorInit: Color) extends Supplier[Color]:
  def this(onColor: Color) = this(onColor, Color.blackColor())
  def this() = this(Color.blackColor(), Color.blackColor())

  private var onColor: Color = onColorInit
  private var offColor: Color = offColorInit
  private var isOn: Boolean = false

  /**
   * Sets the color for the "on" state.
   *
   * @param onColor The desired color.
   */
  def setOnColor(onColor: Color): Unit =
    this.onColor = onColor

  /**
   * Sets the color for the "off" state.
   *
   * @param offColor The desired color.
   */
  def setOffColor(offColor: Color): Unit =
    this.offColor = offColor

  /**
   * Sets the state.
   *
   * @param on "on" if true, "off" if false.
   */
  def setOn(on: Boolean): Unit =
    this.isOn = on

  /** @return The current state color. */
  override def get(): Color =
    if isOn then onColor else offColor
