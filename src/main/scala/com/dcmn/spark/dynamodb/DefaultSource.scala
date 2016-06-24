package com.dcmn.spark.dynamodb

import org.apache.spark.sql.{ DataFrame, SaveMode, SQLContext }
import org.apache.spark.sql.sources.{
  BaseRelation, SchemaRelationProvider, RelationProvider, CreatableRelationProvider }
import org.apache.spark.sql.types.StructType

/** Default Class establishing connection with specific DynamoDB tables and region
  */
class DefaultSource extends RelationProvider
  with SchemaRelationProvider
  with CreatableRelationProvider {

  override def createRelation(
      sqlContext: SQLContext,
      parameters: Map[String, String]): BaseRelation = {
    createRelation(sqlContext, parameters)
  }
  override def createRelation(
      sqlContext: SQLContext,
      parameters: Map[String, String],
      schema: StructType): BaseRelation = {
    val tableName = parameters.getOrElse("tableName", sys.error("'tableName must be specified"))
    val region = parameters.getOrElse("region", sys.error("'region must be specified"))
    val scanEntireTable = parameters.getOrElse("scanEntireTable", "true")

    DynamoDBRelation(tableName, region, Option(schema), scanEntireTable.toBoolean)(sqlContext)
  }

  override def createRelation(
      sqlContext: SQLContext,
      mode: SaveMode, parameters: Map[String, String],
      dataFrame: DataFrame) = {
    createRelation(sqlContext, parameters, dataFrame.schema)
  }

}
