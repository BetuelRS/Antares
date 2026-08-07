package pt.antares.app.feature.fasting

interface FastingNotifier {

    fun scheduleGoal(sessionId: String, targetEndAt: Long)

    fun cancel(sessionId: String)
}

class NoopFastingNotifier : FastingNotifier {
    override fun scheduleGoal(sessionId: String, targetEndAt: Long) {}
    override fun cancel(sessionId: String) {}
}
