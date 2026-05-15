package com.tcmanna.tcsaddon.events.core

import com.odtheking.odin.OdinMod.mc
import com.tcmanna.tcsaddon.events.WorldLoadEndEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents

object CustomEventDispatcher {

    private var tabLoaded = false

    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            if (!tabLoaded && !mc.connection?.listedOnlinePlayers.isNullOrEmpty()) {
                tabLoaded = true
                WorldLoadEndEvent().postAndCatch()
            }
        }

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            tabLoaded = false
        }
    }
}