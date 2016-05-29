/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package com.cloudera.spark.mllib

import scala.beans.BeanInfo

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{Row, SQLContext}

@BeanInfo
case class SpamDocument(file: String, text: String, label: Double)

object Spark {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Spam")
    SparkConfUtil.setConf(conf)
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // read in the spam files
    val spamrdd = sc.wholeTextFiles("data/titanic", 1)
    val spamdf = spamrdd.map(d => SpamDocument(d._1, d._2, 0)).toDF()
    spamdf.show()
    
    // read in the ham files
    val hamrdd = sc.wholeTextFiles("data/titanic", 1)
    val hamdf = hamrdd.map(d => SpamDocument(d._1, d._2, 1)).toDF()
    hamdf.show()
    
    // all
    val alldf = spamdf.unionAll(hamdf)
    alldf.show()
    
    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
    lr.setLabelCol("label")
    lr.setFeaturesCol("features")

    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))
    
    val lrModel = pipeline.fit(alldf)
    println(lrModel.toString())
      
    sc.stop()
  }
}
// scalastyle:on println