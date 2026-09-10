package com.linguakey.keyboard

import android.view.inputmethod.InputConnection

/** Present a syllable transition as one editor update, without an intermediate frame. */
internal object CompositionEdits {
    fun apply(connection: InputConnection, result: HangulComposer.Result) {
        connection.beginBatchEdit()
        try {
            if (result.commit.isNotEmpty()) connection.commitText(result.commit, 1)
            connection.setComposingText(result.composing, 1)
            if (result.composing.isEmpty()) connection.finishComposingText()
        } finally {
            connection.endBatchEdit()
        }
    }
}
