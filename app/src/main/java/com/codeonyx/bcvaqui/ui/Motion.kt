package com.codeonyx.bcvaqui.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/** Un solo ViewModel por Activity: evita recargar tasas al cambiar de pestaña. */
@Composable
fun appViewModel(): MainViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}

object Motion {
    const val Fast = 180
    const val Normal = 260
    const val Slow = 340

    val Ease = FastOutSlowInEasing

    fun tabEnter() = fadeIn(tween(Normal, easing = Ease)) +
        scaleIn(initialScale = 0.985f, animationSpec = tween(Normal, easing = Ease))

    fun tabExit() = fadeOut(tween(Fast, easing = Ease))

    fun toolEnter() = slideInHorizontally(
        animationSpec = tween(Normal, easing = Ease),
        initialOffsetX = { it / 8 }
    ) + fadeIn(tween(Normal, easing = Ease))

    fun toolExit() = fadeOut(tween(Fast, easing = Ease))

    fun toolPopEnter() = fadeIn(tween(Normal, easing = Ease))

    fun toolPopExit() = slideOutHorizontally(
        animationSpec = tween(Normal, easing = Ease),
        targetOffsetX = { it / 8 }
    ) + fadeOut(tween(Fast, easing = Ease))

    fun titleTransform(): ContentTransform =
        (fadeIn(tween(Fast, easing = Ease)) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(Fast, easing = Ease)
        )) togetherWith fadeOut(tween(120, easing = Ease))
}

val ResultSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
