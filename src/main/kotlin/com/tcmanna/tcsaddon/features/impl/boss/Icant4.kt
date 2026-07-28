package com.tcmanna.tcsaddon.features.impl.boss

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.util.ArrayList
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

//copy from https://github.com/b34ut1ful/Hunchclient1.21/blob/main/src/client/java/dev/hunchclient/module/impl/dungeons/ICant4Module.java
object Icant4 : Module(
    name = "Icant4",
    description = "Auto I4 Dev"
) {
    val onlyTerminator by BooleanSetting("Only Terminator", true, "")

    val BLOCKS = arrayOf(
        intArrayOf(68, 130, 50), intArrayOf(66, 130, 50), intArrayOf(64, 130, 50),
        intArrayOf(68, 128, 50), intArrayOf(66, 128, 50), intArrayOf(64, 128, 50),
        intArrayOf(68, 126, 50), intArrayOf(66, 126, 50), intArrayOf(64, 126, 50)
    )

    // Settings
    private var enableTerminatorLogic = true
    private var debugMode = false

    private val done: MutableList<Int> = ArrayList<Int>()
    private var on4thDevice = false
    private var cancelNext = false

    private var prefireActive = false

    init {
        on<TickEvent.End> {
            if (mc.player == null) return@on

            // Reset cancelNext each tick (like original - it only blocks within same tick/event batch)
            cancelNext = false


            // Track player position for 4th device detection
            // Original: x > 63 && x < 64 && y == 127 && z > 35 && z < 36
            val x = mc.player!!.x
            val y = mc.player!!.y
            val z = mc.player!!.z

            val wasOnDevice = on4thDevice
            on4thDevice = x > 63 && x < 64 && y >= 126.5 && y <= 127.5 && z > 35 && z < 36


            // Check if holding Terminator
            val heldItem = mc.player!!.mainHandItem
            val holdingTerminator = enableTerminatorLogic && heldItem.item != Items.AIR && heldItem.item == Items.BOW
                    && isTerminator(heldItem)
            if (onlyTerminator && !holdingTerminator) return@on
            val useKey = (mc.options.keyUse as KeyMappingAccessor).boundKey

            // Check if terminal is actually active (has emerald blocks)
            var terminalActive = false
            if (on4thDevice && mc.level != null) {
                for (blockCoords in BLOCKS) {
                    val checkPos = BlockPos(blockCoords[0], blockCoords[1], blockCoords[2])
                    val block = mc.level!!.getBlockState(checkPos).block
                    if (block == Blocks.EMERALD_BLOCK) {
                        terminalActive = true
                        break
                    }
                }
            }


            // PREFIRE: Hold right-click while on device with Terminator AND terminal is active
            if (on4thDevice && holdingTerminator && terminalActive) {
                if (!prefireActive) {
                    // Start prefire - hold right-click
                    mc.options.keyUse.isDown = true
                    KeyMapping.set(useKey, true)
                    KeyMapping.click(useKey)

                    prefireActive = true
                    if (debugMode) {
                        modMessage(Component.literal("§a[ICant4] Prefire started - holding right-click"))
                    }
                }
            } else {
                if (prefireActive) {
                    // Stop prefire - release right-click
                    mc.options.keyUse.isDown = false
                    KeyMapping.set(useKey, false)

                    prefireActive = false
                    if (debugMode) {
                        modMessage(Component.literal("§c[ICant4] Prefire stopped"))
                    }
                }
            }

            if (!on4thDevice && wasOnDevice) {
                // Left device - clear done list (like original: while (done.length) done.pop())
                done.clear()
            }

            if (on4thDevice && !wasOnDevice && debugMode) {
                modMessage(Component.literal("§e[ICant4] 4th Device detected!"))
            }

        }

        on<BlockUpdateEvent> {
            if (!on4thDevice) return@on

            val mc = Minecraft.getInstance()
            if (mc.player == null) return@on

            val block = updated.block

            // Find block index in our 3x3 grid
            var index = -1
            for ((i, element) in BLOCKS.withIndex()) {
                if (pos.x == element[0] && pos.y == element[1] && pos.z == element[2]) {
                    index = i
                    break
                }
            }

            if (index == -1) return@on


            // Original: if (id == 159) done.push(index);
            // 159 = hardened clay/terracotta = block is solved
            if (isTerracotta(block)) {
                if (!done.contains(index)) {
                    done.add(index)
                }
                return@on  // Don't shoot at already solved blocks
            }


            // Original: if (id !== 133) return;
            // 133 = emerald block = shoot!
            if (block !== Blocks.EMERALD_BLOCK) {
                return@on
            }


            // Original: if (cancelNext) return;
            if (cancelNext) return@on


            // Original: if (item?.getID() !== 261) return;
            // 261 = bow
            val heldItem = mc.player!!.mainHandItem
            if (heldItem.item == Items.AIR || heldItem.item != Items.BOW) return@on


            // Check if it's a TERMINATOR
            var isTerminator = false
            if (enableTerminatorLogic) {
                isTerminator = isTerminator(heldItem)
            }


            // Calculate yaw and pitch - EXACT from original
            val angles: FloatArray = calculateAim(pos, index, isTerminator) ?: return@on

            val yaw = angles[0]
            val pitch = angles[1]

            if (yaw.isNaN() || pitch.isNaN()) return@on

            if (debugMode) {
                modMessage(
                    Component.literal(
                        String.format(
                            "§a[ICant4] Shooting block %d (yaw=%.1f, pitch=%.1f)%s",
                            index + 1, yaw, pitch, if (isTerminator) " [TERM]" else ""
                        )
                    )
                )
            }


            // INSTANT rotate - like original: rotate(yaw, pitch)
            mc.player!!.yRot = yaw
            mc.player!!.xRot = pitch


            // For Terminator with prefire: right-click is already held, just rotating triggers the shot
            // For normal bow: we need to click
            if (!prefireActive) {
                // Not in prefire mode - single click for normal bow
                mc.options.keyUse.isDown = true
                // Schedule release on next tick via a simple flag check in onTick would be complex,
                // so we just click and the bow's natural mechanics handle it
            }


            // If prefireActive is true, Terminator is already firing due to held right-click
            // The rotation change will cause the next shot to go to the new target

            // Original: cancelNext = true;
            cancelNext = true


            // For Terminator: mark ENTIRE ROW as done (spread hits all 3 blocks)
            // For normal bow: mark only the current block as done
            if (isTerminator) {
                val row = index / 3 // 0, 1, or 2
                val rowStart = row * 3
                for (i in rowStart..<rowStart + 3) {
                    if (!done.contains(i)) {
                        done.add(i)
                    }
                }
            } else {
                if (!done.contains(index)) {
                    done.add(index)
                }
            }
        }

    }

    override fun onDisable() {
        on4thDevice = false
        cancelNext = false
        done.clear()
        prefireActive = false

        super.onDisable()
    }

    override fun onEnable() {
        done.clear()
        cancelNext = false

        // Stop prefire - release right-click
        if (prefireActive) {
            mc.options.keyUse.isDown = false
            prefireActive = false
        }

        super.onEnable()
    }

    private fun isTerminator(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty) {
            return false
        }

        // Regular check: Look for TERMINATOR in NBT data
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null) {
            val nbt = customData.copyTag()
            if (nbt.contains("ExtraAttributes")) {
                val extraAttrOpt = nbt.getCompound("ExtraAttributes")
                if (extraAttrOpt.isPresent) {
                    val extraAttr = extraAttrOpt.get()
                    if (extraAttr.contains("id")) {
                        val idOpt = extraAttr.getString("id")
                        if (idOpt.isPresent) {
                            val id = idOpt.get()
                            return "TERMINATOR" == id
                        }
                    }
                }
            }
        }

        // Check by item name as fallback
        val itemName = stack.hoverName.string.lowercase(Locale.getDefault())
        return itemName.contains("terminator")
    }

    private fun isTerracotta(block: Block?): Boolean {
        return block == Blocks.TERRACOTTA || block == Blocks.RED_TERRACOTTA ||
                block == Blocks.ORANGE_TERRACOTTA || block == Blocks.YELLOW_TERRACOTTA ||
                block == Blocks.LIME_TERRACOTTA || block == Blocks.GREEN_TERRACOTTA ||
                block == Blocks.CYAN_TERRACOTTA || block == Blocks.LIGHT_BLUE_TERRACOTTA ||
                block == Blocks.BLUE_TERRACOTTA || block == Blocks.PURPLE_TERRACOTTA ||
                block == Blocks.MAGENTA_TERRACOTTA || block == Blocks.PINK_TERRACOTTA ||
                block == Blocks.WHITE_TERRACOTTA || block == Blocks.LIGHT_GRAY_TERRACOTTA ||
                block == Blocks.GRAY_TERRACOTTA || block == Blocks.BLACK_TERRACOTTA ||
                block == Blocks.BROWN_TERRACOTTA
    }

    private fun calculateAim(position: BlockPos, index: Int, isTerminator: Boolean): FloatArray? {
        if (mc.player == null) return null

        val targetX: Double
        val targetY: Double
        val targetZ: Double

        if (isTerminator) {
            // TERMINATOR special logic - aim at edges to hit multiple blocks
            when (index % 3) {
                0 -> {
                    targetX = position.x - 0.5
                    targetY = (position.y + 1).toDouble()
                    targetZ = position.z.toDouble()
                }

                1 -> {
                    val f1 = done.contains(index - 1) // Left neighbor done?
                    val f2 = done.contains(index + 1) // Right neighbor done?
                    targetX = if (f1 && !f2) {
                        // Left done, right not -> aim LEFT to hit middle + right
                        position.x - 0.5
                    } else if (f2 && !f1) {
                        // Right done, left not -> aim RIGHT to hit middle + left
                        position.x + 1.5
                    } else {
                        // Both or neither done -> random direction
                        position.x + 0.5 + (if (Math.random() < 0.5) -1 else 1)
                    }
                    targetY = (position.y + 1).toDouble()
                    targetZ = position.z.toDouble()
                }

                2 -> {
                    targetX = position.x + 1.5
                    targetY = (position.y + 1).toDouble()
                    targetZ = position.z.toDouble()
                }

                else -> return null
            }
        } else {
            // Regular bow - aim at block center
            targetX = position.x + 0.5
            targetY = (position.y + 1).toDouble()
            targetZ = position.z.toDouble()
        }

        // Get player eye position
        val eyePos = Vec3(
            mc.player!!.x,
            mc.player!!.y + mc.player!!.getEyeHeight(mc.player!!.pose),
            mc.player!!.z
        )

        // Calculate difference vector
        val difference = Vec3(
            targetX - eyePos.x,
            targetY - eyePos.y,
            targetZ - eyePos.z
        )

        // Calculate yaw and pitch - EXACT from original getYawPitch
        val yaw = (atan2(difference.z, difference.x) * 180.0 / Math.PI).toFloat() - 90.0f
        val xz = sqrt(difference.x * difference.x + difference.z * difference.z)
        val pitch = -(atan2(difference.y, xz) * 180.0 / Math.PI).toFloat()

        return floatArrayOf(yaw, pitch)
    }
}