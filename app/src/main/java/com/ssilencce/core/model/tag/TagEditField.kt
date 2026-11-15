package com.ssilencce.core.model.tag

import androidx.annotation.StringRes
import com.ssilencce.R
import com.ssilencce.data.local.MetadataReader

enum class TagEditField(
    val propertyKey: String,
    @StringRes val labelRes: Int,
    val inputKind: InputKind,
) {
    TITLE(MetadataReader.TITLE, R.string.title, InputKind.TEXT),
    ALBUM(MetadataReader.ALBUM, R.string.album, InputKind.TEXT),
    ARTIST(MetadataReader.ARTIST, R.string.artist, InputKind.TEXT),
    ALBUM_ARTIST(MetadataReader.ALBUM_ARTIST, R.string.album_artist, InputKind.TEXT),
    COMPOSER(MetadataReader.COMPOSER, R.string.composer, InputKind.TEXT),
    CONDUCTOR(MetadataReader.PRODUCER, R.string.conductor, InputKind.TEXT),
    PUBLISHER(MetadataReader.COPYRIGHT, R.string.publisher, InputKind.TEXT),
    GENRE(MetadataReader.GENRE, R.string.genre, InputKind.TEXT),
    YEAR(MetadataReader.YEAR, R.string.year, InputKind.NUMBER),
    TRACK_NUMBER(MetadataReader.TRACK_NUMBER, R.string.track, InputKind.NUMBER),
    TRACK_TOTAL(MetadataReader.TRACK_TOTAL, R.string.track_total, InputKind.NUMBER),
    DISC_NUMBER(MetadataReader.DISC_NUMBER, R.string.disc, InputKind.NUMBER),
    DISC_TOTAL(MetadataReader.DISC_TOTAL, R.string.disc_total, InputKind.NUMBER),
    LYRICS(MetadataReader.LYRICS, R.string.lyrics, InputKind.MULTILINE),
    LYRICIST(MetadataReader.LYRICIST, R.string.lyricist, InputKind.TEXT),
    ARRANGER(MetadataReader.ARRANGER, R.string.arranger, InputKind.TEXT),
    COMMENT(MetadataReader.COMMENT, R.string.comment, InputKind.MULTILINE),
    ;

    enum class InputKind {
        TEXT,
        NUMBER,
        MULTILINE,
    }
}
