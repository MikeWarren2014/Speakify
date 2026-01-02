package com.mikewarren.speakify.activities

class ActivityProvider private constructor() {
    private val _defaultActivityClass = MainActivity::class.java
    private var activityClass : Class<*> = MainActivity::class.java
    var onDone: () -> Unit = {}

    fun getActivityClass(): Class<*> {
        return activityClass
    }
    fun setActivityClass(activityClass: Class<*>, onDone: () -> Unit = {})  {
        this.activityClass = activityClass
        this.onDone = onDone
    }

    fun resetActivityClass() {
        this.activityClass = _defaultActivityClass
    }


    companion object {
        var _instance: ActivityProvider? = null

        fun GetInstance() : ActivityProvider {
            if (_instance == null) {
                _instance = ActivityProvider()
            }

            return _instance!!

        }

    }
}