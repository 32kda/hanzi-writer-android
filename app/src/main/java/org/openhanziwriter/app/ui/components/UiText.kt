package org.openhanziwriter.app.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(
    @StringRes val resId: Int,
    val args: List<Any> = emptyList()
) {
    constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())

    fun resolve(context: Context): String =
        if (args.isEmpty()) context.getString(resId)
        else context.getString(resId, *args.toTypedArray())
}

@Composable
fun UiText.resolve(): String =
    if (args.isEmpty()) stringResource(resId)
    else stringResource(resId, *args.toTypedArray())
