package easy_a.models

data class QuestionResult (
    val uid: String,
    val questionPaperId: String,
    val questionId: String,
    val questionName: String,
    val questionDescription: String,
    val imageLocation: String?,
    val totatLoggedTime: Number?,
    val isCompleted: Boolean?,
)

data class QuestionUpdateResult (
    val message: String,
    val totalLoggedTime: Number?,
)

data class QuestionResponse (
    val message: String,
    val result: QuestionResult
)

data class QuestionsListResponse(
    val questions: List<QuestionResult>
)