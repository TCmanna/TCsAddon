package com.tcmanna.tcsaddon.features.impl.fishing

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.modMessage
import com.tcmanna.tcsaddon.events.MouseEvent
import com.tcmanna.tcsaddon.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.random.Random

object StaffActionController {
    init {
        on<TickEvent.Start> {
            if (mc.player == null) return@on
            if (running) {
                tick()
            }
        }

        on<MouseEvent.Move> {
            if (running) {
                stop()
                modMessage("Player control transferred")
            }
        }
    }

    

    // =========================
    // Step / Sequence
    // =========================

    data class Step(
        val duration: Int,
        val onTick: ((Int) -> Unit)? = null,
        val onStart: () -> Unit = {}
    ) {
        private var tickIndex = 0

        fun tick() {
            onTick?.invoke(tickIndex)
            tickIndex++
        }

        fun reset() {
            tickIndex = 0
        }
    }

    class Sequence(
        private val steps: List<Step>
    ) {
        private var stepIndex = 0
        private var tick = 0
        private var started = false

        fun reset() {
            stepIndex = 0
            tick = 0
            started = false
            steps.forEach { it.reset() } // 👈 关键
        }

        fun isFinished(): Boolean = stepIndex >= steps.size

        fun onTick() {
            if (isFinished()) return

            val step = steps[stepIndex]

            if (!started) {
                step.onStart()
                started = true
            }

            step.tick()
            tick++

            if (tick >= step.duration) {
                stepIndex++
                tick = 0
                started = false
            }
        }
    }

    // =========================
    // State
    // =========================

    private var currentSequence: Sequence? = null
    private var running = false

    fun isRunning(): Boolean = running

    fun tick() {
        if (!running) return

        currentSequence?.onTick()

        if (currentSequence?.isFinished() == true) {
            running = false
        }
    }

    fun start(sequence: Sequence) {
        currentSequence = sequence
        currentSequence?.reset()
        running = true
    }

    fun stop() {
        if (currentSequence == null) return
        currentSequence = null
        running = false
        
        mc.options.keyAttack.isDown = false
        mc.options.keyLeft.isDown = false
        mc.options.keyRight.isDown = false
        mc.options.keyUp.isDown = false
        mc.options.keyDown.isDown = false
        mc.options.keyJump.isDown = false
        mc.options.keyShift.isDown = false
    }

    // =========================
    // Actions
    // =========================

    fun randomSequence(): Sequence {
        return listOf(
            seqStay(),
            seqMove(),
            seqBackwards()
        ).random()
    }

    fun seqStay() = Sequence(listOf(
        Step(20),

        Step(20,
            onStart = {
                if (mc.screen is AbstractContainerScreen<*>) {
                    mc.player?.closeContainer()
                }
            },
            onTick = { i ->
                if (i == 2) {
                    mc.options.keyJump.isDown = true
                }
                if (i >= 4) {
                    mc.player?.xRot = mc.player!!.xRot + (-10f - mc.player!!.xRot) / 3f
                }
            }
        ),

        Step(1, onStart = {
            mc.options.keyJump.isDown = false
            mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
        }),

        Step(4),

        Step(1, onStart = {
            val key = mc.options.keyAttack as KeyMappingAccessor
            mc.options.keyAttack.isDown = true
            KeyMapping.click(key.boundKey)
            mc.options.keyShift.isDown = true
        }),

        Step(6),

        Step(1, onStart = {
            mc.options.keyAttack.isDown = false
        }),

        Step(4),

        Step(1, onStart = {
            val key = mc.options.keyAttack as KeyMappingAccessor
            mc.options.keyAttack.isDown = true
            KeyMapping.click(key.boundKey)
        }),

        Step(6),

        Step(1, onStart = {
            mc.options.keyAttack.isDown = false
        }),

        Step(8,
            onStart = {
                mc.options.keyShift.isDown = false
            },
            onTick = {
            mc.player?.yRot = mc.player!!.yRot - 16f
        }),

        Step(20),

        Step(10, onTick = {
            mc.player?.yRot = mc.player!!.yRot - 12f
            mc.player?.xRot = mc.player!!.xRot + 5f
        })
    ))

    fun seqMove() = Sequence(listOf(
        Step(20),
        Step(10,
            onStart = {
                if (mc.screen is AbstractContainerScreen<*>) {
                    mc.player?.closeContainer()
                }
            },
            onTick = { i ->
                mc.player?.yRot = mc.player!!.yRot - 10f
                if (i == 5) {
                    mc.options.keyLeft.isDown = true
                    mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
                }
        }),
        Step(2),
        Step(1, onStart = {
            mc.options.keyLeft.isDown = false
        }),
        Step(3, onStart = {
            mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
            mc.options.keyRight.isDown = true
        }),
        Step(1, onStart = {
            mc.options.keyRight.isDown = false
        }),
        Step(3, onStart = {
            mc.options.keyLeft.isDown = true
        }),
        Step(5, onStart = {
            mc.options.keyLeft.isDown = false
            mc.options.keyJump.isDown = true
        }),
        Step(1, onStart = {
            mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
            mc.options.keyRight.isDown = true
            mc.options.keyJump.isDown = false
        }),
        Step(2, onStart = {
            mc.options.keyRight.isDown = false
        }),
        Step(5, onTick = {
            mc.player?.yRot = mc.player!!.yRot + 25f
            mc.player?.xRot = mc.player!!.xRot - 4f
        })
    ))

    fun seqBackwards() = Sequence(listOf(
        Step(10),
        Step(10, onStart = {
            if (mc.screen is AbstractContainerScreen<*>) {
                mc.player?.closeContainer()
            }
        }),

        Step(10,
            onStart = {
                mc.options.keyDown.isDown = true
                mc.options.keyJump.isDown = true
            },
            onTick = {
                mc.player?.yRot = mc.player!!.yRot + 15f
                mc.player?.xRot = mc.player!!.xRot + (-10f - mc.player!!.xRot) / 10f
            }
        ),

        Step(10, onStart = {
            mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
        }),

        Step(20,
            onStart = {
                mc.options.keyDown.isDown = false
                mc.options.keyJump.isDown = false
            },
            onTick = { i ->
                mc.player?.yRot = mc.player!!.yRot - 4f
                if (i == 10)
                    mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
                if (i == 20)
                    mc.player?.inventory?.selectedSlot = Random.nextInt(1, 8)
            }
        ),
    ))
}