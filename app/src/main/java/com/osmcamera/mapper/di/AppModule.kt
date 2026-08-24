package com.osmcamera.mapper.di

import android.content.Context
import androidx.room.Room
import com.osmcamera.mapper.data.api.OSMApiService
import com.osmcamera.mapper.data.api.OverpassApiService
import com.osmcamera.mapper.data.local.AppDatabase
import com.osmcamera.mapper.data.local.CameraDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dagger Hilt module for app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideCameraDao(database: AppDatabase): CameraDao {
        return database.cameraDao()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "BalanceTaCam/2.2.0")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @OSMRetrofit
    fun provideOSMRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OSMApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @OverpassRetrofit
    fun provideOverpassRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OverpassApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideOSMApiService(@OSMRetrofit retrofit: Retrofit): OSMApiService {
        return retrofit.create(OSMApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideOverpassApiService(@OverpassRetrofit retrofit: Retrofit): OverpassApiService {
        return retrofit.create(OverpassApiService::class.java)
    }
    
    @Provides
    @Singleton
    @ORSRetrofit
    fun provideORSRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.osmcamera.mapper.data.api.OpenRouteServiceApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideOpenRouteServiceApi(@ORSRetrofit retrofit: Retrofit): com.osmcamera.mapper.data.api.OpenRouteServiceApi {
        return retrofit.create(com.osmcamera.mapper.data.api.OpenRouteServiceApi::class.java)
    }
    
    @Provides
    @Singleton
    @NominatimRetrofit
    fun provideNominatimRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.osmcamera.mapper.data.api.NominatimApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNominatimApi(@NominatimRetrofit retrofit: Retrofit): com.osmcamera.mapper.data.api.NominatimApi {
        return retrofit.create(com.osmcamera.mapper.data.api.NominatimApi::class.java)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OSMRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OverpassRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ORSRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NominatimRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadClient


