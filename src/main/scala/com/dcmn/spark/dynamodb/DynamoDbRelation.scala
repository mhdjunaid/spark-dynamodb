package com.dcmn.spark.dynamodb

import com.amazonaws.ClientConfiguration
import com.google.common.util.concurrent.RateLimiter
import com.dcmn.spark.util.{ReservedWords, DynamoAttributeValue}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.sources.{TableScan, BaseRelation}
import org.apache.spark.sql.types._

import scala.collection.JavaConversions._
import scala.collection.mutable

/** Provides methods for establishing connection using credentials
  * Scans tables based on defaultsource
  * Builds RDD Dataset out of DynamoDB results
  */
case class DynamoDBRelation(tableName: String,
           region: String,
           providedSchema: Option[StructType] = None,
           scanEntireTable: Boolean = true)(@transient val providedSQLContext: SQLContext)
  extends BaseRelation with TableScan {

  @transient val clientConfig = new ClientConfiguration()
  @transient val credentials = new DefaultAWSCredentialsProviderChain()
  @transient val dynamoDbClient = Region.getRegion(Regions.fromName(region))
                                  .createClient(classOf[AmazonDynamoDBClient], credentials, clientConfig)

  val dynamoDbTable = dynamoDbClient.describeTable(tableName)
  val rateLimit = 25.0

  override def sqlContext: SQLContext = providedSQLContext

  override val schema: StructType = providedSchema match {
    case Some(struct: StructType) => struct
    case _ => StructType(getSchema(dynamoDbTable.getTable.getAttributeDefinitions))
  }

  private lazy val nameToField: Map[String, DataType] = schema.fields.map(f => f.name -> f.dataType).toMap

  val projectionExpression = {
    val expression = new StringBuilder()
    val expressionNames = mutable.Map[String, String]()
    schema.fieldNames.map { fieldName =>
      if (ReservedWords.reservedWords.contains(fieldName.toUpperCase)) {
        val key = s"#$fieldName"
        expression.append(key).append(",")
        expressionNames.put(key, fieldName)
      } else {
        expression.append(fieldName).append(",")
      }
    }
    if (expressionNames.nonEmpty) {
      (expression.toString().dropRight(1), Option(expressionNames))
    } else {
      (expression.toString().dropRight(1), None)
    }
  }

  override def buildScan: RDD[Row] = {
    var permitsToConsume = 1
    val rateLimiter = RateLimiter.create(rateLimit)

    rateLimiter.acquire(permitsToConsume)
    val scanRequest =
      new ScanRequest()
        .withTableName(tableName)
        .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        .withProjectionExpression(projectionExpression._1)
    var scanResult = dynamoDbClient.scan(scanRequest)
    val rowRDD = sqlContext.sparkContext.parallelize(scanResult.getItems)

    if (scanEntireTable) {
      permitsToConsume = (scanResult.getConsumedCapacity.getCapacityUnits - 1.0).toInt
      if (permitsToConsume <= 0) permitsToConsume = 1

      while (scanResult.getLastEvaluatedKey != null) {
        rateLimiter.acquire(permitsToConsume)
        scanRequest.setExclusiveStartKey(scanResult.getLastEvaluatedKey)
        scanResult = dynamoDbClient.scan(scanRequest)

        permitsToConsume = (scanResult.getConsumedCapacity.getCapacityUnits - 1.0).toInt

        if (permitsToConsume <= 0) permitsToConsume = 1
        rowRDD.union(sqlContext.sparkContext.parallelize(scanResult.getItems))
      }
    }
    rowRDD.map { result =>
      val values = schema.fieldNames.map { fieldName =>
        val data = nameToField.get(fieldName).get
        DynamoAttributeValue.convert(result.get(fieldName), data)
      }
      Row.fromSeq(values)
    }
  }

  private def getSchema(attributeDefinitions: Seq[AttributeDefinition]): Seq[StructField] = {
    attributeDefinitions.map { attributeDefinition =>
      attributeDefinition.getAttributeType match {
        case "S" => StructField(attributeDefinition.getAttributeName, StringType)
        case "N" => StructField(attributeDefinition.getAttributeName, LongType)
        case "B" => StructField(attributeDefinition.getAttributeName, BinaryType)
        case other => sys.error(s"Unsupported $other")
      }
    }
  }
}

