package com.makerpress.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.makerpress.app.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val DocumentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val Context.settingsStore by preferencesDataStore("makerpress")
class SettingsRepository(private val context: Context) {
    private val key = stringPreferencesKey("settings")
    val settings = context.settingsStore.data.map { prefs -> (prefs[key]?.let { DocumentJson.decodeFromString<AppSettings>(it) } ?: AppSettings()).let { if(it.manifestUrl.isBlank()) it.copy(manifestUrl = DEFAULT_UPDATE_URL) else it } }
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs ->
            val old = prefs[key]?.let { DocumentJson.decodeFromString<AppSettings>(it) } ?: AppSettings()
            val next = transform(old); next.profile.validate(); require(next.copies in 1..20)
            prefs[key] = DocumentJson.encodeToString(next)
        }
    }
}
@Entity(tableName = "print_jobs")
data class PrintJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val documentJson: String, val profileJson: String,
    val copies: Int, val createdAt: Long, val state: String,
    val progress: Int = 0, @ColumnInfo(defaultValue = "''") val message: String = ""
)
@Dao interface JobDao {
    @Query("SELECT * FROM print_jobs ORDER BY createdAt DESC, id DESC") fun observe(): Flow<List<PrintJob>>
    @Insert suspend fun insert(job: PrintJob): Long
    @Query("SELECT * FROM print_jobs WHERE state = 'PENDING' ORDER BY id LIMIT 1") suspend fun next(): PrintJob?
    @Query("UPDATE print_jobs SET state=:state, progress=:progress, message=:message WHERE id=:id") suspend fun status(id: Long, state: String, progress: Int, message: String)
    @Query("UPDATE print_jobs SET state='RENDERING' WHERE id=:id AND state='PENDING'") suspend fun claim(id: Long): Int
    @Query("UPDATE print_jobs SET state='CANCELLED' WHERE id=:id AND state='PENDING'") suspend fun cancel(id: Long)
    @Query("DELETE FROM print_jobs WHERE id=:id AND state NOT IN ('SENDING','RENDERING')") suspend fun delete(id: Long)
    @Query("DELETE FROM print_jobs WHERE state NOT IN ('SENDING','RENDERING','PENDING')") suspend fun clearHistory()
    @Query("UPDATE print_jobs SET state='UNKNOWN', message='Aplicativo interrompido durante transmissão. Confira o papel antes de repetir.' WHERE state='SENDING'") suspend fun recoverSending()
    @Query("UPDATE print_jobs SET state='FAILED', message='Renderização interrompida. Pode tentar novamente.' WHERE state='RENDERING'") suspend fun recoverRendering()
}
@Database(entities = [PrintJob::class], version = 2, exportSchema = true)
abstract class MakerDatabase : RoomDatabase() {
    abstract fun jobs(): JobDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE print_jobs ADD COLUMN message TEXT NOT NULL DEFAULT ''") }
        }
        fun open(context: Context) = Room.databaseBuilder(context, MakerDatabase::class.java, "makerpress.db").addMigrations(MIGRATION_1_2).build()
    }
}
class JobRepository(val dao: JobDao) {
    val jobs = dao.observe()
    suspend fun save(document: PrintDocument, profile: PrinterProfile, copies: Int, queued: Boolean): Long {
        require(copies in 1..20); profile.validate()
        return dao.insert(PrintJob(title = document.title, documentJson = DocumentJson.encodeToString(document), profileJson = DocumentJson.encodeToString(profile), copies = copies, createdAt = System.currentTimeMillis(), state = if (queued) "PENDING" else "SAVED"))
    }
    fun document(job: PrintJob) = DocumentJson.decodeFromString<PrintDocument>(job.documentJson)
    fun profile(job: PrintJob) = DocumentJson.decodeFromString<PrinterProfile>(job.profileJson)
}
