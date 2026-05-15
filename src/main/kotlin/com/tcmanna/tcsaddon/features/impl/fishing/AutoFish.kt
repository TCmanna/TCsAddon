package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.utils.Utils
import com.tcmanna.tcsaddon.events.AutoFishingEvent
import com.tcmanna.tcsaddon.events.FishingHookedEvent
import com.tcmanna.tcsaddon.events.PlaySoundEvent
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.FishingHook
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

object AutoFish : Module(
    name = "Auto Fish",
    category = Category.custom("Fishing"),
    description = "Auto Fishing."
) {
    private val slugfishMode by BooleanSetting("Slug Mode", false, "")
    private val withSlugPet by BooleanSetting("With Slug Pet", true, "").withDependency { slugfishMode }
    private val reThrowCooldown by NumberSetting("ReThrow Time", 350L, 100L, 1000L, 50L, "")
    private val stateCheck by BooleanSetting("State Check", true, "")
    private val hitCheck by BooleanSetting("Hit Check", true, "")
    private val checkDelaySetting by NumberSetting("Check Delay", 8, 4, 20, 1, "Unit second.")
    private val hud by HUD("Show Catch Time", "Display the time since the last catch.") {
        if ((lastTime <= 60000 && Utils.playerHoldFishRod(mc.player)) || it) {
            if (lastTime > 60000) lastTime = 60000
            val seconds = lastTime / 1000
            val stringWidth = mc.font.width(seconds.toString())

            return@HUD textDim(
                "$seconds/s",
                0,
                0,
                Colors.WHITE,
                false
            )
        }
        0 to 0
    }


    var mill = 0L
    var lastTime = 0L
    var checkDelay = 0
    var isReThrow = false

    //ik its shit execute
    val executor: ScheduledExecutorService = ScheduledThreadPoolExecutor(4)

    init {
        on<WorldEvent.Load> {
            onKeybind()
        }

        on<AutoFishingEvent.Before> {
            mill = System.currentTimeMillis()
        }

        on<FishingHookedEvent> {
            if (type == FishingHookedEvent.Type.Sound && !LocationUtils.isInSkyblock) runAutoFish()
            if (type == FishingHookedEvent.Type.SkyBlock && LocationUtils.isInSkyblock) {
                val delay = if (withSlugPet) 10000 else 20000

                if (!slugfishMode || lastTime > delay) {
                    runAutoFish()
                }
            }
        }

        on<TickEvent.End> {
            lastTime = System.currentTimeMillis() - mill
            if (mc.isPaused || mc.player == null || !stateCheck) return@on
            checkDelay = (checkDelay + 1) % 100000
            val thePlayer = mc.player!!
            if (checkDelay % (checkDelaySetting * 20) == 0 && checkDelay != 0 && Utils.playerHoldFishRod(thePlayer)) {
                if (lastTime < 5000) return@on
                //check has entity
                if (thePlayer.fishing == null) {
                    executor.schedule({
                        if (thePlayer.fishing == null && Utils.playerHoldFishRod(thePlayer)) 
                            Utils.playerUseHeldItem(thePlayer)
                    }, 2000L, TimeUnit.MILLISECONDS)
                }

                //check in liquid
                if (thePlayer.fishing?.inLiquid() == false) {
                    executor.schedule({
                        if (thePlayer.fishing?.inLiquid() == false)
                            Utils.playerUseHeldItem(thePlayer)
                    }, 2000L, TimeUnit.MILLISECONDS)
                }

                //check when hit mob
                if (thePlayer.fishing?.hookedIn != null) {
                    if (thePlayer.fishing!!.hookedIn is ArmorStand) return@on
                    if (KillWorm.enabled) {
                        executor.schedule({
                            if (KillWorm.enabled && thePlayer.fishing?.hookedIn != null && Utils.playerHoldFishRod(thePlayer))
                                KillWorm.killWorms()
                        }, 2000L, TimeUnit.MILLISECONDS)
                    } else {
                        executor.schedule({
                            if (thePlayer.fishing?.hookedIn != null && Utils.playerHoldFishRod(thePlayer))
                                Utils.playerUseHeldItem(thePlayer)
                        }, 2000L, TimeUnit.MILLISECONDS)
                        executor.schedule({
                            if (thePlayer.fishing?.hookedIn != null && Utils.playerHoldFishRod(thePlayer))
                                Utils.playerUseHeldItem(thePlayer)
                        }, 2500L, TimeUnit.MILLISECONDS)
                    }
                }
            }

        }

        on<PlaySoundEvent> {
            if (mc.isPaused || mc.player == null || !hitCheck) return@on
            if (isReThrow) return@on

            if (mc.player?.fishing != null && soundEvent.location.path == "entity.arrow.hit_player") {
                if (KillWorm.enabled && KillWorm.killWithHit) {
                    KillWorm.kill = true
                    KillWorm.killWorms()
                }
                else {
                    isReThrow = true
                    executor.schedule({
                        if (Utils.playerHoldFishRod(mc.player)) Utils.playerUseHeldItem(mc.player)
                    }, 200, TimeUnit.MILLISECONDS)
                    executor.schedule({
                        if (Utils.playerHoldFishRod(mc.player)) {
                            Utils.playerUseHeldItem(mc.player)
                        }
                        isReThrow = false
                    }, 400, TimeUnit.MILLISECONDS)
                }
            }
        }

    }

    override fun onEnable() {
        super.onEnable()
        isReThrow = false
        if (mc.player?.fishing == null && Utils.playerHoldFishRod(mc.player)) Utils.playerUseHeldItem(mc.player)
    }

    fun runAutoFish() {
        val player: LocalPlayer? = mc.player
        if (player != null && Utils.playerHoldFishRod(player)) {
            val fishingBefore = AutoFishingEvent.Before().postAndCatch()
            if (fishingBefore) return

            executor.schedule({
                if (Utils.playerHoldFishRod(player)) Utils.playerUseHeldItem(player)
                executor.schedule({
                        val fishingAfter = AutoFishingEvent.After().postAndCatch()
                        if (fishingAfter) return@schedule
                        if (Utils.playerHoldFishRod(player)) Utils.playerUseHeldItem(player)
                    }, reThrowCooldown, TimeUnit.MILLISECONDS
                )
            }, 100, TimeUnit.MILLISECONDS)
        }
    }

    fun FishingHook.inLiquid() : Boolean {
        val inWater = this.level().getFluidState(this.blockPosition()).`is`(FluidTags.WATER)
        val inLava = this.level().getFluidState(this.blockPosition()).`is`(FluidTags.LAVA)

        return inWater || inLava
    }
}