package com.carlca.twisterella.settings

import java.nio.file.{Path, Paths}
import java.text.ParseException
import java.util.function.Function
import org.tomlj.{Toml, TomlArray, TomlParseError, TomlParseResult, TomlTable}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import com.carlca.twisterella.settings.BitwigDeviceSetting
import com.carlca.twisterella.settings.Vst3DeviceSetting
import com.carlca.twisterella.settings.Vst2DeviceSetting
import com.carlca.twisterella.settings.AbstractDeviceSetting

/**
 * Represents data loaded from a specific devices settings file.
 *
 * This data can be used to programmatically setup specific devices using the specific device
 * portion of the extension API. Since the API provides no way to query device and param IDs, this
 * allows for more flexibility than a hardcoded approach to specific device setup.
 *
 * The TOML file is required to a have a specific structure.
 *
 * A table called "controls". This table can have any number of string array keys. These string
 * arrays are loaded into the controlMap. This table is optional.
 *
 * Three table arrays, "bitwig", "vst3" and "vst2". These represent all the device settings. These
 * arrays are optional.
 *
 * Each table requires an id key that is equal to the device ID taken from Bitwig. For Bitwig and
 * VST3 devices it is a string, for VST2 it is an integer.
 *
 * Each table has a sub table called "params". This table contains any number of keys that are equal
 * to parameter IDs taken from Bitwig. Bitwig device parameters are strings, VST3 and VST2 device
 * parameters are integers.
 *
 * Example:
 *
 * <pre>
 * [controls]
 * knob1 = ["mix"]
 * knob2 = ["output_gain", "decay_time"]
 *
 * [[bitwig]]
 * id = "b5b2b08e-730e-4192-be71-f572ceb5069b"
 * params.mix = "MIX"
 * params.output_gain = "LEVEL"
 *
 * [[vst3]]
 * id = "5653547665653376616C68616C6C6176"
 * params.mix = 48
 * params.decay_time = 50
 *
 * [[vst2]]
 * id = 1315513406
 * params.mix = 11
 * </pre>
 *
 * These table arrays end up as the bitwigDevices, vst3Devices and vst2Devices lists.
 */
class SpecificDeviceSettings(settingsPath: Path):
  private val _controlMap: Map[String, Set[String]] = loadControls()
  private val _bitwigDevices: List[BitwigDeviceSetting] = loadDevices("bitwig", BitwigDeviceSetting.fromToml)
  private val _vst3Devices: List[Vst3DeviceSetting] = loadDevices("vst3", Vst3DeviceSetting.fromToml)
  private val _vst2Devices: List[Vst2DeviceSetting] = loadDevices("vst2", Vst2DeviceSetting.fromToml)

  def this(settingsPathString: String) = this(Paths.get(settingsPathString))

  /**
   * @return Map of controls with parameter keys.
   */
  def controlMap(): Map[String, Set[String]] = _controlMap

  /**
   * @return List of Bitwig devices.
   */
  def bitwigDevices(): List[BitwigDeviceSetting] = _bitwigDevices

  /**
   * @return List of VST3 devices.
   */
  def vst3Devices(): List[Vst3DeviceSetting] = _vst3Devices

  /**
   * @return List of VST2 devices.
   */
  def vst2Devices(): List[Vst2DeviceSetting] = _vst2Devices

  private def loadTomlResult(): TomlParseResult =
    Try(Toml.parse(settingsPath)) match
      case Success(toml) => toml
      case Failure(exception) => throw new RuntimeException(exception)

  /**
   * Loads all the controls and their device parameter key arrays from the controls table.
   *
   * @return A set of control keys mapped to lists of device parameter keys.
   */
  private def loadControls(): Map[String, Set[String]] =
    val result = loadTomlResult()
    val controlsTable = result.getTable("controls")

    if controlsTable == null then
      return Map.empty[String, Set[String]]

    val output = mutable.Map[String, Set[String]]()

    for key <- controlsTable.keySet.asScala do
      val controlParams = controlsTable.getArray(key)

      if controlParams == null then
        ()

      val params = mutable.Set[String]()

      for i <- 0 until controlParams.size() do
        val param = controlParams.getString(i)

        if param == null then
          ()

        params += param

      output += (key -> params.toSet)

    output.toMap

  /**
   * Loads all the devices of a specific type.
   *
   * @tparam IdType The type of the device ID.
   * @tparam SettingType The settings type for the device.
   * @param key The key of the array of device tables.
   * @param deviceBuilder A function that will construct the device setting from a TOML table.
   * @return A list of valid device settings.
   */
  private def loadDevices[IdType, SettingType <: AbstractDeviceSetting[IdType, ?]](key: String, deviceBuilder: TomlTable => SettingType): List[SettingType] =
    val result = loadTomlResult()
    val devices = result.getArray(key)

    if devices == null then
      return List.empty[SettingType]

    val settings = mutable.Map[IdType, SettingType]()

    for idx <- 0 until devices.size() do
      val device = deviceBuilder(devices.getTable(idx))
      val id = device.id.asInstanceOf[IdType]

      if settings.contains(id) then
        throw RuntimeException(s"Duplicate $key device ID found: $id")

      settings += (id -> device)

    settings.values.toList
