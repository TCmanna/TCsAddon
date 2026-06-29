package com.tcmanna.tcsaddon.features.impl.render

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.concurrent.ConcurrentHashMap




object ChestESP: Module(
    name = "Chest ESP",
    description = "just chest ESP."
) {
    val color by ColorSetting("Color", Color("FFFF008A"), true, "")
    val autoOpen by BooleanSetting("Auto Open", false, "")

    var renderList: MutableList<BlockPos> = ArrayList()
    var lastClickTime = 0L
    var lastClickPos: BlockPos? = null
    val chestCache = ConcurrentHashMap.newKeySet<ChestBlockEntity>()
    var backDown = 0

    init {
        on<TickEvent.Start> {
            val player = mc.player?: return@on
            val level = mc.level?: return@on

            if (backDown > 0) {
                backDown--
                if (backDown == 0) mc.options.keyAttack.isDown = true
            }

            renderList.clear()
            renderList.addAll(chestCache.map { it.blockPos })

            if (!autoOpen || LocationUtils.currentArea != Island.CrystalHollows) return@on

            if (mc.screen != null) return@on
            if (System.currentTimeMillis() - lastClickTime < 150) return@on
            val hit = mc.hitResult?: return@on


            if (hit.type == HitResult.Type.BLOCK) {
                val blockPos = (hit as BlockHitResult).blockPos
                val blockEntity = level.getBlockEntity(blockPos)?: return@on
                if (blockEntity is ChestBlockEntity && lastClickPos != blockPos) {
                    if (blockEntity.getOpenNess(0f) > 0f) return@on

                    backDown = if (mc.options.keyAttack.isDown) 3 else 0
                    mc.options.keyAttack.isDown = false
                    mc.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
                    if (!player.isCrouching) player.swing(InteractionHand.MAIN_HAND)
                    lastClickTime = System.currentTimeMillis()
                    lastClickPos = blockPos
                }
            }
        }

        on<RenderEvent.Extract> {
            if (renderList.isEmpty()) return@on
            renderList.forEach {
                val aabb = AABB(it)
                drawWireFrameBox(aabb, color)
                drawFilledBox(aabb, color)
            }
        }

        on<WorldEvent.Load> {
            chestCache.clear()
            renderList.clear()
        }
    }

    override fun onEnable() {
        super.onEnable()
        chestCache.clear()
        renderList.clear()
    }
}