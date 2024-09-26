package easy_a.models

data class UserResponse(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: String?,
    val dateOfBirth: String?,
    val profilePicture: String?,
    val token: String?
)
