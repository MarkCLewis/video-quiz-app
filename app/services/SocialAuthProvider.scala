package services

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, HTTPLayer, IDGenerator, PasswordHasher }
import com.mohiva.play.silhouette.crypto._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1.TwitterProvider
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{ CookieSecretProvider, CookieSecretSettings }
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{ CookieStateProvider, CookieStateSettings }
import com.mohiva.play.silhouette.impl.providers.oauth2.{ FacebookProvider, GoogleProvider }
import play.api.Configuration

import scala.concurrent.duration._

//object SocialAuthProvider {
//  val providers = Seq("google" -> "Google")
//}
//
//class SocialAuthProvider(
//    config: Configuration,
//    httpLayer: HTTPLayer,
//    hasher: PasswordHasher,
//    authInfoService: AuthInfoRepository,
//    credentials: CredentialsProvider,
//    idGenerator: IDGenerator,
//    clock: Clock
//) {
//  private[this] val cookieSigner = new JcaCookieSigner(new JcaCookieSignerSettings("coding-quizzes"))
//  private[this] val oAuth2StateProvider = new CookieStateProvider(CookieStateSettings(
//    cookieName = config.getString("silhouette.oauth2StateProvider.cookieName").getOrElse(throw new IllegalArgumentException()),
//    cookiePath = config.getString("silhouette.oauth2StateProvider.cookiePath").getOrElse(throw new IllegalArgumentException()),
//    cookieDomain = config.getString("silhouette.oauth2StateProvider.cookieDomain"),
//    secureCookie = config.getBoolean("silhouette.oauth2StateProvider.secureCookie").getOrElse(throw new IllegalArgumentException()),
//    httpOnlyCookie = config.getBoolean("silhouette.oauth2StateProvider.httpOnlyCookie").getOrElse(throw new IllegalArgumentException()),
//    expirationTime = config.getInt("silhouette.oauth2StateProvider.expirationTime").map(_.seconds).getOrElse(throw new IllegalArgumentException())
//  ), idGenerator, cookieSigner, clock)
//
//  private[this] val googleSettings = OAuth2Settings(
//    authorizationURL = config.getString("silhouette.google.authorizationUrl"),
//    accessTokenURL = config.getString("silhouette.google.accessTokenUrl").getOrElse(throw new IllegalArgumentException()),
//    redirectURL = config.getString("silhouette.google.redirectUrl").getOrElse(throw new IllegalArgumentException()),
//    clientID = config.getString("silhouette.google.clientId").getOrElse(throw new IllegalArgumentException()),
//    clientSecret = config.getString("silhouette.google.clientSecret").getOrElse(throw new IllegalArgumentException()),
//    scope = config.getString("silhouette.google.scope")
//  )
//
//  private[this] val google = new GoogleProvider(httpLayer, oAuth2StateProvider, googleSettings)
//
//  val providers = Seq("google" -> google)
//}
