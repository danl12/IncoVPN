package com.danl.incovpn.data.model

import androidx.lifecycle.liveData

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {

    class Success<T>(data: T) : Resource<T>(data)
    class Loading<T> : Resource<T>()
    class Error<T>(message: String) : Resource<T>(message = message)

    fun success(onSuccess: (T) -> Unit) {
        if (this is Success) {
            onSuccess(data!!)
        }
    }

    fun loading(onLoading: (T?) -> Unit) {
        if (this is Loading) {
            onLoading(data)
        }
    }

    fun error(onError: (String) -> Unit) {
        if (this is Error) {
            onError(message!!)
        }
    }
}

fun <T> resourceLiveData(get: suspend () -> T) = liveData<Resource<T>> {
    emit(Resource.Loading())

    try {
        emit(Resource.Success(get()))
    } catch (exception: Exception) {
        emit(Resource.Error(exception.message ?: "Unknown error."))
    }
}