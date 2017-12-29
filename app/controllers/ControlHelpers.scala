package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object ControlHelpers {
  private[controllers] def authenticate(request: MessagesRequest[AnyContent]): Boolean = {
    request.session.get("username") match {
      case None => false
      case Some(uname) => true
    }
  }

  private[controllers] def isInstructor(request: MessagesRequest[AnyContent]): Boolean = {
    request.session.get("instructor") match {
      case Some("yes") => true
      case _ => false
    }
  }

  private[controllers] def AuthenticatedAction(f: MessagesRequest[AnyContent] => Future[Result])
      (implicit ec: ExecutionContext, Action: ActionBuilder[MessagesRequest, AnyContent]): Action[AnyContent] = {
    Action.async { request =>
      if (authenticate(request)) {
        f(request)
      } else {
        Future { Redirect(routes.Application.index()) }
      }
    }
  }

  private[controllers] def AuthenticatedInstructorAction(f: MessagesRequest[AnyContent] => Future[Result])
      (implicit ec: ExecutionContext, Action: ActionBuilder[MessagesRequest, AnyContent]): Action[AnyContent] = {
    Action.async { request =>
      if (authenticate(request) && isInstructor(request)) {
        f(request)
      } else {
        Future { Redirect(routes.Application.index()) }
      }
    }
  }
}