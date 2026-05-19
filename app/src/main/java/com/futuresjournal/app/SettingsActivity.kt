// 오버레이 설정 화면 — 타이핑 멘트 선택/추가, 카운트다운 시간 설정
package com.futuresjournal.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.futuresjournal.app.util.OverlayPrefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var sentenceList: LinearLayout
    private lateinit var inputCountdown: EditText
    private lateinit var inputCustom: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = "오버레이 설정"

        sentenceList = findViewById(R.id.sentence_list)
        inputCountdown = findViewById(R.id.input_countdown)
        inputCustom = findViewById(R.id.input_custom_sentence)

        inputCountdown.setText(OverlayPrefs.getCountdownSeconds(this).toString())

        findViewById<Button>(R.id.btn_save_countdown).setOnClickListener {
            val secs = inputCountdown.text.toString().toIntOrNull()
            if (secs == null || secs < 5) {
                Toast.makeText(this, "5 이상 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
            } else {
                OverlayPrefs.setCountdownSeconds(this, secs)
                Toast.makeText(this, "${secs}초로 저장됐습니다", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_add_sentence).setOnClickListener {
            val text = inputCustom.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "멘트를 입력하세요", Toast.LENGTH_SHORT).show()
            } else if (OverlayPrefs.getAllSentences(this).contains(text)) {
                Toast.makeText(this, "이미 존재하는 멘트입니다", Toast.LENGTH_SHORT).show()
            } else {
                OverlayPrefs.addCustomSentence(this, text)
                inputCustom.text.clear()
                refreshSentenceList()
            }
        }

        refreshSentenceList()
    }

    private fun refreshSentenceList() {
        sentenceList.removeAllViews()
        val selected = OverlayPrefs.getSelectedSentence(this)
        val defaults = OverlayPrefs.DEFAULT_SENTENCES.toSet()
        val all = OverlayPrefs.getAllSentences(this)

        all.forEach { sentence ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val radio = RadioButton(this).apply {
                text = sentence
                isChecked = sentence == selected
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    OverlayPrefs.setSelectedSentence(this@SettingsActivity, sentence)
                    refreshSentenceList()
                }
            }
            row.addView(radio)

            // 커스텀 멘트에만 삭제 버튼 표시
            if (!defaults.contains(sentence)) {
                val deleteBtn = Button(this).apply {
                    text = "삭제"
                    setTextColor(0xFFFF5252.toInt())
                    backgroundTintList = null
                    background = null
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        OverlayPrefs.removeCustomSentence(this@SettingsActivity, sentence)
                        if (OverlayPrefs.getSelectedSentence(this@SettingsActivity) == sentence) {
                            OverlayPrefs.setSelectedSentence(this@SettingsActivity, OverlayPrefs.DEFAULT_SENTENCE)
                        }
                        refreshSentenceList()
                    }
                }
                row.addView(deleteBtn)
            }

            sentenceList.addView(row)

            // 항목 사이 구분선
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(0xFF333333.toInt())
            }
            sentenceList.addView(divider)
        }
    }
}
