package com.dcmn.spark.resource 

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import java.io._
import org.apache.commons.io.FileUtils

object SparkJob {
    /* This is main scala program   
   * This will execute the Spark Job to import Dynamodb data to Parquet File
   */
  def main(args: Array[String]) {
     println("Defining the Schema")
     // Define The Schema of Dataframe 
     val schema = StructType(
      Seq(
        StructField("adnetwork_name", StringType),
        StructField("network_id", StringType),
        StructField("campaign_id", StringType),
        StructField("publisher_id", StringType),
        StructField("post_url", StringType),
        StructField("tags", StringType),
        StructField("transaction_id", StringType)
        )
      ) 
      
     /** Add Database and Table details to Scala Options */
     val options = Map(
      "tableName" -> "prod1.ad-network.configuration",
      "region" -> "eu-west-1",
      "scanEntireTable" -> "true"
      )
      /** Create SparkContext and SQLContext  */
     val conf = new SparkConf().setAppName("Spark-DynamoDB Application")
     val sc = new SparkContext(conf)
     val sqlContext = new org.apache.spark.sql.SQLContext(sc)
     val df = sqlContext.read.format("com.dcmn.spark.dynamodb").
     schema(schema).options(options).load()
     /** Show the DataFrame  */
     df.show
      /** Print the DataFrame Schema */
     df.printSchema
    
      /** Delete or Clean any existing files */
      FileUtils.deleteQuietly(new File("adnetworks.parquet"))
       /** Write Dataframe into a Parquet */
       df.write.parquet("adnetworks.parquet")
    
       val adnetworksFile = sqlContext.read.parquet("adnetworks.parquet") //Read Parquet

       //Parquet files can also be registered as tables and then used in SQL statements.
       adnetworksFile.registerTempTable("adnetworksFile")
       val networks = sqlContext.sql("SELECT adnetwork_name FROM adnetworksFile")
       networks.map(t => "Name: " + t(0)).collect().foreach(println)   //Print the result
     }
}