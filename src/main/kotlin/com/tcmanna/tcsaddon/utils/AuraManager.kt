package com.tcmanna.tcsaddon.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.BlockInteractEvent
import com.odtheking.odin.utils.modMessage
import com.tcmanna.tcsaddon.mixin.accessors.MultiPlayerGameModeAccessor
import com.tcmanna.tcsaddon.utils.AuraManager.debugBox
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.multiplayer.prediction.PredictiveAction
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

object AuraManager {
    private val blockTasks = mutableListOf<BlockInteract>()
    private var interactBlockCd = 0

    private val recentClicks = mutableListOf<Vec3>()

    val executor: ScheduledExecutorService = ScheduledThreadPoolExecutor(4)

    fun interactBlock(pos: BlockPos, force: Boolean = false) {
        val task = BlockInteract(pos, force)
        if (interactBlockCd > 0) blockTasks.add(task) else task.execute()
    }

    fun debugBox(vec3: Vec3) {
        recentClicks.add(vec3)
        executor.schedule({recentClicks.remove(vec3) }, 500, TimeUnit.MILLISECONDS )
    }
}

class BlockInteract(private val pos: BlockPos, private val force: Boolean) {
    fun execute() {
        val hitResult = pos.getHitResult(force) ?: return

        mc.gameMode?.startPrediction { sequence ->
            ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                hitResult,
                sequence
            )
        }
        BlockInteractEvent(pos).postAndCatch()

        debugBox(hitResult.location)
    }
}

fun MultiPlayerGameMode.startPrediction(action: PredictiveAction) {
    val level = mc.level ?: return
    (this as MultiPlayerGameModeAccessor).invokeStartPrediction(level, action)
}