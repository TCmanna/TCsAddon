package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.sendCommand
import com.tcmanna.tcsaddon.events.TickEventStart
import com.tcmanna.tcsaddon.features.impl.skyblock.AutoCarnivalZombie.calcYawPitch
import com.tcmanna.tcsaddon.utils.Animation
import com.tcmanna.tcsaddon.utils.ControlSystem
import com.tcmanna.tcsaddon.utils.RotationUtils.rotateSmoothly
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3

object AutoCarnivalFruit: Module(
    name = "Auto Carnival Fruit",
    description = "Auto dig."
) {
    private val noti by BooleanSetting("Request SkyHanni Guesses", false, "")
    private val autoRestart by BooleanSetting("Auto Restart", false, "")
    private val rotateSpeed by NumberSetting("Rotate Speed", 120f, 50f, 200f, 10f, "", "ms")

    private val endTpPos = Vec3(-105.5, 73.0, 30.5)
    private val startTpPos = Vec3(-106.5, 73.0, 24.5)
    private val npcPos = Vec3(-107.0, 74.5, 28.0)
    private val walkPos = Vec3(-108.5, 73.0, 22.5)

    private var sendCommand = false
    private var doWalk = false
    private var aimDelay = 0
    private var inGame = false
    private var inAim = false
    private var lastPos = BlockPos(0, 0, 0)
    @JvmField
    var guessPos: BlockPos = BlockPos(0, 0, 0)

    init {
        on<TickEvent.End> {
            val player = mc.player ?: return@on
            val level = mc.level?: return@on

            if (doWalk) {
                runMid()
                return@on
            }

            val item = player.mainHandItem
            val id = item.itemId
            if (id == "CARNIVAL_SHOVEL" && inGame && !inAim && lastPos != guessPos) {
                val vec = Corner.matchCorner(guessPos) ?: Vec3(guessPos).addVec(x = 0.5, y = 1, z = 0.5)
                val calcYawPitch = calcYawPitch(vec)?: return@on
                lastPos = guessPos
                mc.options.keyAttack.isDown = false
                inAim = true
                player.rotateSmoothly(
                    calcYawPitch.first,
                    calcYawPitch.second,
                    rotateSpeed,
                    Animation.Style.Linear
                ) {
                    inAim = false
                    mc.options.keyAttack.isDown = true
                }
            }
        }

        on<TickEventStart> {
            if (aimDelay > 0 && --aimDelay == 0) talkNPC()
        }

        onReceive<ClientboundPlayerPositionPacket> {
            if (change.position == startTpPos) {
                inGame = true
                doWalk = true
            }
            if (change.position == endTpPos) {
                reset()
                if (autoRestart) aimDelay = 10
            }
        }

        on<MessageEvent.Chat> {
            if (!autoRestart) return@on
            if (message == "[NPC] Carnival Pirateman: Would ye like to do some Fruit Digging?") {
                sendCommand = true
            }
            if (message.contains("[Aye sure do!]") && sendCommand) {
                sendCommand("selectnpcoption carnival_pirateman r_2_1")
                sendCommand = false
            }
        }

        on<LevelEvent.Load> {
            reset()
        }
    }

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    private fun runMid() {
        ControlSystem.fullRelease()
        val player = mc.player?: return
        val distance = player.distanceToSqr(walkPos)
        if (distance < 0.1) {
            ControlSystem.haltMovement()
            doWalk = false
            Utils.realRightClick()
            return
        }
        ControlSystem.setMovementToCoords(walkPos)
    }

    private fun talkNPC() {
        val player = mc.player?: return

        val calcYawPitch = calcYawPitch(npcPos)?: return

        player.rotateSmoothly(
            calcYawPitch.first,
            calcYawPitch.second,
            200f,
            Animation.Style.Linear
        ) {
            Utils.realRightClick()
        }
    }

    private fun reset() {
        mc.options.keyAttack.isDown = false
        doWalk = false
        inGame = false
        inAim = false
        lastPos = BlockPos(0, 0, 0)
    }

    enum class Corner(val blockPos: BlockPos, val fixVec: Vec3) {
        EN(BlockPos(-106, 72, 19), Vec3(-105.9, 73.0, 19.9)),
        ES(BlockPos(-106, 72, 25), Vec3(-105.9, 73.0, 25.1)),
        WS(BlockPos(-112, 72, 25), Vec3(-111.1, 73.0, 25.1)),
        WN(BlockPos(-112, 72, 19), Vec3(-111.1, 73.0, 19.9));

        companion object {
            fun matchCorner(input: BlockPos): Vec3? {
                return entries.find { it.blockPos == input }?.fixVec
            }
        }
    }
}