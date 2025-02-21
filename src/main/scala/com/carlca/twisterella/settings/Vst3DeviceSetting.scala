package com.carlca.twisterella.settings

import com.bitwig.extension.controller.api.{Device, Parameter}
import org.tomlj.TomlTable
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import com.carlca.twisterella.settings.AbstractDeviceSetting

/**
 * VST3 specific device settings.
 *
 * Contains an ID as well as any parameter IDs that were set during construction.
 */
class Vst3DeviceSetting private (id: String, params: mutable.Map[String, Integer]) extends AbstractDeviceSetting[String, Integer](id, params):

  override def createParameter(device: Device, key: String): Parameter =
    device.createSpecificVst3Device(id).createParameter(params(key))

object Vst3DeviceSetting:
  /**
   * Constructs a Vst3DeviceSetting object from a TOML table.
   *
   * @param table The TOML table from which to create the object.
   * @return A new Vst3DeviceSetting. Throws on errors.
   */
  def fromToml(table: TomlTable): Vst3DeviceSetting =
    val id = table.getString("id")

    // All VST3 IDs seem to be a 32 character hex strings
    if id.length != 32 then
      throw IllegalArgumentException("VST3 device ID must be 32 characters long")

    val params = mutable.Map[String, Integer]()
    val tomlParams = table.getTable("params")

    for key <- tomlParams.keySet.asScala do
      val paramId = java.lang.Math.toIntExact(tomlParams.getLong(key))

      if paramId < 0 then
        throw IllegalArgumentException("Parameter ID must not be negative")

      params += (key -> paramId)

    new Vst3DeviceSetting(id, params)
