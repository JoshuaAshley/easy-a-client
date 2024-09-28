package easy_a.models

data class EventResult (
    val uid: String,
    val eventId: String,
    val eventName: String,
    val eventDate: String,
)

data class EventResponse (
    val message: String,
    val eventList: List<EventResult>
)