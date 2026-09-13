package com.dislopik.pretendo

import androidx.test.core.app.ApplicationProvider
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.ForumAuth
import com.dislopik.pretendo.data.ForumAuthException
import com.dislopik.pretendo.data.KeyValueStore
import com.dislopik.pretendo.data.initPretendoAndroid
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sign-on handshake, driven against a stubbed forum and account site.
 *
 * This chain crosses two hosts over several redirects and cannot be exercised against the
 * real sites without an account, so it is pinned here instead. It is also the test that
 * catches the handshake being broken by an HTTP client that follows redirects on its own:
 * the client below follows them by default, exactly like the shared one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForumAuthTest {

    private val account = ForumAuth.ACCOUNT_URL
    private val forum = Forum.BASE_URL

    @Before
    fun setUp() {
        initPretendoAndroid(ApplicationProvider.getApplicationContext())
        // Robolectric keeps storage between methods in a class, so start each one clean.
        ForumAuth(KeyValueStore()).signOut()
    }

    /** Stands in for both sites, answering the way they really do. */
    private fun handshakeClient(
        vouchResponse: (sentCookie: String?) -> Pair<HttpStatusCode, String>,
        loginStatus: HttpStatusCode = HttpStatusCode.OK,
        loginBody: String = """{"accessToken":"token-abc","refreshToken":"refresh-xyz"}"""
    ): HttpClient {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            when {
                url.startsWith("$account/api/auth/login") ->
                    if (loginStatus == HttpStatusCode.OK) {
                        respond(
                            content = loginBody,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respondError(loginStatus, loginBody)
                    }

                // The forum starts the handshake and points at the account site.
                url == "$forum/session/sso" -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location to listOf("$account/account/sso/discourse?sso=A&sig=B"),
                        HttpHeaders.SetCookie to listOf("_forum_session=session-1; path=/; HttpOnly")
                    )
                )

                // The account site vouches, if it was given the token.
                url.startsWith("$account/account/sso/discourse") -> {
                    val (status, location) = vouchResponse(request.headers[HttpHeaders.Cookie])
                    respond(
                        content = "",
                        status = status,
                        headers = headersOf(HttpHeaders.Location, location)
                    )
                }

                // The forum reads the signed payload and issues the login cookie.
                url.startsWith("$forum/session/sso_login") -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location to listOf("$forum/"),
                        HttpHeaders.SetCookie to listOf("_t=login-cookie-1; path=/; HttpOnly")
                    )
                )

                url == "$forum/" -> respond("<html></html>")

                else -> respondError(HttpStatusCode.NotFound, "unexpected: $url")
            }
        }
        // Redirect following is left on, because the shared client has it on. The handshake
        // has to cope with that itself.
        return HttpClient(engine) { expectSuccess = false }
    }

    @Test
    fun signsInAndKeepsTheForumSession() = runTest {
        var cookieSeenByAccountSite: String? = null
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(vouchResponse = { cookie ->
                cookieSeenByAccountSite = cookie
                HttpStatusCode.Found to "$forum/session/sso_login?sso=A&sig=B"
            })
        )

        auth.signIn("someone", "a-password")

        assertTrue(auth.isSignedIn)
        assertEquals("login-cookie-1", auth.sessionCookie)
        // The session cookie from the first hop has to survive to the last, because it is
        // what carries the sign-on nonce.
        assertEquals("session-1", auth.forumSessionCookie)
        assertTrue(
            cookieSeenByAccountSite?.contains("access_token=token-abc") == true,
            "the account site was not given the token from the login"
        )
    }

    @Test
    fun theSessionSurvivesIntoANewInstance() = runTest {
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(vouchResponse = {
                HttpStatusCode.Found to "$forum/session/sso_login?sso=A&sig=B"
            })
        )
        auth.signIn("someone", "a-password")

        // A fresh instance stands in for the next launch of the app.
        assertTrue(ForumAuth(KeyValueStore()).isSignedIn)
    }

    @Test
    fun followsARelativeRedirectBackToTheForum() = runTest {
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(vouchResponse = {
                // Some sites answer with a path rather than a whole address.
                HttpStatusCode.Found to "$forum/session/sso_login?sso=A&sig=B"
            })
        )
        auth.signIn("someone", "a-password")
        assertEquals("login-cookie-1", auth.sessionCookie)
    }

    @Test
    fun reportsWhenTheAccountSiteRefusesTheToken() = runTest {
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(vouchResponse = {
                // An unusable token lands on the login page instead.
                HttpStatusCode.Found to "/account/login?redirect=%2Faccount%2Fsso%2Fdiscourse"
            })
        )

        val failure = assertFailsWith<ForumAuthException> {
            auth.signIn("someone", "a-password")
        }
        assertTrue(failure.message!!.contains("did not accept"), failure.message!!)
        assertFalse(auth.isSignedIn)
    }

    @Test
    fun reportsAWrongPasswordInPlainWords() = runTest {
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(
                vouchResponse = { HttpStatusCode.Found to "$forum/session/sso_login" },
                loginStatus = HttpStatusCode.BadRequest,
                loginBody = """{"data":{"code":"INVALID_PASSWORD","message":"Incorrect password"}}"""
            )
        )

        val failure = assertFailsWith<ForumAuthException> {
            auth.signIn("someone", "wrong")
        }
        assertEquals("That password is not right.", failure.message)
        assertFalse(auth.isSignedIn)
    }

    @Test
    fun reportsAnUnknownUsernameSeparately() = runTest {
        val auth = ForumAuth(
            KeyValueStore(),
            handshakeClient(
                vouchResponse = { HttpStatusCode.Found to "$forum/session/sso_login" },
                loginStatus = HttpStatusCode.BadRequest,
                loginBody = """{"data":{"code":"INVALID_USERNAME","message":"Could not find user"}}"""
            )
        )

        val failure = assertFailsWith<ForumAuthException> { auth.signIn("nobody", "x") }
        assertEquals("There is no account with that username.", failure.message)
    }

    @Test
    fun refusesToEvenTryWithAnEmptyForm() = runTest {
        val auth = ForumAuth(KeyValueStore(), handshakeClient(vouchResponse = {
            HttpStatusCode.Found to "$forum/session/sso_login"
        }))
        assertFailsWith<ForumAuthException> { auth.signIn("", "") }
        assertFailsWith<ForumAuthException> { auth.signIn("someone", "") }
    }

    @Test
    fun signingOutClearsEverything() = runTest {
        val store = KeyValueStore()
        val auth = ForumAuth(store, handshakeClient(vouchResponse = {
            HttpStatusCode.Found to "$forum/session/sso_login?sso=A&sig=B"
        }))
        auth.signIn("someone", "a-password")
        assertTrue(auth.isSignedIn)

        auth.signOut()

        assertFalse(auth.isSignedIn)
        assertEquals(null, auth.sessionCookie)
        assertFalse(ForumAuth(store).isSignedIn)
    }

    @Test
    fun forumCookiesCarryBothTheLoginAndTheSession() = runTest {
        val auth = ForumAuth(KeyValueStore(), handshakeClient(vouchResponse = {
            HttpStatusCode.Found to "$forum/session/sso_login?sso=A&sig=B"
        }))
        auth.signIn("someone", "a-password")

        val cookies = auth.forumCookies()!!
        assertTrue(cookies.contains("_t=login-cookie-1"), cookies)
        assertTrue(cookies.contains("_forum_session=session-1"), cookies)
    }
}
