package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.example.data.lyrics.LrcParser
import com.example.data.model.Song
import java.io.File

object AudioScanner {

    private const val TAG = "AudioScanner"

    private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "flac", "aac", "ogg", "opus")

    /**
     * Scans MediaStore for audio files on the device.
     */
    fun scanMediaStore(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 10000"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durCol)
                    val filePath = cursor.getString(dataCol)
                    val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    // Check for companion LRC file next to the audio file
                    val lrcContent = filePath?.let { LrcParser.readCompanionLrc(it) }

                    // Album art URI via content resolver
                    val artUri = Uri.parse("content://media/external/audio/media/$id/albumart")

                    songList.add(
                        Song(
                            id = "mediastore_$id",
                            title = if (title.isBlank() || title == "<unknown>") "Track $id" else title,
                            artist = if (artist.isBlank() || artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album.isBlank() || album == "<unknown>") "Single" else album,
                            durationMs = if (duration > 0) duration else 180000L,
                            uri = contentUri,
                            albumArtUri = artUri,
                            filePath = filePath,
                            lrcContent = lrcContent,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        return songList
    }

    /**
     * Directly scans standard music and storage folders on device for audio and matching LRC files.
     */
    fun scanDeviceFolders(context: Context): List<Song> {
        val foundSongs = mutableListOf<Song>()
        val externalStorage = Environment.getExternalStorageDirectory()

        val directoriesToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            File(externalStorage, "Audio"),
            File(externalStorage, "Telegram/Telegram Audio"),
            File(externalStorage, "Music")
        )

        for (dir in directoriesToScan) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryRecursively(context, dir, foundSongs, maxDepth = 4)
            }
        }

        return foundSongs
    }

    private fun scanDirectoryRecursively(
        context: Context,
        dir: File,
        accumulator: MutableList<Song>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryRecursively(context, file, accumulator, maxDepth - 1)
                }
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    val song = extractSongFromFile(context, file)
                    accumulator.add(song)
                }
            }
        }
    }

    private fun extractSongFromFile(context: Context, file: File): Song {
        var title = file.nameWithoutExtension
        var artist = "Unknown Artist"
        var album = file.parentFile?.name ?: "Local Folder"
        var duration = 180000L

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(file))
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            if (!metaTitle.isNullOrBlank()) title = metaTitle
            if (!metaArtist.isNullOrBlank()) artist = metaArtist
            if (!metaAlbum.isNullOrBlank()) album = metaAlbum
            metaDur?.toLongOrNull()?.let { if (it > 0) duration = it }
            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve metadata for ${file.name}", e)
        }

        val lrc = LrcParser.readCompanionLrc(file.absolutePath)

        return Song(
            id = "file_${file.absolutePath.hashCode()}",
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            uri = Uri.fromFile(file),
            filePath = file.absolutePath,
            lrcContent = lrc,
            dateAdded = file.lastModified()
        )
    }

    /**
     * Scans a custom folder picked by user via Storage Access Framework (SAF).
     */
    fun scanDocumentTree(context: Context, treeUri: Uri): List<Song> {
        val results = mutableListOf<Song>()
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()

                    if (mime.startsWith("audio/") || ext in AUDIO_EXTENSIONS) {
                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        results.add(
                            Song(
                                id = "saf_${childDocId.hashCode()}",
                                title = name.substringBeforeLast('.'),
                                artist = "Folder Import",
                                album = "Local Folder",
                                durationMs = 180000L,
                                uri = fileDocUri,
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SAF document tree", e)
        }
        return results
    }
}
