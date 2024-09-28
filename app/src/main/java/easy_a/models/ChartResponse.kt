package easy_a.models

data class ChartResult (
    val questionPaperName: String,
    val totalLoggedTime: Number,
)

data class ChartResponse (
    val message: String,
    val data: List<ChartResult>
)