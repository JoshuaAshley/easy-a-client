package easy_a.controllers

data class UserResponse(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val token: String?  // Optional token for authentication
)
