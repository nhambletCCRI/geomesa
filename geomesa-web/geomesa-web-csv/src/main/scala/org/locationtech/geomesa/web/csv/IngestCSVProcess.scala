package org.locationtech.geomesa.web.csv

import java.{util => ju}

import org.geotools.process.factory.{DescribeParameter, DescribeProcess}
import org.locationtech.geomesa.accumulo.csv
import org.locationtech.geomesa.process.ImportProcess
import org.locationtech.geomesa.web.csv.CSVUploadCache.Record

@DescribeProcess(
  title = "Ingest CSV data",
  description = "Ingest the data contained in an uploaded CSV file"
)
class IngestCSVProcess(csvUploadCache: CSVUploadCache, importer: ImportProcess)
  extends GeomesaCSVProcess(csvUploadCache) {

  def execute(
               @DescribeParameter(
                 name = "csvId",
                 description = "The temporary ID of the CSV file to ingest")
               csvId: String,

               @DescribeParameter(
                 name = "workspace",
                 description = "Target workspace")
               workspace: String,

               @DescribeParameter(
                 name = "store",
                 description = "Target store")
               store: String,

               @DescribeParameter(
                 name = "keywords",
                 min = 0,
                 collectionType = classOf[String],
                 description = "List of (comma-separated) keywords for layer")
               keywordStrs: ju.List[String],

               @DescribeParameter(
                 name = "numShards",
                 min = 0,
                 max= 1,
                 description = "Number of shards to store for this table (defaults to 4)")
               numShards: Integer,

               @DescribeParameter(
                 name = "securityLevel",
                 min = 0,
                 max = 1,
                 description = "The level of security to apply to this import")
               securityLevel: String
              ) = {
    def ingest(record: Record) = {
      val fc = csv.csvToFeatures(record.csvFile, record.hasHeader, record.schema)
      val name = record.schema.name
      importer.execute(fc, workspace, store, name, keywordStrs, numShards, securityLevel)
    }

    val tag = getTag(csvId)
    Option(csvUploadCache.load(tag)) match {
      case None => false
      case Some(record) => ingest(record)

    }
  }
}
