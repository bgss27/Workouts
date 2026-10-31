package com.fittrack.app.data.db

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Snapshot the SQLite file before Room migrates it to a newer schema version.
 *
 * Migrations are additive and tested, but a snapshot costs a few hundred KB
 * and turns a bad migration from "history gone" into "restore the file".
 * Backups live in the app's private storage next to the live database, so
 * they are removed with the app and never leave the device.
 */
object DatabaseBackup {
    private const val KEEP = 5

    /**
     * If [dbName] exists on disk at a schema version lower than
     * [targetVersion], copy it (plus -wal / -shm) into `databases/backups/`.
     * Returns the backup directory used, or null when nothing was copied.
     */
    fun backupBeforeUpgrade(context: Context, dbName: String, targetVersion: Int): File? {
        val main = context.getDatabasePath(dbName)
        if (!main.exists() || main.length() < 100) return null
        val onDisk = readUserVersion(main) ?: return null
        if (onDisk <= 0 || onDisk >= targetVersion) return null

        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dir = File(main.parentFile, "backups").apply { mkdirs() }
        val prefix = "$dbName.v$onDisk.$stamp"
        runCatching {
            main.copyTo(File(dir, prefix), overwrite = true)
            for (ext in listOf("-wal", "-shm")) {
                val side = File(main.path + ext)
                if (side.exists()) side.copyTo(File(dir, prefix + ext), overwrite = true)
            }
            prune(dir, dbName)
        }.onFailure { return null }
        return dir
    }

    /** List backup main files, newest first. */
    fun list(context: Context, dbName: String): List<File> {
        val dir = File(context.getDatabasePath(dbName).parentFile, "backups")
        return dir.listFiles { f -> f.name.startsWith("$dbName.v") && !f.name.endsWith("-wal") && !f.name.endsWith("-shm") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    private fun prune(dir: File, dbName: String) {
        val mains = dir.listFiles { f ->
            f.name.startsWith("$dbName.v") && !f.name.endsWith("-wal") && !f.name.endsWith("-shm")
        }?.sortedByDescending { it.lastModified() } ?: return
        mains.drop(KEEP).forEach { old ->
            old.delete()
            File(old.path + "-wal").delete()
            File(old.path + "-shm").delete()
        }
    }

    /** SQLite stores PRAGMA user_version as a big-endian int at header offset 60. */
    private fun readUserVersion(file: File): Int? = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(60)
            val b = ByteArray(4)
            raf.readFully(b)
            ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
                ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
        }
    }.getOrNull()
}
