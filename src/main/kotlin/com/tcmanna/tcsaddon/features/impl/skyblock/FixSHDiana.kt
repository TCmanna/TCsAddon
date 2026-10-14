package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module

object FixSHDiana: Module(
    name = "Fix SH Diana",
    description = "Fix SkyHanni Burrow Guess"
) {
    val cancelRCBlock by BooleanSetting("Cancel RC Block", true, "Cancel when right click spade will block next click.")
    val fixRemoveGuess by BooleanSetting("Fix Remove Guess", true, "Now only holding spade can closing remove burrow.")
}