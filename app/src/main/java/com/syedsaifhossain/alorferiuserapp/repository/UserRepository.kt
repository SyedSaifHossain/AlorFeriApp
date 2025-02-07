package com.syedsaifhossain.alorferiuserapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.syedsaifhossain.alorferiuserapp.api.UserAPI.UserAPI
import com.syedsaifhossain.alorferiuserapp.models.UserRequest
import com.syedsaifhossain.alorferiuserapp.utils.NetworkResult
import jakarta.inject.Inject
import org.json.JSONObject


class UserRepository @Inject constructor((private val userAPI: UserAPI) {

    private val _userResponseLiveData = MutableLiveData<NetworkResult<UserResponse>>()
    val userResponseLiveData: LiveData<NetworkResult<UserResponse>>
        get() = _userResponseLiveData

    suspend fun registerUser(userRequest: UserRequest) {
        _userResponseLiveData.postValue(NetworkResult.Loading())
        val response = userAPI.signin(userRequest)
        handleResponse(response)
    }

    suspend fun loginUser(userRequest: UserRequest) {
        _userResponseLiveData.postValue(NetworkResult.Loading())
        val response = userAPI.signin(userRequest)
        handleResponse(response)
    }

    private fun handleResponse(response: Response<UserResponse>) {

        if (response.isSuccessful && response.body() != null) {
            _userResponseLiveData.postValue(NetworkResult.Success(response.body()!!))
        } else if (response.errorBody() != null) {
            val errorObj = JSONObject(response.errorBody()!!.charStream().readText())
            _userResponseLiveData.postValue(NetworkResult.Error(errorObj.getString("message")))
        } else {
            _userResponseLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
        }
    }
}