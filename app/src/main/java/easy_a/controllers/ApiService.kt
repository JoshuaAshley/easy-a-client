package easy_a.controllers

import easy_a.models.CheckUserExistsResponse
import easy_a.models.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import java.util.Date

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
        @Field("FirstName") firstName: String? = null,
        @Field("LastName") lastName: String? = null,
        @Field("Gender") gender: String? = null,
        @Field("DateOfBirth") dateOfBirth: String? = null,
        @Field("ProfilePicture") profilePicture: String? = null
    ): Call<UserResponse>

    @FormUrlEncoded
    @POST("api/User/google-signin")
    fun registerUserWithGoogle(
        @Field("Uid") uid: String,
        @Field("Email") email: String,
        @Field("FirstName") firstName: String,
        @Field("LastName") lastName: String,
        @Field("ProfilePicture") profilePicture: String?
    ): Call<UserResponse>

    @Multipart
    @PUT("api/user/update")
    fun updateUserSettings(
        @Part("uid") uid: RequestBody,
        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("dateOfBirth") dob: RequestBody,
        @Part profileImage: MultipartBody.Part? // Optional part for the image
    ): Call<UserResponse> // Adjust the return type based on your API response

    @FormUrlEncoded
    @POST("api/User/check-user-exists")
    fun checkUserExists(
        @Field("Email") email: String
    ): Call<CheckUserExistsResponse>

}
