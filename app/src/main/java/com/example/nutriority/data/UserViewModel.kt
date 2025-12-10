package com.example.nutriority.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nutriority.data.model.User
import com.example.nutriority.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository

    private val _user = MutableLiveData<User>()

    val user: LiveData<User> = _user

    init {
        val userDao = UserDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)

        viewModelScope.launch {
            val initialUser = repository.getInitialUser() ?: User(id = 1)
            _user.postValue(initialUser)
        }
    }
    fun updateOnboardingData(updateAction: (User) -> User) {
        val updatedUser = updateAction(_user.value ?: User(id = 1))
        _user.value = updatedUser
    }
    fun saveOnboardingData() {
        _user.value?.let { userToSave ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertUser(userToSave)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
