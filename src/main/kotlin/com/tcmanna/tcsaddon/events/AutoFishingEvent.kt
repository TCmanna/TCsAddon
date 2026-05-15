package com.tcmanna.tcsaddon.events

import com.odtheking.odin.events.core.CancellableEvent

abstract class AutoFishingEvent : CancellableEvent() {
    class Before() : AutoFishingEvent()
    class After(): AutoFishingEvent()
}