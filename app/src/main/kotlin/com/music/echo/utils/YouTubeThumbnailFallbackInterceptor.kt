package echo.music.iad1tya.utils

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import echo.music.iad1tya.ui.utils.ytimgFallbackUrl

/**
 * Retries a failed YouTube video thumbnail with the always-available variant.
 *
 * maxresdefault / sddefault do not exist for many videos. Older builds stored
 * those URLs in the database and in queued media items, so without this the
 * affected songs never showed a cover (in the app, the notification or on
 * Android Auto). Every request goes through the singleton loader, so this covers
 * all of them.
 */
class YouTubeThumbnailFallbackInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val result = chain.proceed()
    if (result !is ErrorResult) return result
    val fallback = ytimgFallbackUrl(chain.request.data.toString()) ?: return result
    return chain.withRequest(chain.request.newBuilder().data(fallback).build()).proceed()
  }
}
