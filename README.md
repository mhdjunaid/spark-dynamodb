# DynamoDB to Parquet by Spark

## Introduction 
This sample project provides support for analyzing data on Amazon DynamoDB using Apache Spark and store as Parquet files . 
The `DynamoDBRelation` class extends `BaseRelation` with `TableScan` which returns RDD of Rows. The main class `SparkJob` runs the spark job to import data from DynamoDb and convert it into Parquet from Spark DataFrame.

## Requirements 
This project requires: 
- Spark 1.4.1
- Scala - 2.10.4
- AWS-Java-SDK - 1.10.11

## AWS Credentials
Configure credentials in the AWS Java SDK way.

http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html


## Features 
This library reads Amazon DynamoDB tables as Spark DataFrames. The API accepts several options: 
- `tableName` : name of the DynamoDB table
- `region`: region of the DynamoDB table e.g `eu-west-1`
- `scanEntireTable` : defaults to true, if set false only first page of items is read. 

If the schema is not provided, the API will infer key schema of DynamoDB table which will only include hash and range key attributes. 
Depending on the attributes specified in schema, the API will only read these columns in DynamoDB table. 

## Scala Main to Create Parquet file
```scala
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.types._

object SparkDeploymentDemo {
  def main(args: Array[String]) {
     println("Defining the Schema")
     // Define The Schema of Dataframe 
     val schema = StructType(
      Seq(
        StructField("adnetwork_name", StringType),
        StructField("network_id", StringType)
        )
      ) 
     // Add Database and Table details to Scala Options
     val options = Map(
      "tableName" -> "prod1.ad-network.configuration",
      "region" -> "eu-west-1",
      "scanEntireTable" -> "true"
      )
     val conf = new SparkConf().setAppName("SparkMe Application")
     val sc = new SparkContext(conf)
     val sqlContext = new org.apache.spark.sql.SQLContext(sc)
     val df = sqlContext.read.format("com.onzo.spark.dynamodb").
     schema(schema).options(options).load()
     // Show the dataframe
    df.show
    // Print The Schema
    df.printSchema
    // Write Dataframe into a Parquet
    df.write.parquet("df.parquet")
    //Read Parquet
    val parquetFile = sqlContext.read.parquet("df.parquet")

    //Parquet files can also be registered as tables and then used in SQL statements.
    parquetFile.registerTempTable("parquetFile")
    // Executing a simple SQl Query and Print the result
    val networks = sqlContext.sql("SELECT adnetwork_name FROM parquetFile")
    networks.map(t => "Name: " + t(0)).collect().foreach(println)
     }
}
```
## Interactive Scala Shell

The easiest way to start using Spark is through the Scala shell:

    ./bin/spark-shell

## Building from source

Assuming git , scala and sbt installed.

This library is built using sbt. To build an assembly jar, simply run `sbt assembly` from the project root. 
```bash
 host> git clone https://github.com/mhdjunaid/spark-dynamodb
 host> cd spark-dynamodb
 host> sbt assembly
```
## Configure Spark Master and Worker Locally

Assuming Spark is installed. 
```bash
 host> ./sbin/start-master.sh
 host> ./bin/spark-class org.apache.spark.deploy.worker.Worker <spark-master-url>
```


## Submit to spark
In Spark Shell execute the command below replacing the your spark environment options.
```bash
 host> ./bin/spark-submit --class com.dcmn.spark.resource.SparkJob --master <spark-master-url> --name "Spark DynamoDB Parquet" <application-jar>
```
That's it!