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

import java.io.File
import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, RemovalNotification, RemovalListener, Cache}
import org.locationtech.geomesa.accumulo.TypeSchema
import org.locationtech.geomesa.web.scalatra.User

object CSVUploadCache {
  case class RecordTag(userId: Option[String], csvId: String)
  case class Record(csvFile: File, hasHeader: Boolean, schema: TypeSchema)
}

import CSVUploadCache._

class CSVUploadCache {

  val records: Cache[RecordTag, Record] = {
    val removalListener = new RemovalListener[RecordTag, Record]() {
      override def onRemoval(notification: RemovalNotification[RecordTag, Record]) =
        cleanup(notification.getKey, notification.getValue)
    }
    CacheBuilder.newBuilder()
      .expireAfterAccess(1, TimeUnit.HOURS)
      .removalListener(removalListener)
      .build()
  }

  private def cleanup(tag: RecordTag, record: Record) {
    record.csvFile.delete()
    records.invalidate(tag)
  }

  def store(tag: RecordTag, record: Record) { records.put(tag, record) }
  def load(tag: RecordTag) = records.getIfPresent(tag)
  def clear(tag: RecordTag) { for {record <- Option(load(tag))} cleanup(tag, record) }
}
