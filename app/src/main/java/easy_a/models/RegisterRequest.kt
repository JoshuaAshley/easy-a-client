package easy_a.models

data class RegisterRequest(
    val Email: String,
    val Password: String,
    val FirstName: String,
    val LastName: String
)
