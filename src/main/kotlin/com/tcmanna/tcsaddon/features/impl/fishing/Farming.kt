package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.core.BlockPos

//nan
object Farming : Module(
    name = "Farming",
    category = Category.custom("Fishing"),
    description = "Test."
) {
    private val delaySetting by NumberSetting("delay", 0, 0, 20, 1, "", "tick")
    private val cropType by SelectorSetting("Crop Type", "wheat", CropType.entries.map { it.cropName }.toList(), "")

    private var lastBlockPos: BlockPos? = null
    private var pendingAction: (() -> Unit)? = null
    private var delay = 0

    init {
        on<TickEventStart> {
            val player = mc.player?: return@on
            val level = mc.level?: return@on

            mc.screen?.let {
                onKeybind()
                return@on
            }

            if (pendingAction != null) {
                if (--delay <= 0) {
                    pendingAction?.invoke()
                    pendingAction = null
                }
                return@on
            }

            val playerBp = player.blockPosition()

            checkBlockPos(playerBp)?.let {
                pendingAction = it
                delay = delaySetting
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        lastBlockPos?.let { checkBlockPos(it)?.invoke() }

        val type = CropType.entries[cropType]

        jumpRotation(type.yw)
        if (type.holdW) mc.options.keyUp.isDown = true
        val keyAttack = mc.options.keyAttack
        keyAttack.isDown = true
        KeyMapping.click((keyAttack as KeyMappingAccessor).boundKey)
    }

    override fun onDisable() {
        super.onDisable()
        releaseKey()
        mc.options.keyUp.isDown = false
        mc.options.keyAttack.isDown = false
    }

    private fun checkBlockPos(blockPos: BlockPos): (() -> Unit)? {
        val type = CropType.entries[cropType]
        val walkList = type.walkList

        when {
            walkList.w.contains(blockPos) -> return {
                releaseKey()
                mc.options.keyUp.isDown = true
                lastBlockPos = blockPos
            }


            walkList.a.contains(blockPos) -> return {
                releaseKey()
                mc.options.keyLeft.isDown = true
                lastBlockPos = blockPos
            }


            walkList.s.contains(blockPos) -> return {
                releaseKey()
                mc.options.keyDown.isDown = true
                lastBlockPos = blockPos

            }

            walkList.d.contains(blockPos) -> return {
                releaseKey()
                mc.options.keyRight.isDown = true
                lastBlockPos = blockPos

            }

            walkList.spawn == blockPos -> return {
                releaseKey()
                sendCommand("warp garden")
                lastBlockPos = null

            }
        }

        return null
    }

    private fun releaseKey() {
        mc.options.keyLeft.isDown = false
        mc.options.keyDown.isDown = false
        mc.options.keyRight.isDown = false
    }

    private fun jumpRotation(yw: Pair<Float, Float>) {
        val player = mc.player ?: return

        player.xRot = yw.first
        player.yRot = yw.second
    }

    enum class CropType(val cropName: String, val yw: Pair<Float, Float>, val holdW: Boolean, val walkList: WalkInfo) {
        Whale(
            "whale", 3f to 90f, true, WalkInfo(
                w = setOf(
                    BlockPos(-49, 68, -47),
                    BlockPos(-61, 68, -47),
                    BlockPos(-73, 68, -47),
                    BlockPos(-85, 68, -47),
                    BlockPos(-97, 68, -47),
                    BlockPos(-109, 68, -47),
                    BlockPos(-121, 68, -47),
                    BlockPos(-133, 68, -47),

                    BlockPos(-55, 68, 46),
                    BlockPos(-67, 68, 46),
                    BlockPos(-79, 68, 46),
                    BlockPos(-91, 68, 46),
                    BlockPos(-103, 68, 46),
                    BlockPos(-115, 68, 46),
                    BlockPos(-127, 68, 46)
                ),
                a = setOf(
                    BlockPos(-55, 68, -47),
                    BlockPos(-67, 68, -47),
                    BlockPos(-79, 68, -47),
                    BlockPos(-91, 68, -47),
                    BlockPos(-103, 68, -47),
                    BlockPos(-115, 68, -47),
                    BlockPos(-127, 68, -47),
                    BlockPos(-139, 68, -47)
                ),
                d = setOf(
                    BlockPos(-49, 68, 46),
                    BlockPos(-61, 68, 46),
                    BlockPos(-73, 68, 46),
                    BlockPos(-85, 68, 46),
                    BlockPos(-97, 68, 46),
                    BlockPos(-109, 68, 46),
                    BlockPos(-121, 68, 46),
                    BlockPos(-133, 68, 46)
                ),
                spawn = BlockPos(-139, 68, 46)
            )
        ),
        Mushroom(
            "mushroom", 6.7f to -16f, false, WalkInfo(
                a = setOf(
                    BlockPos(-239, 74, 69),
                    BlockPos(-239, 71, 63),
                    BlockPos(-239, 68, 57)
                ),
                s = setOf(
                    BlockPos(237, 74, 69),
                    BlockPos(237, 71, 63),
                    BlockPos(237, 68, 57)
                ),
                d = setOf(
                    BlockPos(237, 74, 63),
                    BlockPos(237, 71, 57),
                    BlockPos(237, 68, 51)
                ),
                spawn = BlockPos(-239, 68, 51)
            )
        ),
        Cactus(
            "cactus", -32f to 0f, false, WalkInfo(
                a = setOf(
                    BlockPos(-239, 72, 81),
                    BlockPos(-239, 72, 75),
                    BlockPos(-239, 67, 72),
                    BlockPos(-239, 67, 66)
                ),
                s = setOf(
                    BlockPos(238, 72, 81),
                    BlockPos(238, 67, 72),
                    BlockPos(-239, 72, 78),
                    BlockPos(-239, 67, 75),
                    BlockPos(-239, 67, 69),
                ),
                d = setOf(
                    BlockPos(238, 72, 78),
                    BlockPos(238, 67, 75),
                    BlockPos(238, 67, 69)
                ),
                spawn = BlockPos(238, 67, 66)
            )
        ),
        Carrot(
            "carrot", 3f to 90f, true, WalkInfo(
                w = setOf(
                    BlockPos(47, 68, -143),
                    BlockPos(35, 68, -143),
                    BlockPos(23, 68, -143),
                    BlockPos(11, 68, -143),
                    BlockPos(-1, 68, -143),
                    BlockPos(-13, 68, -143),
                    BlockPos(-25, 68, -143),
                    BlockPos(-37, 68, -143),

                    BlockPos(41, 68, -50),
                    BlockPos(29, 68, -50),
                    BlockPos(17, 68, -50),
                    BlockPos(5, 68, -50),
                    BlockPos(-7, 68, -50),
                    BlockPos(-19, 68, -50),
                    BlockPos(-31, 68, -50)
                ),
                a = setOf(
                    BlockPos(41, 68, -143),
                    BlockPos(29, 68, -143),
                    BlockPos(17, 68, -143),
                    BlockPos(5, 68, -143),
                    BlockPos(-7, 68, -143),
                    BlockPos(-19, 68, -143),
                    BlockPos(-31, 68, -143),
                    BlockPos(-43, 68, -143)
                ),
                d = setOf(
                    BlockPos(47, 68, -50),
                    BlockPos(35, 68, -50),
                    BlockPos(23, 68, -50),
                    BlockPos(11, 68, -50),
                    BlockPos(-1, 68, -50),
                    BlockPos(-13, 68, -50),
                    BlockPos(-25, 68, -50),
                    BlockPos(-37, 68, -50)
                ),
                spawn = BlockPos(-43, 68, -50)
            )
        ),
        Potato(
            "potato", 3f to 90f, true, WalkInfo(
                w = setOf(
                    BlockPos(143, 68, -143),
                    BlockPos(131, 68, -143),
                    BlockPos(119, 68, -143),
                    BlockPos(107, 68, -143),
                    BlockPos(95, 68, -143),
                    BlockPos(83, 68, -143),
                    BlockPos(71, 68, -143),
                    BlockPos(59, 68, -143),

                    BlockPos(137, 68, -50),
                    BlockPos(125, 68, -50),
                    BlockPos(113, 68, -50),
                    BlockPos(101, 68, -50),
                    BlockPos(89, 68, -50),
                    BlockPos(77, 68, -50),
                    BlockPos(65, 68, -50)
                ),
                a = setOf(
                    BlockPos(137, 68, -143),
                    BlockPos(125, 68, -143),
                    BlockPos(113, 68, -143),
                    BlockPos(101, 68, -143),
                    BlockPos(89, 68, -143),
                    BlockPos(77, 68, -143),
                    BlockPos(65, 68, -143),
                    BlockPos(53, 68, -143)
                ),
                d = setOf(
                    BlockPos(143, 68, -50),
                    BlockPos(131, 68, -50),
                    BlockPos(119, 68, -50),
                    BlockPos(107, 68, -50),
                    BlockPos(95, 68, -50),
                    BlockPos(83, 68, -50),
                    BlockPos(71, 68, -50),
                    BlockPos(59, 68, -50)
                ),
                spawn = BlockPos(53, 68, -50)
            )
        )
    }

    data class WalkInfo(
        val w: Set<BlockPos> = emptySet(),
        val a: Set<BlockPos> = emptySet(),
        val s: Set<BlockPos> = emptySet(),
        val d: Set<BlockPos> = emptySet(),
        val spawn: BlockPos
    )
}
