package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.oauth2.GoogleProvider

import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future
import models.Queries
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

case class User() extends Identity

trait DefaultEnv extends Env {
  type I = User
  type A = SessionAuthenticator
}

class UserService extends IdentityService[User] {
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = ???
  def save(user: User): Future[User] = ???
  def save(profile: CommonSocialProfile): Future[User] = ???
}

/**
 * The social auth controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets The webjar assets implementation.
 */
class AuthenticationController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  dbConfigProvider: DatabaseConfigProvider,
  googleProvider: GoogleProvider)
  extends Controller with I18nSupport with Logger {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  implicit val db = dbConfig.db
  private val TrinityEmail = """(\w+)@trinity.edu""".r

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticateGoogle = Action.async { implicit request =>
    googleProvider.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- googleProvider.retrieveProfile(authInfo)
            authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
            value <- silhouette.env.authenticatorService.init(authenticator)
            action <- profile.email match {
              case Some(TrinityEmail(email)) =>
                for {
                  value <- Queries.fetchUserByName(email, db)
                  n = value.userid
                  instructorCourses <- Queries.instructorCourseIds(n, db)
                } yield instructorCourses match {
                  case Seq() =>
                    Redirect(routes.Application.quizList).withSession(request.session + ("username" -> value.username) + ("userid" -> n.toString))
                  case _ =>
                    Redirect(routes.Application.instructorPage).withSession(request.session + ("username" -> value.username) + ("userid" -> n.toString) + ("instructor" -> "yes"))
                }
              case _ => 
                Future.successful(Redirect(routes.Application.index()).flashing("message" -> "Invalid Login"))
            }
            result <- silhouette.env.authenticatorService.embed(value, action)
          } yield {
            println(profile.email)
            result
          }
        }
    .recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.Application.index()).flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}