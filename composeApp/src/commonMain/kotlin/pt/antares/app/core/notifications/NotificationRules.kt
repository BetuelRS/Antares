package pt.antares.app.core.notifications

import pt.antares.app.core.model.MealSlot

object NotificationRules {

    const val DEFAULT_QUIET_START_MIN = 22 * 60
    const val DEFAULT_QUIET_END_MIN = 8 * 60

    fun isQuiet(nowMin: Int, startMin: Int, endMin: Int): Boolean {
        if (startMin == endMin) return false
        return if (startMin < endMin) {
            nowMin in startMin until endMin
        } else {
            nowMin >= startMin || nowMin < endMin
        }
    }

    fun shouldRemindMeal(
        slot: MealSlot,
        loggedSlots: Set<MealSlot>,
        enabled: Boolean,
    ): Boolean = enabled && slot !in loggedSlots

    fun nextAllowedMinute(nowMin: Int, quietStartMin: Int, quietEndMin: Int, quietEnabled: Boolean): Int {
        if (!quietEnabled) return nowMin
        return if (isQuiet(nowMin, quietStartMin, quietEndMin)) quietEndMin else nowMin
    }
}
