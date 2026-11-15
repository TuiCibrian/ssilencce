package com.ssilencce.core.model

enum class GenreSubcategory {
    Songs,
    Albums,
    Artists,
    ;

    companion object {
        fun fromName(name: String?): GenreSubcategory = entries.firstOrNull { it.name == name } ?: Songs
    }
}
