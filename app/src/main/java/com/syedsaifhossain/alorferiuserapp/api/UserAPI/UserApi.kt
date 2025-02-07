package com.syedsaifhossain.alorferiuserapp.api.UserAPI

interface UserApi {

    @POST("/users/signin")

    suspend fun signin(@Body userRequest: UserRequest) : Response<UserResponse>
}