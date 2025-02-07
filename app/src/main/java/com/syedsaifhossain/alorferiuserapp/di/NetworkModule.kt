package com.syedsaifhossain.alorferiuserapp.di

import com.syedsaifhossain.alorferiuserapp.AuthInterceptor
import com.syedsaifhossain.alorferiuserapp.NoteAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkModule {

    @InstallIn(SingletonComponent::class)
    @Module
    class NetworkModule {

        @Singleton
        @Provides
        fun providesRetrofit(): Retrofit.Builder {
            return Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
        }

        @Singleton
        @Provides
        fun provideOkHttpClient(interceptor: AuthInterceptor): OkHttpClient {
            return OkHttpClient.Builder().addInterceptor(interceptor).build()
        }

        @Singleton
        @Provides
        fun providesUserAPI(retrofitBuilder: Retrofit.Builder): UserAPI {
            return retrofitBuilder.build().create(UserAPI::class.java)
        }

        @Singleton
        @Provides
        fun providesNoteAPI(
            retrofitBuilder: Retrofit.Builder,
            okHttpClient: OkHttpClient
        ): NoteAPI {
            return retrofitBuilder.client(okHttpClient).build().create(NoteAPI::class.java)
        }
}