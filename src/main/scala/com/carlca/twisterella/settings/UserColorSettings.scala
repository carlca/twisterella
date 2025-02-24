package com.carlca.twisterella.settings

import scala.annotation.nowarn
import com.bitwig.extension.controller.api.{
  ControllerHost,
  DocumentState,
  RelativeHardwarControlBindable,
  SettableEnumValue,
  SettableRangedValue,
  Setting
}
import com.carlca.twisterella.twister.Twister
import com.carlca.utils.MathUtil
// import scala.jdk.CollectionConverters._

/** Color settings for the individual RGB lights in each user mappable bank.
  *
  * These settings can be accessed in the I/O panel. There is also a helper
  * function to create bindable targets for a setting so that it can be
  * controlled via hardware.
  */
class UserColorSettings(documentState: DocumentState):
  import UserColorSettings._

  private val options: List[String] = List("Hide", "2", "3", "4")
  private val settings: Array[Array[SettableRangedValue]] =
    Array.ofDim[SettableRangedValue](NUM_USER_BANKS, NUM_KNOBS_PER_BANK)
  @nowarn
  private val selector: SettableEnumValue =
    val strings = options.toArray[String]
    val sel =
      documentState.getEnumSetting("Bank", "Colors", strings, options(0))
    sel.addValueObserver((value: String) =>
      showBank(options.indexOf(value) - 1)
    )
    sel

  // Create all the individual settings
  for bank <- 0 until settings.length do
    val settingsBank = settings(bank)
    for idx <- 0 until settingsBank.length do
      val label = String.format("Color %02d%" + (bank + 1) + "s", idx + 1, " ")
      val colorSetting =
        documentState.getNumberSetting(label, "Colors", 0, 125, 1, null, 0)
      settingsBank(idx) = colorSetting
      (colorSetting.asInstanceOf[Setting]).hide()

  /** Gets a specific setting.
    *
    * @param colorBankIndex
    *   Bank index of the desired setting.
    * @param knobIndex
    *   Knob index of the desired setting.
    * @return
    *   The setting for the given bank and index.
    */
  def getSetting(colorBankIndex: Int, knobIndex: Int): SettableRangedValue =
    settings(colorBankIndex)(knobIndex)

  /** Hides all the settings from the UI panel. */
  private def hideAll(): Unit =
    for settingsBank <- settings do
      for colorSetting <- settingsBank do
        (colorSetting.asInstanceOf[Setting]).hide()

  /** Handles bank visibility */
  private def showBank(index: Int): Unit =
    hideAll()

    if index < 0 then return

    for colorSetting <- settings(index) do
      (colorSetting.asInstanceOf[Setting]).show()

object UserColorSettings:
  private val NUM_USER_BANKS: Int = 3
  private val NUM_KNOBS_PER_BANK: Int = Twister.Bank.NUM_KNOBS

  /** Creates a target to a color setting that is able to be bound to hardware.
    *
    * Despite being a SettableRangedValue, the settings are not compatible
    * targets and this proxy target must be created instead.
    *
    * @param host
    *   The controller host.
    * @param setting
    *   The setting to create a target for.
    * @return
    *   A bindable target to the setting.
    */
  def createTarget(
      host: ControllerHost,
      setting: SettableRangedValue
  ): RelativeHardwarControlBindable =
    host.createRelativeHardwareControlAdjustmentTarget((value: Double) =>
      val adjustedValue = MathUtil.clamp(setting.get() + value, 0.0, 1.0)
      setting.set(adjustedValue)
    )
