package com.ssilencce.coil.model

import com.ssilencce.data.local.room.PlaylistEntity
import com.ssilencce.data.model.Song

class PlaylistImage(val playlistEntity: PlaylistEntity, val songs: List<Song>) {
    override fun toString(): String {
        return buildString {
            append("PlaylistImage{")
            append("playlistEntity=$playlistEntity,")
            append("songs=$songs")
            append("}")
        }
    }
}