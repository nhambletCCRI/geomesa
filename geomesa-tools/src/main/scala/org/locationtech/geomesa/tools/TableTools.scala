/*
 * Copyright 2014 Commonwealth Computer Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.locationtech.geomesa.tools

import java.util.Map.Entry
import java.util.UUID

import com.typesafe.scalalogging.slf4j.Logging
import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException
import org.apache.accumulo.core.client.{TableNotFoundException, ZooKeeperInstance}
import org.apache.hadoop.fs.Path
import org.geotools.data._
import org.locationtech.geomesa.core.data.AccumuloDataStore

import scala.collection.JavaConversions._
import scala.util.Try
import scala.xml.XML

class TableTools(config: ScoptArguments, password: String) extends Logging {
  val accumuloConf = XML.loadFile(s"${System.getenv("ACCUMULO_HOME")}/conf/accumulo-site.xml")
  val zookeepers = (accumuloConf \\ "property")
    .filter(x => (x \ "name")
    .text == "instance.zookeeper.host")
    .map(y => (y \ "value").text)
    .head
  val instanceDfsDir = Try((accumuloConf \\ "property")
    .filter(x => (x \ "name")
    .text == "instance.dfs.dir")
    .map(y => (y \ "value").text)
    .head)
    .getOrElse("/accumulo")
  val instanceIdDir = new Path(instanceDfsDir, "instance_id")
  val instanceIdStr = ZooKeeperInstance.getInstanceIDFromHdfs(instanceIdDir)
  val instanceName = new ZooKeeperInstance(UUID.fromString(instanceIdStr), zookeepers).getInstanceName
  val ds: AccumuloDataStore = Try({
    DataStoreFinder.getDataStore(Map(
      "instanceId" -> instanceName,
      "zookeepers" -> zookeepers,
      "user"       -> config.username,
      "password"   -> password,
      "tableName"  -> config.catalog)).asInstanceOf[AccumuloDataStore]
  }).getOrElse{
    logger.error("Incorrect username or password. Please try again.")
    sys.exit()
  }

  val tableOps = ds.connector.tableOperations()
  val tableName = config.suffix match {
    case "st_idx" => ds.getSpatioTemporalIdxTableName(config.featureName)
    case "attr_idx" => ds.getAttrIdxTableName(config.featureName)
    case "records" => ds.getRecordTableForType(config.featureName)
    case _ =>
      logger.error("Incorrect table suffix. Please check that all arguments are correct and try again.")
      sys.exit()
  }
  def listConfig(): Unit = {
    logger.info(s"Gathering the configuration parameters for $tableName. Just a few moments...")
    try {
      val properties = tableOps.getProperties(tableName)
      properties.foreach(prop => logger.info(s"$prop"))
    } catch {
      case tnfe: TableNotFoundException => logger.error(s"Error: table $tableName could not be found.")
      case e: Exception => logger.error(s"Error listing properties for $tableName.")
    }
  }

  def getProperty: Entry[String, String] = {
    try {
      tableOps.getProperties(tableName).find(_.getKey == config.param).getOrElse({
        logger.error(s"Parameter '${config.param}' not found. Please ensure that all arguments from the " +
          s"previous command are correct, and try again.")
        sys.exit()
      })
    }  catch {
      case tnfe: TableNotFoundException =>
        logger.error(s"Error: table $tableName could not be found.")
        sys.exit()
      case e: Exception =>
        logger.error(s"Error listing properties for $tableName.")
        sys.exit()
    }
  }

  def describeConfig(): Unit = {
    logger.info(s"Finding the value for '${config.param}' on table '$tableName'. Just a few moments...")
    val property = getProperty
    logger.info(s"$property")
  }

  def updateConfig(): Unit = {
    val property = getProperty
    logger.info(s"'${config.param}' on table '$tableName' currently set to: \n$property")
    if (config.newValue != property.getValue) {
      logger.info(s"Now attempting to update to '${config.newValue}'...")
      try {
        tableOps.setProperty(tableName, config.param, config.newValue)
        val updatedProperty = getProperty
        logger.info(s"'${config.param}' on table '$tableName' is now set to: \n$updatedProperty")
      } catch {
        case ttoe: ThriftTableOperationException => "Error: there was a problem altering the table property."
        case e: Exception => "Error updating the table property."
      }
    } else {
      logger.info(s"'${config.param}' already set to '${config.newValue}'. No need to update.")
    }
  }
}
