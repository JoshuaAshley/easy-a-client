package easy_a.models

data class QuestionPaperResponse (
    val questionPaperId: String,
    val questionPaperName: String,
    val questionPaperDueDate: String,
    val questionPaperDescription: String,
    val pdfLocation: String?,
    val numQuestions: Number?,
    val numCompletedQuestions: Number?,
)

data class QuestionPaperListResponse(
    val questionPapers: List<QuestionPaperResponse>
)