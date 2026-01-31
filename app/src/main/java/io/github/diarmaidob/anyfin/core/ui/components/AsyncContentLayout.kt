package io.github.diarmaidob.anyfin.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.diarmaidob.anyfin.core.ui.uimodel.ScreenStateUiModel

private enum class AsyncViewState {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT
}

@Composable
fun <Event> AsyncContentLayout(
    modifier: Modifier = Modifier,
    uiModel: ScreenStateUiModel<Event>,
    onRefresh: () -> Unit,
    onEventConsumed: (String) -> Unit,
    onEvent: suspend (Event) -> Unit,
    loadingContent: @Composable () -> Unit = { DefaultLoadingView() },
    emptyContent: @Composable () -> Unit = { DefaultEmptyView() },
    errorContent: @Composable (Throwable) -> Unit = { DefaultErrorView(it) },
    content: @Composable () -> Unit
) {
    EventEffect(uiModel.events, onEventConsumed, onEvent)

    val viewState = when {
        uiModel.isInitialLoading -> AsyncViewState.LOADING
        uiModel.error != null && uiModel.isEmpty -> AsyncViewState.ERROR
        uiModel.isEmpty -> AsyncViewState.EMPTY
        else -> AsyncViewState.CONTENT
    }

    AnimatedContent(
        targetState = viewState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AsyncContentTransition",
        modifier = modifier
    ) { targetState ->

        when (targetState) {
            AsyncViewState.LOADING -> {
                loadingContent()
            }

            AsyncViewState.ERROR -> {
                uiModel.error?.let { errorContent(it) }
            }

            AsyncViewState.EMPTY -> {
                PullToRefreshBox(
                    isRefreshing = uiModel.isPullRefreshing,
                    onRefresh = onRefresh
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        emptyContent()
                    }
                }
            }

            AsyncViewState.CONTENT -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = uiModel.isPullRefreshing,
                        onRefresh = onRefresh
                    ) {
                        content()
                    }

                    AnimatedVisibility(
                        visible = uiModel.isSyncing,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}