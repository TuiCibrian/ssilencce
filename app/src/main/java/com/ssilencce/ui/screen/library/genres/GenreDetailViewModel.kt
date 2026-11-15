package com.ssilencce.ui.screen.library.genres

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.GenreContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenreDetailViewModel(
    private val repository: Repository,
    private val genre: Genre,
) : ViewModel() {
    private val _genreContent = MutableLiveData<GenreContent>()

    fun getContent(): LiveData<GenreContent> = _genreContent

    init {
        loadGenreContent()
    }

    fun loadGenreContent() =
        viewModelScope.launch(Dispatchers.IO) {
            _genreContent.postValue(repository.genreContent(genre.id))
        }
}
