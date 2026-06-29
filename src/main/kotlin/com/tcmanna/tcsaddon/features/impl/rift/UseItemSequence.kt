package com.tcmanna.tcsaddon.features.impl.rift

import com.tcmanna.tcsaddon.utils.Utils.mc
import net.minecraft.world.InteractionHand

class UseItemSequence(private val itemName: String, val delayTicks: Int = 3) : TickTask {
    private enum class State {
        FIND,
        SWITCH,
        WAIT_AFTER_SWITCH,
        FIRST_USE,
        WAIT_SECOND,
        RESTORE,
        DONE
    }

    private var state = State.FIND
    private var timer = 0
    private var slot = -1
    private var previousSlot = -1

    override fun tick(): Boolean {

        val player = mc.player ?: return true

        when (state) {
            State.FIND -> {
                slot = findHotbarItem(itemName)
                if (slot == -1) return true

                previousSlot = player.inventory.selectedSlot
                state = State.SWITCH
            }

            State.SWITCH -> {
                player.inventory.selectedSlot = slot
                timer = 3
                state = State.WAIT_AFTER_SWITCH
            }

            State.WAIT_AFTER_SWITCH -> {
                if (--timer <= 0) state = State.FIRST_USE
            }

            State.FIRST_USE -> {
                useHeldItem()
                timer = 2
                state = State.WAIT_SECOND
            }

            State.WAIT_SECOND -> {
                if (--timer <= 0) state = State.RESTORE
            }

            State.RESTORE -> {
                player.inventory.selectedSlot = previousSlot
                state = State.DONE
            }

            State.DONE -> return true
        }

        return false
    }

    override fun cancel() {
        mc.player?.inventory?.selectedSlot = previousSlot
    }

    private fun findHotbarItem(name: String): Int {
        val player = mc.player ?: return -1

        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue

            val display = stack.hoverName.string
            if (display.contains(name, true)) {
                return i
            }
        }
        return -1
    }

    private fun useHeldItem() {
        val player = mc.player ?: return
        mc.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
    }
}