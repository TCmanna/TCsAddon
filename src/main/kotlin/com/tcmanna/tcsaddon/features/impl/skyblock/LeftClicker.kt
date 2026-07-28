package com.tcmanna.tcsaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.lwjgl.glfw.GLFW
import java.util.Random

object LeftClicker : Module(
    name = "Left Clicker",
    description = "Enable to auto left click"
) {
    private val weaponOnly by BooleanSetting("Weapon Only", true, desc = "Description.")
    private val breakBlocks by BooleanSetting("Break Block", false, desc = "Description.")
    private val jitterLeft by NumberSetting("Jitter left", 0.0, 0.0, 3.0, 0.1, desc = "Description.")
    private val cpsMin by NumberSetting("CPS Min", 9.0, 0.0, 20.0, 0.5, desc = "Description.")
    private val cpsMax by NumberSetting("CPS Max", 13.0, 0.0, 20.0, 0.5, desc = "Description.")

    private var leftDown = false
    private var rand: Random? = null
    private var leftDownTime: Long = 0
    private var leftUpTime: Long = 0
    private var leftk: Long = 0
    private var leftl: Long = 0
    private var leftm = 0.0
    private var leftn = false
    private var breakHeld = false

    init {
        on<RenderEvent.Last> {
            if (mc.player == null) return@on
            if (mc.screen != null && (mc.screen !is InventoryScreen) && (mc.screen !is ContainerScreen))
                return@on

            if (weaponOnly && !isPlayerHoldingWeapon()) return@on

            val isDown = GLFW.glfwGetMouseButton(mc.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS

            if (!isDown && !leftDown) {
                mc.options.keyAttack.isDown = false
            }

            if (isDown || leftDown) {
                leftClickExecute(mc.options.keyAttack)
            }
        }
    }

    override fun onEnable() {
        rand = Random()
        super.onEnable()
    }

    override fun onDisable() {
        this.leftDownTime = 0L
        this.leftUpTime = 0L
        super.onDisable()
    }

    fun leftClickExecute(key: KeyMapping) {
        if (breakBlock()) return

        val player = mc.player ?: return

        // 修复：正确的 yaw / pitch 抖动
        if (jitterLeft > 0.0) {
            val a = jitterLeft * 0.45

            player.yRot += (if (rand!!.nextBoolean()) 1 else -1) * rand!!.nextFloat() * a.toFloat()
            player.xRot += (if (rand!!.nextBoolean()) 1 else -1) * rand!!.nextFloat() * (a.toFloat() * 0.45f)
        }

        if (this.leftUpTime > 0L && this.leftDownTime > 0L) {
            val now = System.currentTimeMillis()

            if (now > this.leftUpTime && leftDown) {
                key.isDown = true
                KeyMapping.click(key.defaultKey)
                this.genLeftTimings()
                leftDown = false
            } else if (now > this.leftDownTime) {
                key.isDown = false
                leftDown = true
            }
        } else {
            this.genLeftTimings()
        }
    }

    fun genLeftTimings() {
        val clickSpeed: Double =
            ranModuleVal(cpsMin, cpsMax, rand!!) + 0.4 * rand!!.nextDouble()
        var delay = Math.round(1000.0 / clickSpeed).toInt().toLong()

        if (System.currentTimeMillis() > this.leftk) {
            if (!this.leftn && rand!!.nextInt(100) >= 85) {
                this.leftn = true
                this.leftm = 1.1 + rand!!.nextDouble() * 0.15
            } else {
                this.leftn = false
            }

            this.leftk = System.currentTimeMillis() + 500L + rand!!.nextInt(1500).toLong()
        }

        if (this.leftn) {
            delay = (delay.toDouble() * this.leftm).toLong()
        }

        if (System.currentTimeMillis() > this.leftl) {
            if (rand!!.nextInt(100) >= 80) {
                delay += 50L + rand!!.nextInt(100).toLong()
            }

            this.leftl = System.currentTimeMillis() + 500L + rand!!.nextInt(1500).toLong()
        }

        this.leftUpTime = System.currentTimeMillis() + delay
        this.leftDownTime = System.currentTimeMillis() + delay / 2L - rand!!.nextInt(10).toLong()
    }

    fun breakBlock(): Boolean {
        val hit = mc.hitResult ?: return false

        if (breakBlocks && hit.type == HitResult.Type.BLOCK) {
            val blockHit = hit as BlockHitResult
            val pos = blockHit.blockPos

            val state = mc.level?.getBlockState(pos) ?: return false

            if (!state.isAir && state.block !is LiquidBlock) {
                if (!breakHeld) {
                    mc.options.keyAttack.isDown = true
                    KeyMapping.click(mc.options.keyAttack.defaultKey)
                    breakHeld = true
                }
                return true
            }

            if (breakHeld) {
                breakHeld = false
            }
        }
        return false
    }

    fun ranModuleVal(min: Double, max: Double, r: Random): Double {
        return if (min == max) min else min + r.nextDouble() * (max - min)
    }

    fun isPlayerHoldingWeapon(): Boolean {
        val currentEquippedItem: ItemStack? = mc.player?.mainHandItem
        if (currentEquippedItem == null) {
            return false
        } else {
            val item: Item? = currentEquippedItem.item
            return item is BowItem || item == Items.WOODEN_SWORD ||
                    item == Items.STONE_SWORD || item == Items.IRON_SWORD ||
                    item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD ||
                    item == Items.NETHERITE_SWORD
        }
    }
}