package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.setTitle
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.random.Random

//code by chatGPT
object RandomMove : Module(
    name = "Random Move",
    category = Category.custom("Fishing"),
    description = "Just random move."
) {
    // ---------------- Sneak ----------------

    private val randomSneak by BooleanSetting("Random Sneak", true, "")
    private val randomSneakMin by NumberSetting("Random Sneak Min", 1, 1, 60, 1, "Unit minute.").withDependency { randomSneak }
    private val randomSneakMax by NumberSetting("Random Sneak Max", 60, 1, 60, 1, "Unit minute.").withDependency { randomSneak }
    private val sneakTimes by NumberSetting("Sneak Times", 1, 1, 5, 1, "").withDependency { randomSneak }

    private var nextSneakTime = 0L
    private var remainingSneaks = 0
    private var sneakTick = 0
    private var isSneaking = false

    private val sneakDuration = 6
    private val sneakInterval = 6

    // ---------------- Jump ----------------

    private val randomJump by BooleanSetting("Random Jump", true, "")
    private val randomJumpMin by NumberSetting("Random Jump Min", 1, 1, 60, 1, "Unit minute.").withDependency { randomJump }
    private val randomJumpMax by NumberSetting("Random Jump Max", 60, 1, 60, 1, "Unit minute.").withDependency { randomJump }

    private var nextJumpTime = 0L
    private var jumpTick = 0
    var isJumping = false

    // ---------------- AFK ----------------

    private val randomAFK by BooleanSetting("Random AFK", true, "")
    private val randomAFKMin by NumberSetting("Random AFK Min", 1, 1, 60, 1, "Unit minute.").withDependency { randomAFK }
    private val randomAFKMax by NumberSetting("Random AFK Max", 60, 1, 60, 1, "Unit minute.").withDependency { randomAFK }
    private val AFKKeepTime by NumberSetting("AFK Keeping Time", 10, 5, 60, 1, "Unit second.").withDependency { randomAFK }

    private var nextAFKTime = 0L
    private var afkEndTime = 0L
    var isAFKing = false

    private var prevSlot = 0
    private var prevYaw = 0f
    private var prevPitch = 0f

    private var targetYaw = 0f
    private var targetPitch = 0f
    private var restoring = false
    private var aimingRandom = false
    private var turnOnAF = false

    init {
        on<TickEvent.Start> {

            val player = mc.player ?: return@on
            val now = System.currentTimeMillis()

            // ================= AFK（优先级最高） =================
            if (randomAFK) {

                if (!isAFKing && now >= nextAFKTime) {
                    nextAFKTime = sendNextTime("AFK", now)
                    if (mc.screen is AbstractContainerScreen<*>) {
                        modMessage("Skip bc screen in GUI", prefix = "§3RandomMove §8»§r ")
                        return@on
                    }

                    if (AutoFish.enabled) {
                        AutoFish.onKeybind()
                        turnOnAF = true
                    }

                    prevSlot = player.inventory.selectedSlot
                    prevYaw = player.yRot
                    prevPitch = player.xRot

                    player.inventory.selectedSlot = Random.nextInt(1, 9)

                    targetPitch = Random.nextInt(70, 90).toFloat()
                    targetYaw = player.yRot + Random.nextInt(-30, 30)

                    isAFKing = true
                    restoring = false
                    aimingRandom = true
                    afkEndTime = now + (AFKKeepTime * 1000)

                    setTitle("Current on Random AFK")
                }

                // AFK中持续平滑
                if (isAFKing && !restoring) {
                    if (aimingRandom) {
                        player.xRot = smoothRotate(player.xRot, targetPitch, 0.15f)
                        player.yRot = smoothRotate(player.yRot, targetYaw, 0.15f)
                        if (abs(player.xRot - targetPitch) < 1f &&
                            abs(player.yRot - targetYaw) < 1f
                        ) {
                            player.xRot = targetPitch
                            player.yRot = targetYaw
                            aimingRandom = false
                        }
                    }

                }

                // 到时间 → 开始恢复
                if (isAFKing && now >= afkEndTime) {
                    restoring = true
                    targetPitch = prevPitch
                    targetYaw = prevYaw
                }

                // 恢复过程
                if (restoring) {
                    player.xRot = smoothRotate(player.xRot, targetPitch, 0.15f)
                    player.yRot = smoothRotate(player.yRot, targetYaw, 0.15f)

                    if (abs(player.xRot - targetPitch) < 1f &&
                        abs(wrapDegrees(player.yRot - targetYaw)) < 1f
                    ) {
                        player.xRot = targetPitch
                        player.yRot = targetYaw
                        player.inventory.selectedSlot = prevSlot
                        isAFKing = false
                        restoring = false
                        if (turnOnAF) {
                            AutoFish.onKeybind()
                            turnOnAF = false
                        }
                    }
                }
            }

            // ================= Sneak =================
            if (randomSneak) {

                if (remainingSneaks == 0 && now >= nextSneakTime) {
                    nextSneakTime = sendNextTime("Sneak", now)
                    if (mc.screen is AbstractContainerScreen<*>) {
                        modMessage("Skip bc screen in GUI", prefix = "§3RandomMove §8»§r ")
                        return@on
                    }
                    remainingSneaks = if (sneakTimes == 1) 1 else Random.nextInt(1, sneakTimes)
                }

                if (remainingSneaks > 0) {
                    sneakTick++

                    if (!isSneaking && sneakTick >= sneakInterval) {
                        mc.options.keyShift.isDown = true
                        isSneaking = true
                        sneakTick = 0
                    }

                    if (isSneaking && sneakTick >= sneakDuration) {
                        mc.options.keyShift.isDown = false
                        isSneaking = false
                        sneakTick = 0
                        remainingSneaks--
                    }
                }
            }

            // ================= Jump =================
            if (randomJump && !isAFKing) {

                if (now >= nextJumpTime) {
                    nextJumpTime = sendNextTime("Jump", now)
                    if (mc.screen is AbstractContainerScreen<*>) {
                        modMessage("Skip bc screen in GUI", prefix = "§3RandomMove §8»§r ")
                        return@on
                    }
                    mc.options.keyJump.isDown = true
                    jumpTick = 20
                    isJumping = true
                }

                if (jumpTick > -10) {
                    jumpTick--
                    if (jumpTick == 0) {
                        mc.options.keyJump.isDown = false
                        if (jumpTick == -10) isJumping = false
                    }
                }
            }
        }

        on<LevelEvent.Load> {
            onKeybind()
        }
    }

    override fun onEnable() {
        val now = System.currentTimeMillis()
        if (randomAFK) nextAFKTime = sendNextTime("AFK", now)
        if (randomSneak) nextSneakTime = sendNextTime("Sneak", now)
        if (randomJump) nextJumpTime = sendNextTime("Jump", now)
        super.onEnable()
    }

    private fun smoothRotate(current: Float, target: Float, speed: Float): Float {
        val diff = wrapDegrees(target - current)
        return current + diff * speed
    }

    private fun wrapDegrees(angle: Float): Float {
        var a = angle
        while (a <= -180f) a += 360f
        while (a > 180f) a -= 360f
        return a
    }

    private fun sendNextTime(type: String, now: Long): Long {
        val (min, max) = when (type) {
            "AFK" -> randomAFKMin to randomAFKMax
            "Sneak" -> randomSneakMin to randomSneakMax
            "Jump" -> randomJumpMin to randomJumpMax
            else -> 1 to 1
        }

        val delay = if (max == min) 60000 else
            Random.nextLong(
                min * 60_000L,
                max * 60_000L
            )
        val time = now + delay

        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        val formatted = Instant.ofEpochMilli(time)
            .atZone(ZoneId.systemDefault())
            .format(formatter)

        modMessage("Next $type time set: $formatted", prefix = "§3RandomMove §8»§r ")
        modMessage("Random Time: ${delay / 60_000} min", prefix = "§3RandomMove §8»§r ")

        return time
    }

}