package io.github.diarmaidob.anyfin.injection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.diarmaidob.anyfin.BuildConfig
import io.github.diarmaidob.anyfin.core.auth.data.api.AuthApi
import io.github.diarmaidob.anyfin.core.auth.data.api.AuthInterceptor
import io.github.diarmaidob.anyfin.core.data.JellyfinImageMapper
import io.github.diarmaidob.anyfin.core.data.RelativePathMapper
import io.github.diarmaidob.anyfin.core.entity.AnyfinDatabase
import io.github.diarmaidob.anyfin.core.entity.MediaItemQueries
import io.github.diarmaidob.anyfin.core.mediaitem.data.api.MediaItemApi
import io.github.diarmaidob.anyfin.core.session.SecureSessionSerializer
import io.github.diarmaidob.anyfin.core.session.StoredSession
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val KEYSET_NAME = "master_keyset"
    private const val PREF_FILE_NAME = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://master_key"

    @Provides
    @Singleton
    fun provideAead(@ApplicationContext context: Context): Aead {
        AeadConfig.register()

        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()

        return manager.keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            Aead::class.java
        )
    }

    @Provides
    @Singleton
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
        aead: Aead,
        moshi: Moshi
    ): DataStore<StoredSession> {
        return DataStoreFactory.create(
            serializer = SecureSessionSerializer(aead, moshi),
            produceFile = { File(context.filesDir, "session_secure.json") }
        )
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        relativePathMapper: RelativePathMapper,
        jellyfinImageMapper: JellyfinImageMapper
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(relativePathMapper)
                add(jellyfinImageMapper)
                add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient))
            }
            .crossfade(true)
            .build()
    }


    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }


    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMediaApi(retrofit: Retrofit): MediaItemApi = retrofit.create(MediaItemApi::class.java)


    @Provides
    @Singleton
    fun provideSqlDriver(@ApplicationContext context: Context): AndroidSqliteDriver {
        return AndroidSqliteDriver(
            schema = AnyfinDatabase.Companion.Schema,
            context = context,
            name = "anyfin.db",
            callback = object : AndroidSqliteDriver.Callback(AnyfinDatabase.Companion.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(driver: AndroidSqliteDriver): AnyfinDatabase {
        return AnyfinDatabase.Companion(driver)
    }

    @Provides
    @Singleton
    fun provideMediaItemQueries(db: AnyfinDatabase): MediaItemQueries {
        return db.mediaItemQueries
    }
}