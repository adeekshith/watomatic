package com.parishod.watomatic.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.parishod.watomatic.R

/**
 * Common utility for showing unsaved changes dialog across the app.
 * Used to confirm user's intent to discard changes when navigating away.
 */
object UnsavedChangesDialog {
    /**
     * Show a dialog to confirm discarding unsaved changes
     *
     * @param context The context to show the dialog in
     * @param onDiscard Callback invoked when user chooses to discard changes
     * @param onStay Callback invoked when user chooses to stay on the page (optional)
     */
    fun show(
        context: Context,
        onDiscard: () -> Unit,
        onStay: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_changes_message)
            .setPositiveButton(R.string.discard_changes) { dialog, _ ->
                dialog.dismiss()
                onDiscard()
            }
            .setNegativeButton(R.string.stay_on_page) { dialog, _ ->
                dialog.dismiss()
                onStay?.invoke()
            }
            .show()
    }
}

