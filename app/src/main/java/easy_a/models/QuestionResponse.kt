package easy_a.models

data class QuestionResult (
    val uid: String,
    val questionPaperId: String,
    val questionId: String,
    val questionName: String,
    val questionDescription: String,
    val imageLocation: String?,
    val totalLoggedTime: Number?,
    val isCompleted: Boolean?,
)

data class QuestionResponse (
    val message: String,
    val result: QuestionResult
)

data class QuestionsListResponse(
    val questions: List<QuestionResult>
)