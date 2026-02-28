package net.yqloss.yqcterminal.betterterminal.terminal

import com.odtheking.odin.utils.hasGlint
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.yqloss.yqcterminal.betterterminal.*

private val REGEX = Regex("What starts with: '(.)'\\?")

private val SLOTS = rectSlots(1, 1, 7, 3)

data class TerminalStart(
    val letter: String,
) : Terminal {
    override val enableQueue = true
    override val enableRightClick = false
    override val lines = 4
    override val beginLine = 1
    override val chestLines = 5
    override val title = "What starts with: '$letter'?"

    override fun parse(items: List<ItemStack?>): List<Int>? {
        return mapSlots(items, SLOTS, false) {
            if (it.hoverName?.string?.uppercase()?.startsWith(letter)==true) {
              when {
                it.item?.components()?.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true -> 114514
                it.hasGlint() -> -1
                else -> 1
              }
            } else {
                0
            }
        }
    }

    private fun getSlot(state: Int): SlotType {
        return when (state) {
            1, 114514 -> SlotType.START_CORRECT
            -1 -> SlotType.START_CLICKED
            else -> SlotType.START_WRONG
        }
    }

    override fun draw(state: List<Int>): List<Terminal.SlotRenderInfo> {
        return buildList {
            repeat(10) { add(SlotType.EMPTY) }
            (0..6).forEach { add(getSlot(state[it])) }
            repeat(2) { add(SlotType.EMPTY) }
            (7..13).forEach { add(getSlot(state[it])) }
            repeat(2) { add(SlotType.EMPTY) }
            (14..20).forEach { add(getSlot(state[it])) }
            repeat(1) { add(SlotType.EMPTY) }
        }
    }

    override fun predict(
        state: List<Int>,
        slotID: Int,
        button: Int,
    ): Terminal.Prediction {
        val pos =
            when (slotID) {
                in 10..16 -> slotID - 10
                in 19..25 -> slotID - 12
                in 28..34 -> slotID - 14
                else -> return Terminal.Prediction(state, ClickType.NONE, button)
            }
        val result = state.toMutableList()
        return Terminal.Prediction(
            result,
            when (result[pos]) {
                1, 114514 -> {
                    result[pos] = -1
                    ClickType.CORRECT
                }

                -1 -> ClickType.WRONG_WITHOUT_WINDOW_ID_UPDATE

                else -> ClickType.FAIL
            },
            button,
        )
    }

    companion object : TerminalFactory<TerminalStart> {
        override fun createIfMatch(title: String): TerminalStart? {
            return REGEX.matchEntire(title)?.let { result ->
                TerminalStart(result.groupValues[1].uppercase())
            }
        }
    }
}
