package com.tcmanna.tcsaddon.events

import com.odtheking.odin.events.core.CancellableEvent

abstract class MouseEvent {
    class Click(val button: Int, val state: Boolean) : CancellableEvent()
    class Scroll(val horizontal: Double, val vertical: Double) : CancellableEvent()
    class Move(val mx: Double, val my: Double) : CancellableEvent()
}