package com.mikewarren.speakify.data.events

interface Emittable<T> {
    class DataFetched<T>(val data: List<T>) : Emittable<T>
    object PermissionDenied : Emittable<Nothing>
    class FetchFailed<T>(val message: String) : Emittable<T>

    object RequestData : Emittable<Nothing>
}