package com.syedsaifhossain.alorferiuserapp.api.UserAPI

interface UserAPI {

    @POST("/users/signin")

    suspend fun signin(@Body userRequest: UserRequest) : Response<UserResponse>
}