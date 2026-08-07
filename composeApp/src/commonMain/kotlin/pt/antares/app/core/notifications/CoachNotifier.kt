package pt.antares.app.core.notifications

interface CoachNotifier {
    fun notifyReportReady()
}

class NoopCoachNotifier : CoachNotifier {
    override fun notifyReportReady() {}
}
