# ADR-022: Scrobbling (Last.fm and ListenBrainz) with Keystore-Encrypted Credentials

## Status

Accepted

## Context

The app offers Last.fm and ListenBrainz scrobbling from the settings screens. Both services
require persistent credentials (session key for Last.fm, personal API token for ListenBrainz)
which are secrets and must not be stored in plaintext. Last.fm also needs API keys that are
channel-restricted (github/playstore only, ADR-003).

## Decision

- Implement two clients over the shared Ktor stack (ADR-020):
  - `LastFmService`: REST calls for album/artist info, `auth.getMobileSession` (POST,
    MD5 `api_sig` over sorted params + secret), `track.scrobble` and `track.updateNowPlaying`;
    API key/secret injected via `BuildConfig.LASTFM_API_KEY`/`LASTFM_SECRET` and gated by the
    `enable_lastfm_integration` resValue.
  - `ListenBrainzService`: token validation (GET) and `submit-listens` (POST, `Authorization`
    header, `additional_info` records the media player).
- Store credentials encrypted: `CryptoUtil` generates an AES-GCM key in the Android Keystore
  (`SsilencceCryptoKey`), encrypts with a random 12-byte IV and persists
  `Base64(IV || ciphertext)` in SharedPreferences (`LASTFM_LOGIN` / `LISTENBRAINZ_LOGIN`).
- Model login state as a sealed machine (`LoginState`: Empty/LoggingIn/LoggedIn/Failure) and
  scrobble outcomes as sealed results (`ScrobblingResult`), both surfaced through
  `NetworkRepository` flows consumed by the Compose login bottom sheet and settings.
- Dispatch scrobbling through `Repository` (ADR-010) so `PlaybackService` stays service-agnostic
  (`ScrobblingService` enum: Lastfm/ListenBrainz).

## Consequences

- Secrets at rest are protected by the hardware-backed Keystore; a plaintext dump of
  SharedPreferences yields only ciphertext.
- Last.fm signature handling (MD5 `api_sig`) is contained in one service; the shared secret
  never leaves the build-injected constants.
- Credentials are tied to the Keystore alias: after a device migration or key invalidation,
  the user must log in again (a documented trade-off of secure storage).
- PlaybackService scrobbling is best-effort and network-gated by `NetworkFeature`; failures
  surface as `ScrobblingResult.Failure` without interrupting playback.
