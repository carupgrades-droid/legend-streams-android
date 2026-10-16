# Legend Streams — Android starter (Step 5: first openly licensed playable film)

Original Android streaming-app starter built with Kotlin, Jetpack Compose, AndroidX Media3, Coil, and TMDB metadata APIs.

## Implemented
- Legend Streams original dark UI
- Home, Movies, Series, Live TV, Search, My List
- TMDB API client for trending titles, popular movies/series, and multi-search (non-commercial developer use only unless separately approved)
- Internet Archive advanced-search integration for movie/video candidates, with links to the source item and its license information
- Live TV provider placeholder and documented partner onboarding path; no unauthorized channel list or stream scraping
- TMDB poster artwork and overview metadata in catalog rows
- API key supplied through a Gradle property rather than committed into source code
- Media3/ExoPlayer playback for HLS and regular MP4 URLs
- Curated Big Buck Bunny sample film from Blender Foundation, with CC BY 3.0 attribution and source/license link
- Explicit distinction between metadata and licensed playback content
- Placeholder screen explaining that subscription billing and advertising are not connected


## First openly licensed playable title
The Movies tab includes **Big Buck Bunny** as a curated playback test from the Blender Foundation. The film is released under Creative Commons Attribution 3.0; the app displays attribution and links to the official project page. Review the license and required credit before redistribution or monetization: https://peach.blender.org/about/.

Playback URL used by this prototype: `https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4`. It is an externally hosted MP4, so availability and performance depend on the host. This validates the player path only; it is not a commercial catalog agreement.

## Free movie candidates (Internet Archive)
- The Movies tab searches Internet Archive metadata and combines candidate items with TMDB movie metadata when TMDB is configured.
- Candidate cards link to their Internet Archive item page so the source's license and rights information can be reviewed.
- The app intentionally does not auto-play Internet Archive search results: Internet Archive warns that it cannot guarantee uploader-provided copyright status. Review rights item-by-item and obtain permission where required before enabling in-app commercial playback. See https://archivesupport.zendesk.com/hc/en-us/articles/360014759692-Rights and https://archivesupport.zendesk.com/hc/en-us/articles/360017808151-Movies-and-Videos-A-Basic-Guide.
- Internet Archive API documentation: https://doc-tools.readthedocs.io/en/ia-test-gsod/item-search-apis.html

## Modern movies and series

1. Create a TMDB account and request an API key for your application at https://www.themoviedb.org/settings/api.
2. Add the following line to your **user-level** Gradle properties file (recommended, not this repository's tracked files):
   `TMDB_API_KEY=your_tmdb_v3_api_key`
3. In Android Studio, sync Gradle and run the app.
4. If the key is absent or rejected, the app displays a catalog setup/error message and keeps the demo playback test available.

TMDB metadata access is subject to TMDB's terms, attribution, and branding requirements. Review https://www.themoviedb.org/terms-of-use and https://developer.themoviedb.org/docs/ before distribution. This product uses the TMDB API but is not endorsed or certified by TMDB. The TMDB API does **not** grant rights to stream films, TV episodes, or live channels.

## Live TV and modern licensed catalogs
- There is no universal free API that grants rights to stream current studio movies, full TV catalogs, or live channels inside a third-party app.
- For modern titles, obtain a written content/API agreement with a distributor or streaming provider. TMDB metadata access is separate from streaming rights, and its published API terms require a separate written agreement for commercial use: https://www.themoviedb.org/api-terms-of-use.
- For live TV, use a provider that authorizes distribution in Legend Streams. Pluto TV has a partner/distribution inquiry page at https://pluto.tv/partners; this is a partner application, not a public playback API or guarantee of approval.
- Ads and subscriptions are deliberately postponed until playback rights and provider agreements are settled.

## Build
1. Install Android Studio (current stable) and JDK 17.
2. Open this folder in Android Studio.
3. Configure `TMDB_API_KEY` in user-level Gradle properties.
4. Allow Gradle sync and dependency downloads.
5. Select Build > Build APK(s).
6. Test on an Android device/emulator.

## Playback test note
The `HLS Player Test` entry uses a public stream published for testing:
`https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`

It is a technical playback test only, not a movie catalog, a guarantee of uptime, or a commercial content license. Replace it with stream URLs supplied by a provider that has authorized distribution rights before release.

## Not production-ready yet
No licensed film/TV catalog or live TV provider, ad network, Google Play Billing, backend, authentication, durable watchlist, DRM configuration, parental controls, Android TV remote/focus support, or Play Store release configuration is included. The watchlist currently lives only in app memory. The TMDB API key embedded in a client app should be treated as public and restricted appropriately; use a backend for private credentials and higher-risk provider integrations.

## Next integration sequence
1. Test Big Buck Bunny playback on a real Android device; confirm the attribution remains visible and the remote MP4 host is reachable.
2. Curate and review additional free movie candidates, recording license evidence and commercial permission where needed.
3. Obtain a commercial metadata license if TMDB will be used in the monetized app.
4. Add a licensed VOD/live provider. Use provider-approved HLS/DASH endpoints, DRM and authorization headers.
3. Add Google Mobile Ads or another approved ad network with consent and privacy disclosures.
4. Add Google Play Billing and server-side purchase verification.
5. Persist user preferences and watchlist safely.
6. Implement Android TV launcher/Leanback support, D-pad focus, and remote testing.
7. Test build and playback on real devices; complete app signing, privacy policy, data-safety, and rights review.

## Build an APK with GitHub Actions (no local Android Studio required)

This repository includes `.github/workflows/android-build.yml`. After you upload/push this project to a GitHub repository:

1. Open the repository on GitHub and select **Actions**. If prompted, enable GitHub Actions for the repository.
2. Push to `main` or `master`, open a pull request into either branch, or manually run **Build Legend Streams APK** from the Actions tab using **Run workflow**.
3. Open the completed workflow run.
4. Under **Artifacts**, download `legend-streams-debug-apk` and unzip it to get `app-debug.apk`.
5. Transfer the APK to your Android phone and install it. Android may ask you to allow installation from that source. Only install APKs you trust.

### Optional TMDB key for GitHub builds

The app can build without a TMDB key; TMDB-powered catalog requests will not work until a valid key is configured. To add one without committing it to source:

1. In your GitHub repository, open **Settings → Secrets and variables → Actions**.
2. Choose **New repository secret**.
3. Name it `TMDB_API_KEY` and enter your key as the secret value.
4. Re-run the workflow.

Do not paste API keys into committed source files. Confirm TMDB's current terms and commercial-use permissions before distributing a monetized app.

### Build setup details

The workflow uses Java 17 and Gradle 8.9, compatible with this project's Android Gradle Plugin 8.7.3, and uploads the debug APK as a workflow artifact. It builds an **unsigned debug APK for testing**, not a Play Store-signed production release. The first run may take several minutes while Android/Gradle dependencies are downloaded. The build has not been run in this chat environment; GitHub Actions will report the actual build result and logs.
