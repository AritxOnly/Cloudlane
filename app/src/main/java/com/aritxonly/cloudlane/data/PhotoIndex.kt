package com.aritxonly.cloudlane.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aritxonly.cloudlane.domain.PhotoEvidence
import java.time.LocalDate

data class Photo(val uri: String, val name: String, val size: Long, val modified: Long, val reason: String, val kept: Boolean)
data class CachedAnalysis(val hash: String, val evidence: PhotoEvidence)

class PhotoIndex(context: Context) : SQLiteOpenHelper(context, "photos.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE photos(
            uri TEXT PRIMARY KEY, name TEXT NOT NULL, size INTEGER NOT NULL,
            modified INTEGER NOT NULL, hash TEXT NOT NULL, reason TEXT NOT NULL,
            kept INTEGER NOT NULL DEFAULT 0, seen INTEGER NOT NULL, day INTEGER NOT NULL,
            path TEXT NOT NULL DEFAULT '', analysis_ready INTEGER NOT NULL DEFAULT 0,
            screenshot INTEGER NOT NULL DEFAULT 0, protected INTEGER NOT NULL DEFAULT 0,
            temporary INTEGER NOT NULL DEFAULT 0, deadline TEXT
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        check(old == 1 && new == 2) { "Migration required: $old -> $new" }
        // Retain the user's keep list. Old entries get fresh structured analysis once.
        db.execSQL("ALTER TABLE photos ADD COLUMN path TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE photos ADD COLUMN analysis_ready INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE photos ADD COLUMN screenshot INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE photos ADD COLUMN protected INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE photos ADD COLUMN temporary INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE photos ADD COLUMN deadline TEXT")
    }

    fun cached(uri: String, name: String, path: String, size: Long, modified: Long): CachedAnalysis? =
        readableDatabase.rawQuery(
            """SELECT hash,screenshot,protected,temporary,deadline FROM photos
                WHERE uri=? AND name=? AND path=? AND size=? AND modified=? AND analysis_ready=1""",
            arrayOf(uri, name, path, size.toString(), modified.toString()),
        ).use { c ->
            if (!c.moveToFirst()) null else CachedAnalysis(
                c.getString(0),
                PhotoEvidence(c.getInt(1) == 1, c.getInt(2) == 1, c.getInt(3) == 1,
                    c.getString(4)?.let(LocalDate::parse)),
            )
        }

    fun save(photo: Photo, path: String, analysis: CachedAnalysis, generation: Long) {
        val values = ContentValues().apply {
            put("uri", photo.uri); put("name", photo.name); put("path", path)
            put("size", photo.size); put("modified", photo.modified); put("hash", analysis.hash)
            put("reason", photo.reason); put("seen", generation); put("day", LocalDate.now().toEpochDay())
            put("analysis_ready", 1); put("screenshot", analysis.evidence.screenshot)
            put("protected", analysis.evidence.protectedDocument); put("temporary", analysis.evidence.temporaryCode)
            put("deadline", analysis.evidence.deadline?.toString())
        }
        // Updating rather than REPLACE preserves the keep decision.
        if (writableDatabase.update("photos", values, "uri=?", arrayOf(photo.uri)) == 0) {
            writableDatabase.insertOrThrow("photos", null, values)
        }
    }

    fun candidates(): List<Photo> = readableDatabase.rawQuery(
        "SELECT uri,name,size,modified,reason,kept FROM photos WHERE kept=0 AND protected=0 AND reason<>'' ORDER BY modified DESC", null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(Photo(c.getString(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4), c.getInt(5) == 1))
        }
    }

    fun hashes(generation: Long): Map<String, String> = readableDatabase.rawQuery(
        "SELECT uri,hash FROM photos WHERE seen=? AND hash<>''", arrayOf(generation.toString()),
    ).use { c -> buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) } }

    fun duplicate(uri: String) {
        writableDatabase.execSQL("UPDATE photos SET reason='发现内容相同的另一份图片，请预览后选择保留版本' WHERE uri=?", arrayOf(uri))
    }
    fun keep(uri: String) { writableDatabase.execSQL("UPDATE photos SET kept=1 WHERE uri=?", arrayOf(uri)) }
    fun resetKept() { writableDatabase.execSQL("UPDATE photos SET kept=0") }
    fun remove(uris: List<String>) {
        writableDatabase.beginTransaction()
        try {
            uris.forEach { writableDatabase.delete("photos", "uri=?", arrayOf(it)) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun hideUnseen(generation: Long) {
        // Permission loss only hides local suggestions; it never deletes media.
        writableDatabase.execSQL("UPDATE photos SET reason='' WHERE seen<>?", arrayOf(generation))
    }
}
