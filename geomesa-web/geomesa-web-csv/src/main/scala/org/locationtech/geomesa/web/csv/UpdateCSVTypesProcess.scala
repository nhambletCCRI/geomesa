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

package org.locationtech.geomesa.web.csv

import org.geotools.process.factory.{DescribeParameter, DescribeResult, DescribeProcess}
import org.locationtech.geomesa.accumulo.TypeSchema

@DescribeProcess(
  title = "Update CSV Types",
  description = "Update type schema associated with an uploaded CSV file"
)
class UpdateCSVTypesProcess(csvUploadCache: CSVUploadCache)
  extends GeomesaCSVProcess(csvUploadCache) {

  @DescribeResult(name = "updated", description = "True if the CSV was found and the schema updated")
  def execute(
               @DescribeParameter(
                 name = "csvId",
                 description = "The temporary ID of the uploaded CSV file to update")
               csvId: String,

               @DescribeParameter(
                 name = "name",
                 min = 0,
                 max = 1,
                 description = "The new layer name, if any, to be applied to the CSV data")
               name: String,

               @DescribeParameter(
                 name = "schema",
                 min = 0,
                 max = 1,
                 description = "The new schema, if any, to be applied to the CSV data")
               schema: String,

               @DescribeParameter(
                 name = "latField",
                 min = 0,
                 max = 1,
                 description = "The new latitude field name, if any, to be applied to the CSV data")
               latField: String,

               @DescribeParameter(
                 name = "lonField",
                 min = 0,
                 max = 1,
                 description = "The new longitude field name, if any, to be applied to the CSV data")
               lonField: String
              ) = {
    val tag = getTag(csvId)
    Option(csvUploadCache.load(tag)) match {
      case None         => false
      case Some(record) =>
        val newName   = Option(name).getOrElse(record.schema.name)
        val newSchema = Option(schema).getOrElse(record.schema.schema)
        val newLLF    = for (latf <- Option(latField); lonf <- Option(lonField)) yield (latf, lonf)
        val newTS     = TypeSchema(newName, newSchema, newLLF)
        csvUploadCache.store(tag, record.copy(schema = newTS))
        true
    }
  }
}
