# Killmon Music 0.7.10 (75) — fix crash al abrir de 0.7.9

## Causa raíz (alta confianza; no hay logcat)
`NextTrackPrefetcher` (core, 0.7.9) pedía sus cachés como `Cache` (interfaz). Koin registra
`single<SimpleCache>(named(PLAYER_CACHE/DOWNLOAD_CACHE))` y busca por clase exacta, así que
`get(named(PLAYER_CACHE))` con tipo esperado `Cache` lanza `NoDefinitionFoundException` al crear
el adapter del reproductor (arranque del servicio de media). Todos los demás consumidores usan `SimpleCache`.
Evidencia: diff 1290b3d..375e7fe / afc15d1..9aad09f; test `KoinSupertypeLookupTest` demuestra que Koin 4.2.2 no resuelve el supertipo.
## Fix
- core: `NextTrackPrefetcher` recibe `SimpleCache` (misma clase que registra el módulo).
- Tests: KoinSupertypeLookupTest, PrefetcherCacheTypeTest.
- versión 0.7.10 / 75. Se mantienen swipe en Recientes, prefetch de 2 siguientes y Home Spotify.
