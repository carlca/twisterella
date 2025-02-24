package com.carlca.twisterella.settings

import com.bitwig.extension.controller.api.{Device, Parameter}
import org.tomlj.TomlTable
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** VST2 specific device settings.
  *
  * Contains an ID as well as any parameter IDs that were set during
  * construction.
  */
class Vst2DeviceSetting private (
    id: Integer,
    params: mutable.Map[String, Integer]
) extends AbstractDeviceSetting[Integer, Integer](id, params):

  override def createParameter(device: Device, key: String): Parameter =
    device.createSpecificVst2Device(id).createParameter(params(key))

object Vst2DeviceSetting:
  /** Constructs a Vst2DeviceSetting object from a TOML table.
    *
    * @param table
    *   The TOML table from which to create the object.
    * @return
    *   A new Vst2DeviceSetting. Throws on errors.
    */
  def fromToml(table: TomlTable): Vst2DeviceSetting =
    val id = java.lang.Math.toIntExact(table.getLong("id"))

    // All VST2 IDs seem to be positive integers
    if id < 0 then
      throw IllegalArgumentException("VST2 device ID must not be negative")

    val params = mutable.Map[String, Integer]()
    val tomlParams = table.getTable("params")

    for key <- tomlParams.keySet.asScala do
      val paramId = java.lang.Math.toIntExact(tomlParams.getLong(key))

      if paramId < 0 then
        throw IllegalArgumentException("Parameter ID must not be negative")

      params += (key -> paramId)

    new Vst2DeviceSetting(id, params)
