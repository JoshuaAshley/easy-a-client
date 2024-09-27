package easy_a.controllers

import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import easy_a.models.CheckUserExistsResponse
import easy_a.models.QuestionPaperListResponse
import easy_a.models.QuestionPaperResponse
import easy_a.models.QuestionResponse
import easy_a.models.QuestionResult
import easy_a.models.QuestionsListResponse
import easy_a.models.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
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



    @Multipart
    @POST("api/QuestionPaper/create")
    fun createQuestionPaper(
        @Part("uid") uid: RequestBody,
        @Part("questionPaperName") firstName: RequestBody,
        @Part("questionPaperDueDate") lastName: RequestBody,
        @Part("questionPaperDescription") gender: RequestBody,
        @Part pdfFile: MultipartBody.Part? // Optional part for the image
    ): Call<QuestionPaperResponse> // Adjust the return type based on your API response

    @GET("api/QuestionPaper/list/{uid}")
    fun getListQuestionPapers(
        @Path("uid") uid: String
    ): Call<QuestionPaperListResponse>

    @GET("api/QuestionPaper/home/{uid}")
    fun getHomeInfo(
        @Path("uid") uid: String
    ): Call<QuestionPaperListResponse>

    @GET("api/QuestionPaper/{uid}/question-paper/{questionPaperId}")
    fun getQuestionPaper(
        @Path("uid") uid: String,
        @Path("questionPaperId") questionPaperId: String
    ): Call<QuestionPaperResponse>



    @Multipart
    @POST("api/Question/create") // Updated to include the path parameter
    fun createQuestion(
        @Part("uid") uid: RequestBody,
        @Part("questionPaperId") questionPaperId: RequestBody,
        @Part("questionName") questionName: RequestBody,
        @Part("questionDescription") questionDescription: RequestBody,
        @Part questionImage: MultipartBody.Part?
    ): Call<QuestionResponse>

    @GET("api/Question/{uid}/question-paper/{questionPaperId}/questions")
    fun getListQuestions(
        @Path("uid") uid: String,
        @Path("questionPaperId") questionPaperId: String
    ): Call<QuestionsListResponse>

    @GET("api/Question/{uid}/question-paper/{questionPaperId}/questions/{questionId}")
    fun getQuestion(
        @Path("uid") uid: String,
        @Path("questionPaperId") questionPaperId: String,
        @Path("questionId") questionId: String
    ): Call<QuestionResult>
}
