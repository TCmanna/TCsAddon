package com.tcmanna.tcsaddon.events

import net.minecraft.network.chat.Component
import com.odtheking.odin.events.core.Event

class ClientModMessage(val message: String, val component: Component) : Event