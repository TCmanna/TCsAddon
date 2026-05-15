package com.tcmanna.tcsaddon.features.impl.boss

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.BlockInteractEvent
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.toFixed
import com.tcmanna.tcsaddon.utils.Animation
import com.tcmanna.tcsaddon.utils.AuraManager
import com.tcmanna.tcsaddon.utils.RotationUtils.rotateSmoothly
import com.tcmanna.tcsaddon.utils.getDirection
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.random.Random

//copy from quoi https://github.com/pigeonlover1998/quoi
object AutoSS: Module(
    name = "Auto SS",
    description = "Automatically completes Simon says device."
) {
    private val auto by BooleanSetting("Enable Auto", true, "")
    private val delay by NumberSetting("Delay", 360f, 50f, 500f, 10f, "Click delay", unit = "ms").withDependency { auto }
    private val startDelay by NumberSetting("Start delay", 200, 50, 300, 1, "Delay between clicks when skipping a button.", unit = "ms").withDependency { auto }
    private val smoothRotate by BooleanSetting("Smooth rotate", true, "").withDependency { auto }
    private val rotateStyle by SelectorSetting("Style", Animation.Style.EaseInOutQuint.name, Animation.Style.entries.map { it.name }.toList(), "").withDependency { smoothRotate }

    private val announceTime by BooleanSetting("Announce time", true, desc = "Announces device completion time in party chat")
    private val forceDevice by BooleanSetting("Force device", true, "")
    private val resetSS by ActionSetting("Reset", ""){ fullReset() }

    private var lastClickTime = 0L
    private var progress = 0
    private var doneFirst = false
    private var doingSS = false
    private var clicked = false
    private var clicks = ArrayList<BlockPos>()
    private var clickedButton: BlockPos? = null
    private var allButtons = ArrayList<BlockPos>()
    private val startButton = BlockPos(110, 121, 91)
    private val standBox = AABB(108.0, 120.0, 90.0, 115.0, 125.0, 95.0)

    private var startActive = false
    private var startStep = 0
    private var nextActionTime = 0L
    private var lastManualReset = 0L
    private var startTime = 0L

    init {
        on<WorldEvent.Load> {
            fullReset()
        }

        on<ChatPacketEvent> {
            if (value == "[BOSS] Goldor: Who dares trespass into my domain?") start()
        }

        on<PacketEvent.Send> {
            val level = mc.level?: return@on
            val player = mc.player?: return@on
            if (packet is ServerboundUseItemOnPacket) {
                val usePack = packet as ServerboundUseItemOnPacket
                if (startActive || usePack.hitResult.blockPos != startButton) return@on

                val isActive = level.getEntitiesOfClass(ArmorStand::class.java, standBox) {
                    it.distanceTo(player) < 6 && it.displayName.string.contains("Device Active")
                }.isNotEmpty()
                if (isActive) return@on

                if (System.currentTimeMillis() - lastManualReset > 500) {
                    if (auto) {
                        cancel()
                        fullReset()
                        start()
                    } else {
                        fullReset()
                        doingSS = true
                        startTime = System.currentTimeMillis()
                    }
                    lastManualReset = System.currentTimeMillis()
                }
            }
        }

        on<BlockUpdateEvent> {
            val player = mc.player?: return@on
            if (pos.y !in 120..123 || pos.z !in 92..95) return@on

            if (pos.x == 111 && updated.block == Blocks.SEA_LANTERN) {
                val buttonPos = BlockPos(110, pos.y, pos.z)
                if (clicks.getOrNull(0) == buttonPos) {
                    progress = 0
                    if (auto && smoothRotate && doingSS) {
                        player.rotateSmoothly(
                            getDirection(buttonPos.randomVec),
                            duration = delay,
                            style = Animation.Style.getFromIndex(rotateStyle)
                        )
                    }
                }

                if (clicks.size == 2 && clicks[0] == buttonPos && !doneFirst) {
                    doneFirst = true
                    clicks.removeFirst()
                    if (allButtons.isNotEmpty()) allButtons.removeFirst()
                }

                if (!clicks.contains(buttonPos)) {
                    progress = 0
                    clicks.add(buttonPos)
                    allButtons.add(buttonPos)
                }
                return@on
            }

            if (pos.x == 110) {
                if (updated.block == Blocks.STONE_BUTTON && updated.hasProperty(BlockStateProperties.POWERED) && updated.getValue(BlockStateProperties.POWERED)) {
                    val i = clicks.indexOf(pos)
                    if (i != -1) {
                        progress = i + 1
                    }
                }
            }
        }

        on<TickEvent.Start> {
            val player = mc.player?: return@on
            if (startActive) {
                if (!auto) {
                    doingSS = true
                    startTime = System.currentTimeMillis()
                    startActive = false
                } else if (System.currentTimeMillis() >= nextActionTime) {
                    when (startStep) {
                        0, 1 -> {
                            reset()
                            clickButton(startButton)

                            val waitMs = Random.nextInt(startDelay, (startDelay * 1.136).toInt())
                            nextActionTime = System.currentTimeMillis() + waitMs
                            startStep++
                        }
                        2 -> {
                            clickButton(startButton)
                            doingSS = true
                            startTime = System.currentTimeMillis()
                            startActive = false
                        }
                    }
                }
                return@on
            }

            if (!doingSS || System.currentTimeMillis() - lastClickTime < delay) return@on
            if (player.distanceToSqr(startButton.center) > 25) return@on

            val canClick = mc.level?.getBlockState(BlockPos(110, 123, 92))?.block == Blocks.STONE_BUTTON

            if (canClick || ( auto && doneFirst)) {

                if (!doneFirst && clicks.size == 3) {
                    clicks.removeAt(0)
                    if (allButtons.isNotEmpty()) allButtons.removeAt(0)
                }

                doneFirst = true

                if (auto) clicks.getOrNull(progress)?.let { nextPos ->
                    if (mc.level?.getBlockState(nextPos)?.block == Blocks.STONE_BUTTON) {
                        clickButton(nextPos)
                        progress++
                    }
                }
            }
        }

        on<RenderEvent.Last> {
            val player = mc.player?: return@on
            if (player.distanceToSqr(startButton.center) > 1600) return@on

            if (doingSS) {
                var finished = false
                var active = forceDevice
                val level = mc.level?: return@on
                level.getEntitiesOfClass(ArmorStand::class.java, standBox)
                { it.distanceTo(player) < 6 }.forEach {
                    val name = it.displayName.string.noControlCodes
                    if ("Device Active" in name) finished = true
                    if ("Device" in name) active = true
                }

                if (finished) {
                    val time = formatTime(System.currentTimeMillis() - startTime)
                    modMessage("Simon Says took $time")
                    if (announceTime) sendCommand("pc Simon Says took $time")
                    fullReset()
                } else if (!active) fullReset()

                if (System.currentTimeMillis() - lastClickTime > delay) clickedButton = null
            }

            if (!doingSS) return@on
        }
    }

    private fun fullReset() {
        startActive = false
        reset()
    }

    private fun reset() {
        allButtons.clear()
        clicks.clear()
        progress = 0
        doneFirst = false
        doingSS = false
        clicked = false
        startTime = 0L
    }

    private fun start() {
        val player = mc.player?: return
        if (player.distanceToSqr(startButton.center) > 25) return

        if (!startActive && !doingSS) {
            reset()
            clicked = true

            startActive = true
            startStep = 0
            nextActionTime = System.currentTimeMillis()
        }
    }

    private fun clickButton(pos: BlockPos) {
        val player = mc.player?: return
        if (player.distanceToSqr(pos.center) > 25) return

        val shouldSmooth = smoothRotate && (pos != startButton || startStep == 0)
        val extraDelay = if (pos != startButton) 100L else 0L
        lastClickTime = System.currentTimeMillis() + extraDelay

        if (shouldSmooth) {

            player.rotateSmoothly(getDirection(pos.randomVec), duration = delay, style = Animation.Style.getFromIndex(rotateStyle)) {
                if (player.distanceToSqr(pos.center) > 25) return@rotateSmoothly
                clickedButton = pos
                AuraManager.interactBlock(pos)
                player.swing(InteractionHand.MAIN_HAND)
            }
        } else {
            clickedButton = pos
            AuraManager.interactBlock(pos)
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    private val BlockPos.randomVec: Vec3
        get() {
        val yy = Random.nextDouble(-0.1, 0.1)
        val zz = Random.nextDouble(-0.1, 0.1)
        return Vec3(x + 0.9375, y + 0.5 + yy, z + 0.5 + zz)
    }

    fun formatTime(
        time: Long,
        decimalPlaces: Int = 2,
        showDays: Boolean = true,
        showHours: Boolean = true,
        showMinutes: Boolean = true,
        showSeconds: Boolean = true,
        forceIfEmpty: Boolean = true
    ): String {
        if (time == 0L) return "0s"
        var remaining = time

        val daysVal = (remaining / 86_400_000).toInt()
        remaining -= daysVal * 86_400_000
        val hoursVal = (remaining / 3_600_000).toInt()
        remaining -= hoursVal * 3_600_000
        val minutesVal = (remaining / 60_000).toInt()
        remaining -= minutesVal * 60_000
        val secondsVal = remaining / 1000f

        val days = daysVal.let { if (showDays && it > 0) "${it}d " else "" }
        val hours = hoursVal.let { if (showHours && it > 0) "${it}h " else "" }
        val minutes = minutesVal.let { if (showMinutes && it > 0) "${it}m " else "" }
        val seconds = secondsVal.let { if (showSeconds && it > 0f) "${it.toFixed(decimalPlaces)}s" else "" }

        val result = "$days$hours$minutes$seconds".trim()
        if (result.isNotEmpty() || !forceIfEmpty) return result

        return when {
            secondsVal > 0f -> "${secondsVal.toFixed(decimalPlaces)}s"
            minutesVal > 0  -> "${minutesVal}m"
            hoursVal > 0    -> "${hoursVal}h"
            daysVal > 0     -> "${daysVal}d"
            else -> "0s"
        }
    }

}