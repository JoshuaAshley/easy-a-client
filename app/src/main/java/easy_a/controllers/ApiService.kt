package easy_a.controllers

import easy_a.models.UserResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    @FormUrlEncoded
    @POST("api/User/login")
    fun loginUser(
        @Field("Email") email: String,
        @Field("Password") password: String
    ): Call<UserResponse>

    @FormUrlEncoded
    @POST("api/User/register")
    fun registerUser(
        @Field("Email") email: String,
        @Field("Password") password: String,
        @Field("FirstName") firstName: String,
        @Field("LastName") lastName: String,
        @Field("Gender") gender: String? = null,
        @Field("DateOfBirth") dateOfBirth: String? = null,
        @Field("ProfilePicture") profilePicture: String? = null
    ): Call<UserResponse>
}
