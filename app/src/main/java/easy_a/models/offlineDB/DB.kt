package easy_a.models.offlineDB

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "study_papers")
data class StudyPaper(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val paperName: String,
    val description: String,
    val dueDate: String,
    val pdfFilePath: String // path to the saved PDF file locally
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyPaper::class,
            parentColumns = ["id"],
            childColumns = ["paperId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val paperId: Int, // Foreign key to StudyPaper
    val questionNumber: String,
    val description: String,
    val imageFilePath: String? = null
)

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val eventId: Int = 0,
    val eventName: String,
    val eventDescription: String,
    val eventDueDate: String,
    val synced: Boolean
)

@Dao
interface EasyDao {

    // Existing methods for StudyPaper
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPaper(studyPaper: StudyPaper)

    @Query("SELECT * FROM study_papers")
    suspend fun getPendingStudyPapers(): List<StudyPaper>

    @Delete
    suspend fun deleteStudyPaper(studyPaper: StudyPaper)

    // New methods for Question
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question)

    @Query("SELECT * FROM questions WHERE paperId = :paperId")
    suspend fun getQuestionsForPaper(paperId: Int): List<Question>

    @Delete
    suspend fun deleteQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<Event>

    @Query("UPDATE events SET synced = 1 WHERE eventId = :eventId")
    suspend fun updateEventSyncedStatus(eventId: Int)

    @Delete
    suspend fun deleteEvent(event: Event)

    // New method to get events by month
    @Query("SELECT * FROM events WHERE eventDueDate BETWEEN :startDate AND :endDate")
    suspend fun getEventsByDateRange(startDate: String, endDate: String): List<Event>

    @Query("SELECT * FROM events WHERE eventDueDate = :date")
    suspend fun getEventsByDate(date: String): List<Event>
}

@Database(entities = [StudyPaper::class, Question::class, Event::class], version = 2)
abstract class EasyDatabase : RoomDatabase() {
    abstract fun EasyDao(): EasyDao

    companion object {
        @Volatile
        private var INSTANCE: EasyDatabase? = null

        fun getDatabase(context: Context): EasyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EasyDatabase::class.java,
                    "easy_a_offline"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}