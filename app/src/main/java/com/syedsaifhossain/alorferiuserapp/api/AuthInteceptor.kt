package com.syedsaifhossain.alorferiuserapp.api

import jakarta.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class AuthInteceptor @Inject constructor():Interceptor{

    @Inject
    lateinit var tokenManager: TokenManager

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()

        val token = tokenManager.getToken()
        request.addHeader("Authorization", "Bearer $token")
        return chain.proceed(request.build())
}