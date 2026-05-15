package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.alert
import com.tcmanna.tcsaddon.utils.Utils
import org.lwjgl.glfw.GLFW

object GoldenFishNotif: Module(
    name = "Golden Fish Notif",
    category = Category.custom("Fishing"),
    description = "title."
) {
    val closeGui by BooleanSetting("Close GUI", true, "Auto close current GUI when spawned.")
    val forceFocus by BooleanSetting("Force Focus", true, "Auto refocus the game window.")

    const val TEXT = "You spot a Golden Fish surface from beneath the lava!"
    var closeTick = 0

    init {
        on<TickEvent.End> {
            val player = mc.player?: return@on

            if (closeTick > 0) {
                closeTick--
                if (closeTick == 0) {
                    if (closeGui) Utils.closeCurrentScreen()
                }
            }
        }

        on<ChatPacketEvent> {
            if (value.trim() == TEXT) {
                if (forceFocus) {
                    val handle = mc.window.handle()
                    GLFW.glfwFocusWindow(handle)
                    GLFW.glfwShowWindow(handle)
                }

                alert("Golden Fish Spawned")
                closeTick = 5
            }
        }
    }
}