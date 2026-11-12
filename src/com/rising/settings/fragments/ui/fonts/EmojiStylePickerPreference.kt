/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package com.rising.settings.fragments.ui.fonts

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.FontManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.R
import com.android.settings.utils.SystemRestartUtils as SystemUiRestartUtils
import com.android.settingslib.development.SystemPropPoker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmojiStylePickerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {
    private var dialog: AlertDialog? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val installer by lazy { ExternalEmojiInstaller(context) }

    private var customName: String = ""

    private var emojiPickerLauncher: ActivityResultLauncher<Array<String>>? = null

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        applySummary()
        Thread({
            installer.dropLegacyInstall()
            refreshState()
        }, "EmojiState").start()

        val activity = context as? FragmentActivity ?: return
        emojiPickerLauncher = activity.activityResultRegistry.register(
            "emoji_picker_${key}",
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { handleEmojiUriSelected(it, activity) }
        }
    }

    override fun onDetached() {
        dialog?.dismiss()
        dialog = null
        emojiPickerLauncher?.unregister()
        emojiPickerLauncher = null
        super.onDetached()
    }

    override fun onClick() {
        val ctx = context
        dialog?.dismiss()

        val labels = entries.map { it.toString() }
        val values = entryValues.map { it.toString() }

        val hasCustom = customName.isNotEmpty()
        val offset = if (hasCustom) 1 else 0
        val current = SystemProperties.get(key, DEFAULT_STYLE)
        val items = buildList {
            if (hasCustom) add(customLabel(ctx))
            addAll(labels)

            if (hasCustom) add(ctx.getString(R.string.emoji_apps_reset))
        }
        val checked = if (hasCustom) 0 else values.indexOf(current).coerceAtLeast(0)

        dialog = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), checked) { d, index ->

                if (hasCustom && index == 0) return@setSingleChoiceItems
                d.dismiss()
                if (hasCustom && index == items.lastIndex) {
                    confirmResetApps(ctx)
                    return@setSingleChoiceItems
                }
                val value = values[index - offset]
                if (!hasCustom && value == current) return@setSingleChoiceItems
                applyStyle(ctx, value, hadCustom = hasCustom)
            }
            .setNeutralButton(R.string.emoji_add_font) { _, _ ->
                emojiPickerLauncher?.launch(
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-otf",
                        "application/octet-stream",
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { dialog = null }
            .show()
    }

    private fun applyStyle(ctx: Context, value: String, hadCustom: Boolean) {
        Thread({
            if (hadCustom) {
                val result = installer.clear()
                if (result != ExternalEmojiInstaller.Result.SUCCESS) {
                    mainHandler.post { showResult(result) }
                    return@Thread
                }
            }
            SystemProperties.set(key, value)
            SystemPropPoker.getInstance().poke()
            mainHandler.post {
                if (hadCustom) customName = ""
                updateSummary()
                if (hadCustom) {
                    showRebootDialog(ctx)
                } else {
                    SystemRestartUtils.showSystemRestartDialog(ctx)
                }
            }
        }, "EmojiStyle").start()
    }

    private fun customLabel(ctx: Context): String =
        ctx.getString(R.string.emoji_custom_label, customName)

    private fun applySummary() {
        summary = if (customName.isNotEmpty()) {
            customLabel(context)
        } else {
            val values = entryValues.map { it.toString() }
            val index = values.indexOf(SystemProperties.get(key, DEFAULT_STYLE))
            entries[if (index < 0) 0 else index]
        }
    }

    private fun updateSummary() {
        applySummary()
        Thread({ refreshState() }, "EmojiState").start()
    }

    private fun refreshState() {
        val name = installer.installedName()
        mainHandler.post {
            customName = name
            applySummary()
        }
    }

    private fun choiceDialog(
        ctx: Context,
        title: String,
        message: String,
        confirmText: String,
        laterText: String,
        onConfirm: () -> Unit,
    ) {
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmText) { _, _ -> onConfirm() }
            .setNegativeButton(laterText, null)
            .show()
    }

    private fun showRebootDialog(ctx: Context) = choiceDialog(
        ctx,
        title = ctx.getString(R.string.emoji_reboot_title),
        message = ctx.getString(R.string.emoji_reboot_message),
        confirmText = ctx.getString(R.string.emoji_reboot_now),
        laterText = ctx.getString(R.string.emoji_later),
        onConfirm = { ExternalFontInstaller.rebootDevice() },
    )

    private fun handleEmojiUriSelected(uri: Uri, activity: FragmentActivity) {
        activity.lifecycleScope.launch {
            val typeface = installer.loadTypefaceFromUri(uri)
            if (typeface == null) {
                toast(R.string.emoji_toast_invalid)
                return@launch
            }
            val problem = installer.previewProblem()
            if (problem != null) {
                installer.discardPreview()
                showResult(problem)
                return@launch
            }
            showEmojiPreviewDialog(typeface, installer.previewCoverage())
        }
    }

    private fun showEmojiPreviewDialog(typeface: Typeface, coverage: Pair<Int, Int>) {
        val ctx = context
        val pad = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, ctx.resources.displayMetrics
        ).toInt()
        val sample = TextView(ctx).apply {
            text = PREVIEW_EMOJI
            this.typeface = typeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val info = TextView(ctx).apply {
            text = ctx.getString(R.string.emoji_preview_coverage, coverage.first, coverage.second)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(sample)
            addView(info)
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.emoji_preview_title)
            .setView(content)
            .setPositiveButton(R.string.emoji_add_font) { _, _ -> installEmojiFont() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> installer.discardPreview() }
            .setOnCancelListener { installer.discardPreview() }
            .show()
    }

    private fun installEmojiFont() {
        ExternalEmojiInstaller.scope.launch {
            val result = installer.installPreview()
            updateSummary()
            if (result == ExternalEmojiInstaller.Result.SUCCESS) {
                if (isScreenAlive()) showRestartDialog(context)
            } else {
                showResult(result)
            }
        }
    }

    private fun isScreenAlive(): Boolean {
        val activity = context as? Activity ?: return false
        return !activity.isFinishing && !activity.isDestroyed
    }

    private fun showRestartDialog(ctx: Context) = choiceDialog(
        ctx,
        title = ctx.getString(R.string.emoji_restart_title),
        message = ctx.getString(R.string.emoji_restart_message),
        confirmText = ctx.getString(R.string.emoji_restart_now),
        laterText = ctx.getString(R.string.emoji_later),
        onConfirm = { restartNow(ctx) },
    )

    private fun restartNow(ctx: Context) {
        Thread({
            installer.killRunningApps()
            mainHandler.post { SystemUiRestartUtils.restartSystemUI(ctx) }
        }, "EmojiRestart").start()
    }

    private fun confirmResetApps(ctx: Context) = choiceDialog(
        ctx,
        title = ctx.getString(R.string.emoji_apps_reset),
        message = ctx.getString(R.string.emoji_apps_confirm),
        confirmText = ctx.getString(R.string.emoji_apps_reset_button),
        laterText = ctx.getString(android.R.string.cancel),
        onConfirm = { resetApps() },
    )

    private fun resetApps() {
        ExternalEmojiInstaller.scope.launch {
            val count = withContext(Dispatchers.IO) { installer.applyToApps() }
            when {
                count >= 0 -> Toast.makeText(
                    context,
                    context.resources.getQuantityString(R.plurals.emoji_apps_done, count, count),
                    Toast.LENGTH_LONG
                ).show()
                count == FontManager.RESULT_ERROR_FONT_NOT_FOUND ->
                    toast(R.string.emoji_toast_not_installed)
                else -> toast(R.string.emoji_toast_failed)
            }
        }
    }

    private fun showResult(result: ExternalEmojiInstaller.Result) {
        when (result) {
            ExternalEmojiInstaller.Result.SUCCESS -> Unit
            ExternalEmojiInstaller.Result.INVALID_FONT -> toast(R.string.emoji_toast_invalid)
            ExternalEmojiInstaller.Result.UPDATER_DISABLED ->
                toast(R.string.emoji_toast_updater_disabled)
            ExternalEmojiInstaller.Result.NOT_INSTALLED -> toast(R.string.emoji_toast_not_installed)
            ExternalEmojiInstaller.Result.FAILED -> toast(R.string.emoji_toast_failed)
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val DEFAULT_STYLE = "android"

        private const val PREVIEW_EMOJI =
            "\uD83D\uDE00\uD83D\uDE0D\uD83C\uDF89\u2764\uFE0F\uD83D\uDC4D"
    }
}
