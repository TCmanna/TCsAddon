package com.tcmanna.tcsaddon.events.core

import com.odtheking.odin.OdinMod.mc
import com.tcmanna.tcsaddon.events.WorldLoadEndEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents

object CustomEventDispatcher {

    private var tabLoaded = false

    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            if (!tabLoaded && !mc.connection?.listedOnlinePlayers.isNullOrEmpty()) {
                tabLoaded = true
                WorldLoadEndEvent().postAndCatch()
            }
        }

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            tabLoaded = false
        }
    }
}