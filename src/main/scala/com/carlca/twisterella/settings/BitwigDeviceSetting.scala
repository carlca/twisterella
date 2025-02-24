package com.carlca.twisterella.settings

import com.bitwig.extension.controller.api.Device
import com.bitwig.extension.controller.api.Parameter
import org.tomlj.TomlTable

import java.util.UUID
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/** Bitwig specific device settings.
  *
  * Contains an ID as well as any parameter IDs that were set during
  * construction.
  */
class BitwigDeviceSetting private (
    id: UUID,
    params: mutable.Map[String, String]
) extends AbstractDeviceSetting[UUID, String](id, params):

  override def createParameter(device: Device, key: String): Parameter =
    device.createSpecificBitwigDevice(id).createParameter(params(key))

object BitwigDeviceSetting:
  /** Constructs a BitwigDeviceSetting object from a TOML table.
    *
    * @param table
    *   The TOML table from which to create the object.
    * @return
    *   A new BitwigDeviceSetting. Throws on errors.
    */
  def fromToml(table: TomlTable): BitwigDeviceSetting =
    val id = UUID.fromString(table.getString("id"))

    val params = mutable.Map[String, String]()
    val tomlParams = table.getTable("params")

    for key <- tomlParams.keySet.asScala do
      val paramId = tomlParams.getString(key)

      if paramId == null || paramId.isEmpty then
        throw IllegalArgumentException("Parameter ID must not be null or empty")

      params += (key -> paramId)

    new BitwigDeviceSetting(id, params)
