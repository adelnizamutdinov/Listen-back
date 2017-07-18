package adeln

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

fun videoInfoUrl(videoId: String): HttpUrl =
    HttpUrl.parse("http://www.youtube.com/get_video_info")!!.newBuilder()
        .setQueryParameter("video_id", videoId)
        .build()

fun videoInfoRequest(videoId: String): Request =
    Request.Builder()
        .url(videoInfoUrl(videoId))
        .build()

fun equalsPair(s: String): Pair<String, String> =
    s.split("=").let { (a, b) -> a to URLDecoder.decode(b, "utf-8") }

data class Audio(
    val type: String,
    val url: String,
    val bitrate: Float,
    val lengthSeconds: Long
)

fun Audio.sizeBytes(): Long =
    (lengthSeconds * bitrate / 8F).toLong()

fun Audio.lengthMillis(): Long =
    lengthSeconds * 1000L

fun audio(client: OkHttpClient, videoId: String): Audio? =
    client.newCall(videoInfoRequest(videoId))
        .execute()
        .body()!!
        .string()
        .split("&")
        .asSequence()
        .map(::equalsPair)
        .toMap()
        .let { map ->
            map["adaptive_fmts"]
                ?.split(",")
                ?.asSequence()
                ?.map {
                    val m = it.split("&")
                        .asSequence()
                        .map(::equalsPair)
                        .toMap()

                    Audio(
                        type = m["type"]!!,
                        url = m["url"]!!,
                        bitrate = m["bitrate"]!!.toFloat(),
                        lengthSeconds = map["length_seconds"]!!.toLong()
                    )
                }
                ?.filter { "audio/webm" in it.type }
                ?.maxBy { it.bitrate }
        }
