package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.tcmanna.tcsaddon.utils.ControlSystem
import net.minecraft.world.phys.Vec3

//nan
object Debug : Module(
    name = "Debug",
    category = Category.custom("Fishing"),
    description = "Test."
) {

    private val tpPos = Vec3(-96.5, 70.0, 37.5)
    private val talkPos = Vec3(-100.5, 70.0, 39.5)

    init {
//        on<TickEventStart> {
//            ControlSystem.fullRelease()
//            val player = mc.player?: return@on
//            val level = mc.level?: return@on
//
//            val distance = player.distanceToSqr(talkPos)
//            if (distance < 1.5) {
//                ControlSystem.haltMovement()
//                this@Debug.toggle()
//                return@on
//            }
//            ControlSystem.setMovementToCoords(talkPos)
//        }
    }

    override fun onEnable() {
        super.onEnable()
    }

    override fun onDisable() {
     super.onDisable()
    }


}
