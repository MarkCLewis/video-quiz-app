package modules

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.impl.providers.oauth2.GoogleProvider
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.SilhouetteProvider
import com.mohiva.play.silhouette.impl.providers.OAuth2StateProvider
import com.mohiva.play.silhouette.impl.providers.OAuth2Settings
import com.mohiva.play.silhouette.crypto.{ JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings }
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._

import controllers.DefaultEnv
import controllers.UserService
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.ws.WSClient
import com.mohiva.play.silhouette.api.util.PlayHTTPLayer
import javax.inject.Named
import com.mohiva.play.silhouette.api.util.FingerprintGenerator
import com.mohiva.play.silhouette.api.util.IDGenerator
import com.mohiva.play.silhouette.api.util.Clock
import scala.concurrent.duration._
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticatorSettings
import com.mohiva.play.silhouette.impl.providers.oauth2.state.CookieStateProvider
import com.mohiva.play.silhouette.impl.providers.oauth2.state.CookieStateSettings
import com.mohiva.play.silhouette.impl.util.DefaultFingerprintGenerator
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator

class SilhouetteModule extends AbstractModule with ScalaModule {
  
  /**
   * Configures the module.
   */
  def configure() {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[Clock].toInstance(Clock())
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the cookie signer for the OAuth2 state provider.
   *
   * @param configuration The Play configuration.
   * @return The cookie signer for the OAuth2 state provider.
   */
  @Provides @Named("oauth2-state-cookie-signer")
  def provideOAuth2StageCookieSigner(configuration: Configuration): CookieSigner = {
    val config = new JcaCookieSignerSettings("my-key")

    new JcaCookieSigner(config)
  }
  
  /**
   * Provides the cookie signer for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The cookie signer for the authenticator.
   */
  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(configuration: Configuration): CookieSigner = {
    val config = new JcaCookieSignerSettings("my-key")

    new JcaCookieSigner(config)
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = new JcaCrypterSettings("my-key")

    new JcaCrypter(config)
  }
  
  /**
   * Provides the authenticator service.
   *
   * @param cookieSigner The cookie signer implementation.
   * @param crypter The crypter implementation.
   * @param fingerprintGenerator The fingerprint generator implementation.
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-cookie-signer") cookieSigner: CookieSigner,
    @Named("authenticator-crypter") crypter: Crypter,
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[SessionAuthenticator] = {

    val config = new SessionAuthenticatorSettings(
        sessionKey = configuration.getString("silhouette.authenticator.session.sessionKey").getOrElse("auth"),
        useFingerprinting = configuration.getBoolean("silhouette.authenticator.session.useFingerprinting").getOrElse(false),
        authenticatorIdleTimeout = Some(configuration.getInt("silhouette.authenticator.session.authenticatorIdelTimeout").getOrElse(60).seconds),
        authenticatorExpiry = configuration.getInt("silhouette.authenticator.session.authenticatorExpiry").getOrElse(60).seconds
        )
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new SessionAuthenticatorService(config, fingerprintGenerator, encoder, clock)
  }
  
  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[SessionAuthenticator],
    eventBus: EventBus): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }
  
  /**
   * Provides the OAuth2 state provider.
   *
   * @param idGenerator The ID generator implementation.
   * @param cookieSigner The cookie signer implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth2 state provider implementation.
   */
  @Provides
  def provideOAuth2StateProvider(
    idGenerator: IDGenerator,
    @Named("oauth2-state-cookie-signer") cookieSigner: CookieSigner,
    configuration: Configuration, clock: Clock): OAuth2StateProvider = {

    val settings = new CookieStateSettings(
        cookieName = configuration.getString("silhouette.oauth2StateProvider.cookieName").getOrElse("OAuth2State"), 
        cookiePath = configuration.getString("silhouette.oauth2StateProvider.cookiePath").getOrElse("/"), 
        cookieDomain = Some(configuration.getString("silhouette.oauth2StateProvider.cookieDomain").getOrElse("localhost")), 
        secureCookie = configuration.getBoolean("silhouette.oauth2StateProvider.secureCookie").getOrElse(true), 
        httpOnlyCookie = configuration.getBoolean("silhouette.oauth2StateProvider.httpOnlyCookie").getOrElse(true), 
        expirationTime = configuration.getInt("silhouette.oauth2StateProvider.expirationTime").getOrElse(5).minutes) 
      
    new CookieStateProvider(settings, idGenerator, cookieSigner, clock)
  }
  
  /**
   * Provides the Google provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    config: Configuration): GoogleProvider = {

    val googleSettings = OAuth2Settings(
      authorizationURL = config.getString("silhouette.google.authorizationUrl"),
      accessTokenURL = config.getString("silhouette.google.accessTokenUrl").getOrElse(throw new IllegalArgumentException()),
      redirectURL = config.getString("silhouette.google.redirectUrl").getOrElse(throw new IllegalArgumentException()),
      clientID = config.getString("silhouette.google.clientId").getOrElse(throw new IllegalArgumentException()),
      clientSecret = config.getString("silhouette.google.clientSecret").getOrElse(throw new IllegalArgumentException()),
      scope = config.getString("silhouette.google.scope"))

    new GoogleProvider(httpLayer, stateProvider, googleSettings)
  }  
}