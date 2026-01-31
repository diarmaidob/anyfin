package io.github.diarmaidob.anyfin.injection

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.diarmaidob.anyfin.feature.player.ExoPlayerController
import io.github.diarmaidob.anyfin.feature.player.VideoPlayerController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PresentationModule {

    @Binds
    @Singleton
    abstract fun bindVideoPlayerController(
        exoPlayerController: ExoPlayerController
    ): VideoPlayerController

}