package org.locationtech.geomesa.web.csv

import org.geotools.process.factory.AnnotatedBeanProcessFactory
import org.geotools.text.Text

class ProcessFactory
  extends AnnotatedBeanProcessFactory(
    Text.text("GeoMesa CSV Process Factory"),
    "gmcsv",
    classOf[IngestCSVProcess]
  )
