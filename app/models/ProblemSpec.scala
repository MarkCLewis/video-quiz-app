package models

import java.io.File
import java.io.PrintWriter
import sys.process._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import Tables._

object ProblemSpec {
  val MultipleChoiceType = 0
  val FunctionType = 1
  val LambdaType = 2
  val ExpressionType = 3

  def multipleChoice(id: Int, db: Database): Future[MultipleChoice] = {
    val mcRow = db.run(MultipleChoiceQuestions.filter(_.mcQuestionId === id).result.head)
    mcRow.map(row => {
      val options = Seq(row.option1, row.option2) ++ Seq(row.option3, row.option4, row.option5, row.option6,
        row.option7, row.option8).filter(_.nonEmpty).map(_.get)
      MultipleChoice(row.mcQuestionId, row.prompt, options, row.correctOption)
    })
  }

  def writeFunction(id: Int, db: Database): Future[WriteFunction] = {
    val funcRow = db.run(FunctionQuestions.filter(_.funcQuestionId === id).result.head)
    funcRow.flatMap(row => {
      val specs = db.run(VariableSpecifications.filter(vs => vs.questionId === id && vs.questionType === FunctionType)
        .sortBy(_.paramNumber).result).map(s => s.map(vs => VariableSpec(vs)))
      specs.map(s => WriteFunction(row.funcQuestionId, row.prompt, row.correctCode, row.functionName, s, row.numRuns))
    })
  }

  def writeLambda(id: Int, db: Database): Future[WriteLambda] = {
    val lambdaRow = db.run(LambdaQuestions.filter(_.lambdaQuestionId === id).result.head)
    lambdaRow.flatMap(row => {
      val specs = db.run(VariableSpecifications.filter(vs => vs.questionId === id && vs.questionType === LambdaType)
        .sortBy(_.paramNumber).result).map(s => s.map(vs => VariableSpec(vs)))
      specs.map(s => WriteLambda(row.lambdaQuestionId, row.prompt, row.correctCode, row.returnType, s, row.numRuns))
    })
  }

  def writeExpression(id: Int, db: Database): Future[WriteExpression] = {
    val exprRow = db.run(ExpressionQuestions.filter(_.exprQuestionId === id).result.head)
    exprRow.flatMap(row => {
      val specs = db.run(VariableSpecifications.filter(vs => vs.questionId === id && vs.questionType === ExpressionType)
        .sortBy(_.paramNumber).result).map(s => s.map(vs => VariableSpec(vs)))
      specs.map(s => WriteExpression(row.exprQuestionId, row.prompt, row.correctCode, s, row.generalSetup, row.numRuns))
    })
  }

  def apply(questionType: Int, id: Int, db: Database): Future[ProblemSpec] = {
    questionType match {
      case MultipleChoiceType => // multiple choice
        multipleChoice(id, db)
      case FunctionType => // function
        writeFunction(id, db)
      case LambdaType => // lambda
        writeLambda(id, db)
      case ExpressionType => // expression
        writeExpression(id, db)
    }
  }
  
  def newWriteFunction(db:Database):Future[WriteFunction] = {
    db.run(FunctionQuestions += FunctionQuestionsRow(0,"","","",10)).flatMap(_ =>
      db.run(FunctionQuestions.map(_.funcQuestionId).max.result)).flatMap(id =>
        writeFunction(id.get,db))
  } 
  
  def newWriteLambda(db:Database):Future[WriteLambda] = {
    db.run(LambdaQuestions += LambdaQuestionsRow(0,"","","",10)).flatMap(_ =>
      db.run(LambdaQuestions.map(_.lambdaQuestionId).max.result)).flatMap(id =>
        writeLambda(id.get,db))
  } 
  
  def newWriteExpression(db:Database):Future[WriteExpression] = {
    db.run(ExpressionQuestions += ExpressionQuestionsRow(0,"","","",10)).flatMap(_ =>
      db.run(ExpressionQuestions.map(_.exprQuestionId).max.result)).flatMap(id =>
        writeExpression(id.get,db))
  } 

  def nestTest(testCode: String, numRuns: Int): String = {
    s"""import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
    
Future {
   Thread.sleep(10000)
   sys.exit(1)
}
for(i <- 1 to $numRuns) {
  $testCode
}
"""
  }

  def runCode(code: String, input: String, numRuns: Int): Boolean = {
    val tmpFile = File.createTempFile("test", ".scala")
    tmpFile.deleteOnExit()
    val pw = new PrintWriter(tmpFile)
    val nestedCode = nestTest(code, numRuns)
    println(nestedCode)
    pw.println(nestedCode)
    pw.close
    val process = s"scala -J-Djava.security.manager -J-Djava.security.policy=mypolicy ${tmpFile.getAbsolutePath()}".run() 
    val ret = process.exitValue == 0
    println("Done running - " + ret)
    println("Exit value is " + process.exitValue)
    ret
  }
}

sealed trait ProblemSpec {
  def checkResponse(response: String): Boolean
  val id:Int
  val typeValue:Int
  val prompt: String
}

case class MultipleChoice(id: Int, prompt: String, options: Seq[String], correct: Int) extends ProblemSpec {
  def checkResponse(response: String): Boolean = {
    try {
      response != null && response.toInt == correct
    } catch {
      case e: NumberFormatException => false
    }
  }
  val typeValue = ProblemSpec.MultipleChoiceType
}

case class WriteFunction(id: Int, prompt: String, correctCode: String, functionName: String, varSpecs: Seq[VariableSpec], numRuns: Int) extends ProblemSpec {
  def checkResponse(response: String): Boolean = {
    val code = s"""
      ${correctCode.replaceAll(functionName+"(", functionName + "Correct(")}
      $response
      ${varSpecs.map(_.codeGenerator).mkString("\n")}
      val theirFunc = $functionName(${varSpecs.map(_.name).mkString(",")})
      val correctFunc = ${functionName}Correct(${varSpecs.map(_.name).mkString(",")})
      if(theirFunc != correctFunc) sys.exit(1)
      """
    ProblemSpec.runCode(code, "", numRuns)
  }
  val typeValue = ProblemSpec.FunctionType
}

case class WriteLambda(id: Int, prompt: String, correctCode: String, returnType: String, varSpecs: Seq[VariableSpec], numRuns: Int) extends ProblemSpec {
  def checkResponse(response: String): Boolean = {
    val funcType = s"(${varSpecs.map(_.typeName).mkString(", ")}) => $returnType"
    val args = varSpecs.map(_.name).mkString(", ")
    val code = s"""
      def tester(f1:$funcType, f2:$funcType):Unit = {
        ${varSpecs.map(_.codeGenerator).mkString("\n")}
        if(f1($args) != f2($args)) sys.exit(1)
      }
      tester($response,$correctCode)
      """
    ProblemSpec.runCode(code, "", numRuns)
  }
  val typeValue = ProblemSpec.LambdaType
}

case class WriteExpression(id: Int, prompt: String, correctCode: String, varSpecs: Seq[VariableSpec], generalSetup: String, numRuns: Int) extends ProblemSpec {
  def checkResponse(response: String): Boolean = {
    val code = s"""
      ${varSpecs.map(_.codeGenerator).mkString("\n")}
      $generalSetup
      if({$response} != {$correctCode}) sys.exit(1)
      """
    ProblemSpec.runCode(code, "", numRuns)
  }
  val typeValue = ProblemSpec.ExpressionType
}
/*

Parson's problem


case class IOCode(prompt: String) extends ProblemSpec {
  def checkResponse(response: String): Boolean = ???
}

case class CodeCompletion(prompt: String, code: String, varSpecs: Seq[VariableSpec], numRuns: Int) extends ProblemSpec {
  def checkResponse(response: String): Boolean = ???
}

case class CodeCompiles(prompt: String) extends ProblemSpec {
  def checkResponse(response: String): Boolean = ???
}

case class RegExMatching(prompt: String) extends ProblemSpec {
  def checkResponse(response: String): Boolean = ???
}

//case class CodeTracing(prompt:String) extends ProblemSpec {
//}

case class FileSubmission(prompt: String) extends ProblemSpec {
  def checkResponse(response: String): Boolean = ???
}
*/