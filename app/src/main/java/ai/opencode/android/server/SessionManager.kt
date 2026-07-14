package ai.opencode.android.server

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Serializable
data class Session(
    val id: String,
    val title: String,
    val model: String = "claude-sonnet-4-20250514",
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolCalls: String? = null,
    val toolResult: String? = null
)

@Singleton
object SessionManager {
    private lateinit var dbHelper: DatabaseHelper

    fun init(context: Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun getAllSessions(): List<Session> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("sessions", null, null, null, null, null, "updated_at DESC")
        val sessions = mutableListOf<Session>()
        while (cursor.moveToNext()) {
            sessions.add(Session(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                model = cursor.getString(cursor.getColumnIndexOrThrow("model")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            ))
        }
        cursor.close()
        return sessions
    }

    fun createSession(title: String = "New Chat"): Session {
        val session = Session(
            id = UUID.randomUUID().toString().replace("-", "").take(24),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val db = dbHelper.writableDatabase
        db.execSQL(
            "INSERT INTO sessions (id, title, model, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            arrayOf(session.id, session.title, session.model, session.createdAt, session.updatedAt)
        )
        return session
    }

    fun getSession(id: String): Session? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("sessions", null, "id = ?", arrayOf(id), null, null, null)
        val session = if (cursor.moveToFirst()) {
            Session(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                model = cursor.getString(cursor.getColumnIndexOrThrow("model")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
        } else null
        cursor.close()
        return session
    }

    fun deleteSession(id: String) {
        val db = dbHelper.writableDatabase
        db.delete("messages", "session_id = ?", arrayOf(id))
        db.delete("sessions", "id = ?", arrayOf(id))
    }
}

@Singleton
object MessageStore {
    private lateinit var dbHelper: DatabaseHelper

    fun init(context: Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun getMessages(sessionId: String): List<Message> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("messages", null, "session_id = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        val messages = mutableListOf<Message>()
        while (cursor.moveToNext()) {
            messages.add(Message(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id")),
                role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                toolCalls = cursor.getString(cursor.getColumnIndexOrThrow("tool_calls")),
                toolResult = cursor.getString(cursor.getColumnIndexOrThrow("tool_result"))
            ))
        }
        cursor.close()
        return messages
    }

    fun addMessage(message: Message) {
        val db = dbHelper.writableDatabase
        db.execSQL(
            "INSERT INTO messages (id, session_id, role, content, timestamp, tool_calls, tool_result) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(message.id, message.sessionId, message.role, message.content, message.timestamp, message.toolCalls, message.toolResult)
        )
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "opencode.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE sessions (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                model TEXT NOT NULL DEFAULT 'claude-sonnet-4-20250514',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE messages (
                id TEXT PRIMARY KEY,
                session_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                tool_calls TEXT,
                tool_result TEXT,
                FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS sessions")
        onCreate(db)
    }
}
