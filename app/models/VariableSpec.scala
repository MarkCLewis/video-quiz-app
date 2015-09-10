package models

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import Tables._

/**
 * @author mlewis
 */
object VariableType extends Enumeration {
  type Type = Value
  val Int, Double, String = Value
}

object VariableSpec {
  val IntSpecType = 0
  val DoubleSpecType = 1
  val StringSpecType = 2
  val IntListSpecType = 3
  val IntArraySpecType = 4
  val StringListSpecType = 5

  def apply(vs:VariableSpecificationsRow): VariableSpec = vs.specType match {
    case IntSpecType =>
      IntSpec(vs.paramNumber, vs.name, vs.min.getOrElse(0), vs.max.getOrElse(10))
    case DoubleSpecType =>
      DoubleSpec(vs.paramNumber, vs.name, vs.min.getOrElse(0).toDouble, vs.max.getOrElse(10).toDouble)
    case StringSpecType =>
      StringSpec(vs.paramNumber, vs.name, vs.length.getOrElse(5), vs.genCode.getOrElse(""))
    case IntListSpecType =>
      ListIntSpec(vs.paramNumber, vs.name, vs.minLength.getOrElse(2), vs.maxLength.getOrElse(10), vs.min.getOrElse(0), vs.max.getOrElse(10))
    case IntArraySpecType =>
      ArrayIntSpec(vs.paramNumber, vs.name, vs.minLength.getOrElse(2), vs.maxLength.getOrElse(10), vs.min.getOrElse(0), vs.max.getOrElse(10))
    case StringListSpecType =>
      ListStringSpec(vs.paramNumber, vs.name, vs.minLength.getOrElse(2), vs.maxLength.getOrElse(10), vs.length.getOrElse(5), vs.genCode.getOrElse(""))
  }
  
}

sealed trait VariableSpec {
  val name: String
  val typeName: String
  val typeNumber: Int
  val paramNumber: Int
  def codeGenerator(): String // Return a string that is code to generate this value. 
}

case class IntSpec(paramNumber:Int, name: String, min: Int, max: Int) extends VariableSpec {
  val typeName = "Int"
  val typeNumber = VariableSpec.IntSpecType

  def codeGenerator(): String = {
    s"val $name = util.Random.nextInt(($max)-($min))+($min)"
  }
}

case class DoubleSpec(paramNumber:Int, name: String, min: Double, max: Double) extends VariableSpec {
  val typeName = "Double"
  val typeNumber = VariableSpec.DoubleSpecType

  def codeGenerator(): String = {
    s"val $name = math.random*(($max)-($min))+($min)"
  }
}

case class StringSpec(paramNumber:Int, name: String, length: Int, genCode: String) extends VariableSpec {
  val typeName = "String"
  val typeNumber = VariableSpec.StringSpecType

  def codeGenerator(): String = {
    if(genCode.isEmpty())
      s"val $name = (for(i <- 0 until $length) yield { ('a'+util.Random.nextInt(26)).toChar }).mkString"
    else
      s"val $name = $genCode"
  }
}

case class ListIntSpec(paramNumber:Int, name: String, minLen:Int, maxLen:Int, min: Int, max: Int) extends VariableSpec {
  val typeName = "List[Int]"
  val typeNumber = VariableSpec.IntListSpecType

  def codeGenerator(): String = {
    s"val $name = List.fill(util.Random.nextInt(($maxLen)-($minLen))+($minLen))(util.Random.nextInt(($max)-($min))+($min))"
  }
}

case class ArrayIntSpec(paramNumber:Int, name: String, minLen:Int, maxLen:Int, min: Int, max: Int) extends VariableSpec {
  val typeName = "Array[Int]"
  val typeNumber = VariableSpec.IntArraySpecType

  def codeGenerator(): String = {
    s"val $name = Array.fill(util.Random.nextInt(($maxLen)-($minLen))+($minLen))(util.Random.nextInt(($max)-($min))+($min))"
  }
}

case class ListStringSpec(paramNumber:Int, name: String, minLen:Int, maxLen:Int, stringLength: Int, genCode: String) extends VariableSpec {
  val typeName = "List[String]"
  val typeNumber = VariableSpec.StringListSpecType

  def codeGenerator(): String = {
    if(genCode.isEmpty())
      s"val $name = List.fill(util.Random.nextInt(($maxLen)-($minLen))+($minLen)){(for(i <- 0 until $stringLength) yield { ('a'+util.Random.nextInt(26)).toChar }).mkString}"
    else
      s"val $name = List.fill(util.Random.nextInt(($maxLen)-($minLen))+($minLen)){$genCode}"
  }
}
