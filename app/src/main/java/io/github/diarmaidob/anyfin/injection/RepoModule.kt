package io.github.diarmaidob.anyfin.injection

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.diarmaidob.anyfin.core.auth.AuthHeaderRepo
import io.github.diarmaidob.anyfin.core.auth.AuthRepo
import io.github.diarmaidob.anyfin.core.auth.data.repo.AuthHeaderRepoImpl
import io.github.diarmaidob.anyfin.core.auth.data.repo.AuthRepoImpl
import io.github.diarmaidob.anyfin.core.device.DeviceRepo
import io.github.diarmaidob.anyfin.core.device.data.DeviceRepoImpl
import io.github.diarmaidob.anyfin.core.mediaitem.MediaItemRepo
import io.github.diarmaidob.anyfin.core.mediaitem.data.repo.MediaItemRepoImpl
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import io.github.diarmaidob.anyfin.core.session.SessionRepoImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {

    @Binds
    @Singleton
    abstract fun bindAuthHeaderRepo(
        impl: AuthHeaderRepoImpl
    ): AuthHeaderRepo

    @Binds
    @Singleton
    abstract fun bindAuthRepo(
        impl: AuthRepoImpl
    ): AuthRepo

    @Binds
    @Singleton
    abstract fun bindMediaItemRepo(
        impl: MediaItemRepoImpl
    ): MediaItemRepo


    @Binds
    @Singleton
    abstract fun bindSessionRepo(
        impl: SessionRepoImpl
    ): SessionRepo

    @Binds
    @Singleton
    abstract fun bindDeviceRepo(
        impl: DeviceRepoImpl
    ): DeviceRepo


}