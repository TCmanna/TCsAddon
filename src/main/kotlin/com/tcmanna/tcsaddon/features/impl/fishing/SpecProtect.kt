package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.ClickGUI
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.playSoundAtPlayer
import com.tcmanna.tcsaddon.TCsAddon
import com.tcmanna.tcsaddon.utils.Utils
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.animal.equine.ZombieHorse
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

object SpecProtect : Module(
    name = "Spec Protect",
    category = Category.custom("Fishing"),
    description = "Protect your auto fishing when admin spec or other player."
) {
    private val packetMove by BooleanSetting("Packet Move", true, "")
    private val aroundBlockCheck by BooleanSetting("Around Block Check", true, "")
    private val hotbarCheck by BooleanSetting("Hotbar Check", false, "")
    private val guiCheck by BooleanSetting("GUI Check", false, "")

    private val strictMode by BooleanSetting("Strict Mode", false, "")
    private val strictModeDropdown by DropdownSetting("Strict Mode Setting").withDependency { strictMode }
    private val positionCheck by BooleanSetting("Position Check", true, "").withDependency { strictModeDropdown && strictMode }
    private val rotationCheck by BooleanSetting("Rotation Check", true, "").withDependency { strictModeDropdown && strictMode }
    private val longCatchCheck by BooleanSetting("LongCatch Check", true, "").withDependency { strictModeDropdown && strictMode }
    private val longCatchSetting by NumberSetting("LongCatch Threshold", 30L, 5L, 100L, 1, "", "sec").withDependency { strictModeDropdown && strictMode && longCatchCheck }

    private val playerClose by DropdownSetting("Player Close")
    private val playerCheck by BooleanSetting("Enable Close Check", true, "Use </tca friend> to add friend list.")
        .withDependency { playerClose }
    private val closeRange by NumberSetting("Player Check Range", 5, 1, 20, 1, "")
        .withDependency { playerClose && playerCheck }
    val whiteListPlayer by ListSetting("White List Player", mutableListOf<String>())

    private val directMessage by DropdownSetting("Message Check")
    private val dmCheck by BooleanSetting("Enable DM Check", true, "")
        .withDependency { directMessage }
    private val msgInfo by StringSetting("DM Msg Contains", desc = "Empty for any dm.")
        .withDependency { directMessage && dmCheck }

    private val soundType by SelectorSetting("Sound Type", "Vanilla", listOf("Vanilla", "Meme"), "Select macro check alert sound.")

    private var startVec : Vec3? = null
    private var startRot : Vec2? = null
    private val startBlockList = ArrayList<Block>()
    private var startHotbar : Int? = null
    private var enableTime = 0L

    init {
        on<TickEvent.End> {
            if (mc.player == null) return@on

            val zombieHorseAround = zombieHorseAround()

            if (strictMode) {
                if (positionCheck && !RandomMove.isJumping && !zombieHorseAround) {
                    if (startVec != null && startVec != mc.player?.position()) {
                        triggerStaffCheck("Position Change")
                        return@on
                    }
                }
                if (rotationCheck && !RandomMove.isAFKing) {
                    if (startRot != null) {
                        val xeq = startRot!!.x == mc.player!!.rotationVector.x
                        val yeq = startRot!!.y == mc.player!!.rotationVector.y
                        if (!xeq || !yeq) {
                            triggerStaffCheck("Rotate Change")
                            return@on
                        }
                    }
                }
                if (longCatchCheck) {
                    if (System.currentTimeMillis() - enableTime < 60000) return@on
                    if (System.currentTimeMillis() - AutoFish.mill > longCatchSetting * 1000) {
                        triggerStaffCheck("Catch Time to Long")
                        return@on
                    }
                }
            }
            if (aroundBlockCheck && !RandomMove.isJumping && !zombieHorseAround) {
                if (startBlockList.isNotEmpty()) {
                    val currentBlockLst = ArrayList<Block>()
                    val range = 5
                    for (x in mc.player!!.blockX - range until mc.player!!.blockX + range) {
                        for (y in mc.player!!.blockY - range until mc.player!!.blockY + range) {
                            for (z in mc.player!!.blockZ - range until mc.player!!.blockZ + range) {
                                currentBlockLst.add(mc.level!!.getBlockState(BlockPos(x, y, z)).block)
                            }
                        }
                    }
                    if (startBlockList != currentBlockLst) {
                        triggerStaffCheck("Around Block Change")
                        return@on
                    }
                }
            }
            if (hotbarCheck && !RandomMove.isAFKing) {
                if (startHotbar != null && startHotbar != mc.player?.inventory?.selectedSlot) {
                    if (!(KillWorm.enabled && mc.player?.inventory?.selectedSlot == KillWorm.weaponSlot - 1)) {
                        triggerStaffCheck("HotBar Selected Change")
                        return@on
                    }
                }
            }
            if (guiCheck) {
                if (mc.screen != null) {
                    if (mc.screen !is InventoryScreen && mc.screen !is ChatScreen && mc.screen !is ClickGUI) {
                        triggerStaffCheck("GUI Change")
                        return@on
                    }
                }
            }
            if (playerCheck) {
                val level = mc.level
                if (level != null) {
                    for (player in level.players()) {
                        if (player is LocalPlayer) continue
                        if (player == mc.player) continue
                        if (whiteListPlayer.contains(player.name.string.noControlCodes)) continue
                        if (player.position().distanceTo(mc.player!!.position()) < closeRange) {
                            triggerStaffCheck("Someone Close for you [${player.name}]")
                            return@on
                            break
                        }
                    }
                }
            }
        }

        on<ChatPacketEvent> {
            if (dmCheck) {
                if (value.startsWith("From ")) {
                    if (!msgInfo.isEmpty()) {
                        if (value.contains(msgInfo)) triggerStaffCheck("Direct Message Contains [$msgInfo]")
                    }
                    else triggerStaffCheck("Any Direct Message [$msgInfo]")
                }
            }
        }

        onReceive<ClientboundPlayerPositionPacket> {
            if (!packetMove) return@onReceive
            if (zombieHorseAround()) return@onReceive
            val player = mc.player?: return@onReceive

            val posChanged = if (player.position() != change.position)
                "Position: ${player.position()} -> ${change.position}" else ""

            val yawChanged = if (player.xRot != change.xRot)
                " Yaw: ${player.xRot} -> ${change.xRot}" else ""

            val pitchChanged = if (player.yRot != change.yRot)
                " Pitch: ${player.yRot} -> ${change.yRot}" else ""

            if (posChanged.isEmpty() && yawChanged.isEmpty() && pitchChanged.isEmpty())
                return@onReceive
            if (posChanged.isEmpty() && change.xRot == 0.0f && change.yRot == 0.0f) {
                if (player.xRot != 0.0f && player.yRot != 0.0f) {
                    return@onReceive
                }
            }

            triggerStaffCheck("Move Packet [$posChanged$yawChanged$pitchChanged]")
        }

        on<WorldEvent.Load> {
            onKeybind()
        }
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.player == null) {
            this.toggle()
            return
        }
        if (!Utils.playerHoldFishRod(mc.player)) {
            modMessage("Please holding fishing rod to enable.", prefix = "§3SpecProt §8»§r ")
            this.toggle()
            return
        }
        if (!AutoFish.enabled) AutoFish.onKeybind()
        enableTime = System.currentTimeMillis()
        startVec = mc.player?.position()
        startRot = mc.player?.rotationVector
        startHotbar = mc.player?.inventory?.selectedSlot

        startBlockList.clear()
        if (mc.level != null) {
            val range = 5
            for (x in mc.player!!.blockX - range until mc.player!!.blockX + range) {
                for (y in mc.player!!.blockY - range until mc.player!!.blockY + range) {
                    for (z in mc.player!!.blockZ - range until mc.player!!.blockZ + range) {
                        startBlockList.add(mc.level!!.getBlockState(BlockPos(x, y, z)).block)
                    }
                }
            }
        }
    }

    fun zombieHorseAround(): Boolean {
        val level = mc.level?: return false
        val player = mc.player?: return false

        val range = 5
        val aabb = AABB(
            player.x - range,
            player.y - range,
            player.z - range,
            player.x + range,
            player.y + range,
            player.z + range
        )

        val zombieHorses = level.getEntitiesOfClass(ZombieHorse::class.java, aabb)
        return zombieHorses.isNotEmpty()
    }

    fun triggerStaffCheck(reason: String) {
        if (AutoFish.enabled) AutoFish.onKeybind()
        this.toggle()

        if (soundType == 1) {
            mc.player?.let { mc.execute { playSoundAtPlayer(TCsAddon.soundEvent) }}
        }
        else {
            Thread.startVirtualThread {
                for (i in 0 until 10) {
                    mc.player?.let { mc.execute { playSoundAtPlayer(SoundEvents.ANVIL_PLACE) } }
                    Thread.sleep(100)
                }
            }
        }

        modMessage("!!!Macro Checking!!!", prefix = "§3SpecProt §8»§r ")
        modMessage("!!!Macro Checking!!!", prefix = "§3SpecProt §8»§r ")
        modMessage("!!!Macro Checking!!!", prefix = "§3SpecProt §8»§r ")
        modMessage("Reason: $reason", prefix = "§3SpecProt §8»§r ")

        val handle = mc.window.handle()
        GLFW.glfwFocusWindow(handle)
        GLFW.glfwShowWindow(handle)

        StaffActionController.start(StaffActionController.randomSequence())
        return
    }
}