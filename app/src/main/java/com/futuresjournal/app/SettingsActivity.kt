// 오버레이 설정 화면 — 타이핑 멘트 선택/추가/삭제, 카운트다운 시간 설정
package com.futuresjournal.app

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.futuresjournal.app.util.OverlayPrefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvSelected: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        tvSelected = findViewById(R.id.tv_selected_sentence)
        val inputCountdown = findViewById<EditText>(R.id.input_countdown)

        inputCountdown.setText(OverlayPrefs.getCountdownSeconds(this).toString())
        refreshSelected()

        findViewById<Button>(R.id.btn_save_countdown).setOnClickListener {
            val secs = inputCountdown.text.toString().toIntOrNull()
            if (secs == null || secs < 5) {
                Toast.makeText(this, "5 이상 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
            } else {
                OverlayPrefs.setCountdownSeconds(this, secs)
                Toast.makeText(this, "${secs}초로 저장됐습니다", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_open_sentence_list).setOnClickListener {
            showSentenceDialog()
        }
    }

    private fun refreshSelected() {
        tvSelected.text = OverlayPrefs.getSelectedSentence(this)
    }

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun showSentenceDialog() {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(4), px(8), px(4), px(8))
        }
        scroll.addView(container)

        val dialog = AlertDialog.Builder(this)
            .setTitle("멘트 선택")
            .setView(scroll)
            .create()

        fun rebuild() {
            container.removeAllViews()
            val selected = OverlayPrefs.getSelectedSentence(this)
            val defaults = OverlayPrefs.DEFAULT_SENTENCES.toSet()

            OverlayPrefs.getAllSentences(this).forEach { sentence ->
                val isSelected = sentence == selected

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(px(12), px(12), px(12), px(12))
                    setBackgroundColor(if (isSelected) 0x22FFFFFF else 0x00000000)
                }

                val tv = TextView(this).apply {
                    text = sentence
                    setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt())
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = px(8)
                    }
                    setOnClickListener {
                        OverlayPrefs.setSelectedSentence(this@SettingsActivity, sentence)
                        refreshSelected()
                        dialog.dismiss()
                    }
                }
                row.addView(tv)

                if (!defaults.contains(sentence)) {
                    val deleteBtn = TextView(this).apply {
                        text = "삭제"
                        setTextColor(0xFFFF5252.toInt())
                        textSize = 13f
                        setPadding(px(8), px(4), px(8), px(4))
                        setOnClickListener {
                            OverlayPrefs.removeCustomSentence(this@SettingsActivity, sentence)
                            if (OverlayPrefs.getSelectedSentence(this@SettingsActivity) == sentence) {
                                OverlayPrefs.setSelectedSentence(this@SettingsActivity, OverlayPrefs.DEFAULT_SENTENCE)
                                refreshSelected()
                            }
                            rebuild()
                        }
                    }
                    row.addView(deleteBtn)
                }

                container.addView(row)

                val divider = android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(0xFF333333.toInt())
                }
                container.addView(divider)
            }

            // 커스텀 멘트 추가 버튼
            val addBtn = Button(this).apply {
                text = "＋ 커스텀 멘트 추가"
                setTextColor(0xFFFFFFFF.toInt())
                backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFD32F2F.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = px(16) }
                setOnClickListener { showAddSentenceDialog { rebuild() } }
            }
            container.addView(addBtn)
        }

        rebuild()
        dialog.show()
    }

    private fun showAddSentenceDialog(onAdded: () -> Unit) {
        val input = EditText(this).apply {
            hint = "새 멘트를 입력하세요"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(px(20), px(16), px(20), px(8))
        }
        AlertDialog.Builder(this)
            .setTitle("커스텀 멘트 추가")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val text = input.text.toString().trim()
                when {
                    text.isEmpty() -> Toast.makeText(this, "멘트를 입력하세요", Toast.LENGTH_SHORT).show()
                    OverlayPrefs.getAllSentences(this).contains(text) ->
                        Toast.makeText(this, "이미 존재하는 멘트입니다", Toast.LENGTH_SHORT).show()
                    else -> {
                        OverlayPrefs.addCustomSentence(this, text)
                        onAdded()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
