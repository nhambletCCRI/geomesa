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
import org.locationtech.geomesa.web.csv.CSVUploadCache.Record

@DescribeProcess(
  title = "Check CSV Types",
  description = "Check type schema associated with an uploaded CSV file"
)
class CheckCSVTypesProcess(csvUploadCache: CSVUploadCache)
  extends GeomesaCSVProcess(csvUploadCache) {

  @DescribeResult(name = "typeSchema", description = "Name and schema associated to the CSV file")
  def execute(
               @DescribeParameter(
                 name = "csvId",
                 description = "The temporary ID of the uploaded CSV file to check")
               csvId: String
              ) = {
    val tag = getTag(csvId)
    Option(csvUploadCache.load(tag)) match {
      case None                                            => ""
      case Some(Record(_, _, TypeSchema(name, schema, _))) => s"$name\n$schema"
    }
  }
}
