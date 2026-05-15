package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.features.impl.render.CorpseESP
import com.tcmanna.tcsaddon.features.impl.render.CorpseESP.Corpse
import com.tcmanna.tcsaddon.features.impl.render.LittlefootESP.SKIN
import com.tcmanna.tcsaddon.features.impl.render.LittlefootESP.getEntityTextureString
import net.minecraft.world.entity.EquipmentSlot

//nan
object Debug : Module(
    name = "DEBUG",
    category = Category.custom("Fishing"),
    description = "Test."
) {
    override fun onEnable() {
        super.onEnable()

        mc.level?.players()?.forEach {
            modMessage(it.getEntityTextureString())
        }
    }

}