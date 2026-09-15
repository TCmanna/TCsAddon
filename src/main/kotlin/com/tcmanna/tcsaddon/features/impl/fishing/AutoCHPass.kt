package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand

object AutoCHPass : Module(
    name = "Auto CH Pass",
    category = Category.custom("Fishing"),
    description = "Auto buying Crystal Hollows pass."
) {
    init {
        on<MessageEvent.Chat> {
            if (message == "Your pass to the Crystal Hollows will expire in 1 minute") {
                sendCommand("purchasecrystallhollowspass")
            }
        }
    }
}