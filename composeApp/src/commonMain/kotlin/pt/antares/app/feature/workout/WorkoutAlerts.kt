package pt.antares.app.feature.workout

interface WorkoutAlerts {

    fun scheduleRestEnd(seconds: Int)

    fun cancelRestEnd()

    fun setSessionOngoing(active: Boolean)
}

class NoopWorkoutAlerts : WorkoutAlerts {
    override fun scheduleRestEnd(seconds: Int) {}
    override fun cancelRestEnd() {}
    override fun setSessionOngoing(active: Boolean) {}
}
