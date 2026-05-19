// 오버레이 설정 저장소 — 타이핑 멘트, 카운트다운 시간
package com.futuresjournal.app.util

import android.content.Context
import org.json.JSONArray

object OverlayPrefs {

    private const val PREFS = "overlay_settings"
    private const val KEY_SENTENCE = "selected_sentence"
    private const val KEY_CUSTOM = "custom_sentences"
    private const val KEY_COUNTDOWN = "countdown_seconds"

    const val DEFAULT_SENTENCE = "나는 충동이 아닌 원칙으로 매매할 것을 맹세한다"

    val DEFAULT_SENTENCES = listOf(
        "나는 원칙 없는 매매를 하지 않겠다",
        "나는 오늘도 원칙을 지킬 것을 맹세한다",
        "나는 충동이 아닌 계획으로 매매할 것을 맹세한다",
        DEFAULT_SENTENCE,
        "나는 이 순간의 감정보다 내일의 나를 선택하겠다",
        "나는 흔들리지 않는 트레이더가 될 것을 맹세한다",
        "두려움도 탐욕도 아닌 오직 원칙만이 나를 이끈다",
        "한 번의 절제가 나를 살아남게 한다",
        "나는 실수를 다시 반복하지 않겠다",
        "나는 도파민이 아닌 원칙으로 진입한다",
        "원칙이 나를 지킨다",
        "멈추는 것도 실력이다",
        "오늘의 절제가 내일의 수익이다"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSelectedSentence(context: Context): String =
        prefs(context).getString(KEY_SENTENCE, DEFAULT_SENTENCE) ?: DEFAULT_SENTENCE

    fun setSelectedSentence(context: Context, sentence: String) =
        prefs(context).edit().putString(KEY_SENTENCE, sentence).apply()

    fun getCustomSentences(context: Context): List<String> {
        val json = prefs(context).getString(KEY_CUSTOM, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomSentence(context: Context, sentence: String) {
        val list = getCustomSentences(context).toMutableList()
        if (!list.contains(sentence)) {
            list.add(sentence)
            prefs(context).edit().putString(KEY_CUSTOM, JSONArray(list).toString()).apply()
        }
    }

    fun removeCustomSentence(context: Context, sentence: String) {
        val list = getCustomSentences(context).toMutableList()
        list.remove(sentence)
        prefs(context).edit().putString(KEY_CUSTOM, JSONArray(list).toString()).apply()
    }

    fun getAllSentences(context: Context): List<String> =
        DEFAULT_SENTENCES + getCustomSentences(context)

    fun getCountdownSeconds(context: Context): Int =
        prefs(context).getInt(KEY_COUNTDOWN, 10)

    fun setCountdownSeconds(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_COUNTDOWN, seconds.coerceAtLeast(5)).apply()
}
