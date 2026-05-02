package com.chatty.android.etc

import android.graphics.Typeface
import android.os.Handler
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.widget.EditText

object TextUtility {
    val TAG ="TextUtility"
    val SENTENCE_REGEX = """(?<=[.!?*])\s+""".toRegex()
    val SENTENCE_REGEX_FIRSTLETTER = """(?<=[.!?]\\s|^|•\\s)[a-z]""".toRegex()
    const val BULLET_CHAR = "•"
    const val BULLET_CHAR_WITH_SPACE = "• "
    const val BULLET_CHAR_WITH_TWOSPACE = "•  "

    fun findSentenceStart(text: CharSequence, position: Int): Int {
        var start = position
        while (start > 0 && !text[start - 1].toString().matches(SENTENCE_REGEX)) {
            start--
        }
        return start
    }

    fun findSentenceEnd(text: CharSequence, position: Int): Int {
        var end = position
        val length = text.length
        while (end < length && !text[end].toString().matches(SENTENCE_REGEX)) {
            end++
        }
        return end
    }


    fun formatPromptPrettyLike(s1: String): String {
        var formattedString = s1.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if ("what" in formattedString || "who" in formattedString || "why" in formattedString || "where" in formattedString) {
            if (!formattedString.endsWith("?")) {
                formattedString += "?"
            }
        }
        return formattedString
    }

    fun capitalizeFirstLetterOfEverySentence(text: String):String {
        val sentences = text.split(SENTENCE_REGEX)
        val capitalizedSentences = sentences.joinToString("") { sentence ->
            if (sentence.isNotEmpty()) {
                sentence.substring(0, 1).uppercase() + sentence.substring(1)
            } else {
                ""
            }
        }
        return capitalizedSentences
    }

    fun capitalizeFirstLetterOfEverySentence(textEdit: EditText) {
        val textBefore = textEdit.text.toString()
        val textAfter = capitalizeFirstLetterOfEverySentence(textBefore)
        if (textBefore != textAfter) {textEdit.setText(textAfter)}
    }

    fun newParagraph(textEdit: EditText) {
        textEdit.isEnabled = false
        val start = textEdit.selectionStart
        var currentPosition = textEdit.selectionEnd
        if (start != currentPosition) { textEdit.setSelection(currentPosition) }
        textEdit.setSelection(currentPosition)
        val editableText = textEdit.editableText
        editableText.insert(currentPosition, "\n")
        currentPosition+=1
        if (currentPosition >= textEdit.text.length) {currentPosition = textEdit.text.length-1}
        textEdit.isEnabled = true
        textEdit.requestFocus()
        textEdit.setSelection(currentPosition)
    }

    fun newBulletItem(textEdit: EditText) {
        textEdit.isEnabled = false
        var currentPosition: Int = textEdit.selectionEnd
        if (textEdit.selectionStart != currentPosition) { textEdit.setSelection(currentPosition) }
        var editableText = textEdit.editableText
        var textBeforeCursor = editableText.subSequence(0, currentPosition).toString()
        var sentenceStart = textBeforeCursor.lastIndexOfAny(charArrayOf('.', '!', '?', '\n')) + 1
        if (sentenceStart > 0 && textBeforeCursor[sentenceStart - 1] != '\n') {
            editableText.insert(sentenceStart, "\n")
            sentenceStart += 1
            currentPosition += 1
        }
        if (editableText.startsWith(" ", sentenceStart)) {
            editableText.insert(sentenceStart, BULLET_CHAR)
            sentenceStart +=2
            currentPosition +=1

        } else if (!editableText.startsWith(BULLET_CHAR, sentenceStart)) {
            editableText.insert(sentenceStart, BULLET_CHAR_WITH_SPACE)
            sentenceStart +=2
            currentPosition +=2
        }
        if (sentenceStart <  textEdit.text.length) {
            if (editableText[sentenceStart].isLetter()) {
                editableText.replace(sentenceStart, sentenceStart + 1, editableText[sentenceStart].uppercaseChar().toString())
            }
        }
        if (currentPosition >= textEdit.text.length) {currentPosition = textEdit.text.length-1}
        textEdit.isEnabled = true
        textEdit.requestFocus()
        //Log.d(TAG, "setting position... ${currentPosition}")
        textEdit.setSelection(currentPosition)
    }
    fun applyMarkdownSpans(text: String): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        val pattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
        var lastEnd = 0
        pattern.findAll(text).forEach { match ->
            ssb.append(text.substring(lastEnd, match.range.first))
            val isBold = match.groupValues[1].isNotEmpty()
            val inner = if (isBold) match.groupValues[1] else match.groupValues[2]
            val spanStart = ssb.length
            ssb.append(inner)
            ssb.setSpan(
                StyleSpan(if (isBold) Typeface.BOLD else Typeface.ITALIC),
                spanStart, ssb.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            lastEnd = match.range.last + 1
        }
        ssb.append(text.substring(lastEnd))
        return ssb
    }

    fun cleanText(text:String):String{
        var result = text.replace("**", "")
        result = result.replace("*", "")
        return result
    }
    fun formatHTMLMarkup(text:String):String{
        val HTMLStartTag = "<!DOCTYPE html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "<style> body {font-size: 8px; font-family: sans-serif;  } div { background-color: black; color: white;} </style>\n</head><body>"
        val HTMLEndTag = "</body> </html>"
        val ticks = "\n```"
        var result = text.replace("\n", "<BR>")
        while (result.contains(ticks)) {
            result = result.replaceFirst(ticks, "<pre><div>")
            result = result.replaceFirst(ticks, "</div></pre>")
        }
        result = cleanText(result)
        return HTMLStartTag + result + HTMLEndTag
    }

    class TypingTextWatcher(private val onTypingStopped: () -> Unit) : TextWatcher {
        private val handler = Handler(android.os.Looper.getMainLooper())
        private val delayMillis: Long = 2000 // 2 seconds
        private var runnable: Runnable? = null

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            runnable?.let { handler.removeCallbacks(it) }
            runnable = Runnable {
                onTypingStopped.invoke()
            }
            handler.postDelayed(runnable!!, delayMillis)
        }
    }
}