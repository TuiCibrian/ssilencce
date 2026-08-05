package com.ssilencce.data

import com.ssilencce.data.model.Song

interface SongProvider {
    val songs: List<Song>
}