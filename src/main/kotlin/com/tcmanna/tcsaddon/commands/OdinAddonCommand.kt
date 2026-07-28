package com.tcmanna.tcsaddon.commands

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.utils.modMessage
import com.tcmanna.tcsaddon.features.impl.fishing.SpecProtect

// Commands are handled via https://github.com/Stivais/Commodore
val odinAddonCommand = Commodore("tca") {

    val friendLiteral = literal("friend")

    friendLiteral.literal("add").runs { string: String ->
        if (SpecProtect.whiteListPlayer.contains(string)) {
            modMessage("already has this name")
        }
        else {
            SpecProtect.whiteListPlayer.add(string)
            modMessage("$string has been added")
        }
    }

    friendLiteral.literal("remove").runs { string: String ->
        if (SpecProtect.whiteListPlayer.removeIf { it == string }) {
            modMessage("$string has been removed")
        } else {
            modMessage("not found $string")
        }
    }


    friendLiteral.literal("list").runs {
        if (SpecProtect.whiteListPlayer.isEmpty()) {
            modMessage("TCA friend list is empty")
        }
        else {
            modMessage("\n" + SpecProtect.whiteListPlayer.joinToString("\n"))
        }
    }
}