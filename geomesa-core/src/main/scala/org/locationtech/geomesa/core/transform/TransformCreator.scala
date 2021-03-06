package org.locationtech.geomesa.core.transform

import org.apache.accumulo.core.data.Value
import org.geotools.process.vector.TransformProcess
import org.locationtech.geomesa.core.data.{FeatureEncoding, SimpleFeatureEncoder}
import org.locationtech.geomesa.feature.{AvroSimpleFeature, AvroSimpleFeatureFactory}
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConversions._

object TransformCreator {

  def createTransform(targetFeatureType: SimpleFeatureType,
                      featureEncoder: SimpleFeatureEncoder,
                      transformString: String): (SimpleFeature => Array[Byte]) =
    FeatureEncoding.withName(featureEncoder.getName) match {
      case FeatureEncoding.AVRO =>
        val defs = TransformProcess.toDefinition(transformString)
        (feature: SimpleFeature) => {
          val newSf = new AvroSimpleFeature(feature.getIdentifier, targetFeatureType)
          defs.map { t => newSf.setAttribute(t.name, t.expression.evaluate(feature)) }
          featureEncoder.encode(newSf)
        }

      case FeatureEncoding.TEXT =>
        val defs = TransformProcess.toDefinition(transformString)
        val builder = AvroSimpleFeatureFactory.featureBuilder(targetFeatureType)
        (feature: SimpleFeature) => {
          builder.reset()
          defs.map { t => builder.set(t.name, t.expression.evaluate(feature)) }
          val newFeature = builder.buildFeature(feature.getID)
          featureEncoder.encode(newFeature)
        }
    }

}
