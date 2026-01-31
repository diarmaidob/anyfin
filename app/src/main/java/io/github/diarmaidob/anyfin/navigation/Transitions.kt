package io.github.diarmaidob.anyfin.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

object Transitions {
    private val animSpec = tween<IntOffset>(300)

    private fun getLateralDirection(initial: NavBackStackEntry, target: NavBackStackEntry): AnimatedContentTransitionScope.SlideDirection? {
        val initialTab = AppTab.getTabOwner(initial.destination)
        val targetTab = AppTab.getTabOwner(target.destination)

        if (initialTab != null && targetTab != null && initialTab != targetTab) {
            return if (targetTab.ordinal > initialTab.ordinal)
                AnimatedContentTransitionScope.SlideDirection.Left
            else
                AnimatedContentTransitionScope.SlideDirection.Right
        }
        return null
    }

    fun enter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        val lateral = getLateralDirection(scope.initialState, scope.targetState)
        return scope.slideIntoContainer(lateral ?: AnimatedContentTransitionScope.SlideDirection.Left, animSpec)
    }

    fun exit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        val lateral = getLateralDirection(scope.initialState, scope.targetState)
        return scope.slideOutOfContainer(lateral ?: AnimatedContentTransitionScope.SlideDirection.Left, animSpec)
    }

    fun popEnter(scope: AnimatedContentTransitionScope<NavBackStackEntry>) =
        scope.slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animSpec)

    fun popExit(scope: AnimatedContentTransitionScope<NavBackStackEntry>) =
        scope.slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animSpec)


    private const val FADE_IN_DURATION = 300
    private const val SCALE_IN_DURATION = 500
    private const val FADE_OUT_DURATION = 500
    private const val SCALE_OUT_DURATION = 500
    private const val SCALE_AMOUNT = 1.2f

    private const val ANIM_DELAY = FADE_OUT_DURATION

    fun enterRoot(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
        return fadeIn(
            animationSpec = tween(durationMillis = FADE_IN_DURATION, delayMillis = ANIM_DELAY)
        ) + scaleIn(
            animationSpec = tween(durationMillis = SCALE_IN_DURATION, delayMillis = ANIM_DELAY),
            initialScale = SCALE_AMOUNT
        )
    }

    fun exitRoot(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
        return fadeOut(
            animationSpec = tween(durationMillis = FADE_OUT_DURATION)
        ) + scaleOut(
            animationSpec = tween(durationMillis = SCALE_OUT_DURATION),
            targetScale = SCALE_AMOUNT
        )
    }
}