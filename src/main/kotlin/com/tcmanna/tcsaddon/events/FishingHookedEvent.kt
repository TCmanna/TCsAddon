package com.tcmanna.tcsaddon.events

import com.odtheking.odin.events.core.Event

class FishingHookedEvent(val type: Type) : Event {
    enum class Type {
        Sound,
        SkyBlock
    }
}

