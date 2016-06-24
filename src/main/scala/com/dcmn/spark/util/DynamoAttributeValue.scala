package com.dcmn.spark.util


import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.apache.spark.sql.types._
import scala.collection.JavaConverters._

/** Class for getting DynamoDB attributes Types 
  * and converting into different types
  */
object DynamoAttributeValue {

  def notNull[T]: T => Boolean = _ != null
 
  def getType(x: Any, value: String): Any = x match {
    case(x: DoubleType) => value.toDouble
    case(x: FloatType) => value.toFloat
    case(x: LongType) => value.toLong
    case(x: ByteType) => value.toByte
    case(x: ShortType) => value.toShort
    case(x: StringType) => value
    case(x: IntegerType) => value.toInt
  }

  def getType(x: DataType, values: List[Any]): Any = x  match {
      case(x: ArrayType) => values.map { i =>
        if (i.isInstanceOf[String]) { getType(x.elementType, i.toString) }
        else if (i.isInstanceOf[AttributeValue]) { convert(i.asInstanceOf[AttributeValue], x.elementType)}
      }
  }

  def getType(x: DataType, value: Map[String, AttributeValue] ): Any = x match {
     case(x: MapType) => value.map(item => (getType(x.keyType, item._1.toString), convert(item._2, x.valueType)))
  }

  def convert(attributeValue: AttributeValue, dataType: DataType): Option[Any] = {
    if (notNull(attributeValue)) {
      if (notNull(attributeValue.getS)) { Option(getType(dataType, attributeValue.getS)) }
      else if (notNull(attributeValue.getN)) { Option(getType(dataType, attributeValue.getN)) }
      else if (notNull(attributeValue.getB)) { Option(attributeValue.getB) }
      else if (notNull(attributeValue.getBOOL)) {Option(attributeValue.getBOOL) }
      else if (notNull(attributeValue.getNS)) { Option(getType(dataType, attributeValue.getNS.asScala.toList)) }
      else if (notNull(attributeValue.getSS)) { Option(getType(dataType, attributeValue.getSS.asScala.toList)) }
      else if (notNull(attributeValue.getBS)) { Option(attributeValue.getBS)}
      else if (notNull(attributeValue.getM)) { Option(getType(dataType, attributeValue.getM.asScala.toMap)) }
      else if (notNull(attributeValue.getL)) { Option(getType(dataType, attributeValue.getL.asScala.toList)) }
      else { None }
    } else { None }
  }
}
