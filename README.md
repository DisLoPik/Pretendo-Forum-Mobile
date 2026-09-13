# Pretendo Forum

A Kotlin Multiplatform app for [forum.pretendo.network](https://forum.pretendo.network),
for Android and iOS. The forum's website is a Discourse install laid out for a desktop
browser; this is the same forum laid out for a phone, with an Accessibility page that
changes how the whole app reads.

## Running it

- Android: `./gradlew :androidApp:assembleDebug`, or the run configuration in the IDE.
- iOS: open [`/iosApp`](./iosApp) in Xcode and run. Needs an Apple Silicon Mac, because
  Compose Multiplatform dropped the Intel simulator target.

Tests: `./gradlew :shared:testAndroidHostTest` and `./gradlew :shared:iosSimulatorArm64Test`.

The host tests include `ScreenCompositionTest`, which draws each screen under Robolectric.
That exists because a screen that compiles can still die the moment it is laid out: a
mismatched Material version once took out every screen with a text field on it, and a
compile said nothing. Keep `material3` and `composeMultiplatform` on matching versions.

## How signing in works

You sign in with your **Pretendo Network username and password**, the same ones as on
pretendo.network. There is no separate forum account.

It takes a detour, because the forum has no account system of its own: it delegates to
Pretendo Network through DiscourseConnect, and the forum's user API keys are switched off.
So the app walks the same path a browser walks, and ends up holding the same session
cookie a browser would:

1. `POST pretendo.network/api/auth/login` with the username and password, which answers
   with an account token;
2. `GET forum.pretendo.network/session/sso`, which points at Pretendo to vouch for you;
3. that Pretendo URL, carrying the token from step 1, which signs a payload and points
   back at the forum;
4. the forum reads the payload and issues its `_t` session cookie.

From then on the app is an ordinary signed-in session. That has one consequence worth
knowing: Discourse protects session writes with a CSRF token, so every post, like and
delete carries one, and a rejected token is refreshed and retried before the failure is
believed. A lapsed session is rebuilt from the stored account token without interrupting
you.

The password is sent to Pretendo's account service and is not stored on the phone; only
the session and the account token it returns are. Signing out clears both.

Reading works fully signed out. An account is only needed to post, reply, like, bookmark
and send messages.

## What is in here

```
shared/src/commonMain/kotlin/com/dislopik/pretendo/
  data/     the forum API, auth, storage, settings, formatting
  model/    Discourse's JSON shapes, plus the enums the UI thinks in
  ui/
    html/   turning a post's HTML into blocks, and drawing them
    components/  avatars, topic rows, post cards, empty and error states
    screens/     one file per screen
```

Platform code is thin and lives in `androidMain` / `iosMain`: key-value storage,
opening a browser, the clipboard, the share sheet, the clock, and keeping the screen awake.

### Reading posts

Discourse renders posts to HTML on the server and sends them as `cooked`. Showing that as
a web page on a phone would mean a WebView per post and a stylesheet written for a desktop
width, and text scaling would not work. So `CookedHtml` parses the HTML into a small list
of blocks: paragraphs, headings, quotes, code, lists, pictures, tables, spoilers, link
previews, and polls. Compose draws them as real text. That is what lets every accessibility
setting reach every word.

The parser is deliberately free of Compose types so it can be tested on its own, and
`CookedHtmlTest` uses the HTML the forum actually sends.

### The Accessibility page

Everything on it is stored on the phone. A Discourse account has nowhere to keep this, so
it is per-device by design (which is not only a limitation), since the phone that needs
larger text is often not the only place the same account gets used.

| | |
|---|---|
| Text | size (85–220%), typeface, bold, line spacing, letter spacing |
| Colour | follow system / dark / light / black, five highlight colours, stronger contrast |
| Layout | compact, comfortable or spacious; bigger tap targets |
| Posts | show in full, or collapse long ones behind "Show more" |
| Media | hide pictures, emoji and avatars, each tappable to reveal |
| Motion | turn off fades and animations |
| Reading | underline links, full dates instead of "3d", quotes open by default, keep the screen on |

A sample of the app's own text sits at the top of the page and is drawn with the live
theme, so a choice can be made by looking at it rather than at a number.

## Design

Colours come from Pretendo Network's own stylesheet (`--bg-shade-*`, `--accent-shade-*`,
`--text-shade-*`), so the dark theme matches the site. Light, black and high-contrast
schemes are derived from the same palette. Icons are drawn in code in `PretendoIcons`,
because Compose Multiplatform ships no icon pack and the standalone artifacts are no longer
maintained.

## What still needs the website

Voting in polls, uploading images to a post, editing your profile and avatar, notification
preferences, and moderation tools. The About screen says the same thing, so nobody has to
find out by hunting for a button that is not there.

## Known limits

- The sign-in chain was traced against the live site, but could not be run end to end
  without an account, so steps 2–4 above are the part to watch if sign-in misbehaves. Each
  leg reports its own failure so a break says which step gave up.
- iOS was typechecked but not run: this was built on Windows, where Kotlin/Native can
  compile iOS sources but not link or launch them. The composition tests are Android-only
  for the same reason.
- Post drafts are not kept if the composer is closed.
- New posts appear after the screen reloads rather than arriving live; Discourse's
  MessageBus is not wired up.

  ### Ai Disclaimer:
  AI was used in the making of documentation and some in-code comments. Mainstream Corperate AI was not used. A local model was used on my PC.
