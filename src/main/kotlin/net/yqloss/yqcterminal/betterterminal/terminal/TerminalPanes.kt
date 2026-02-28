package net.yqloss.yqcterminal.betterterminal.terminal

import net.minecraft.world.item.ItemStack
import net.yqloss.yqcterminal.betterterminal.*

private val SLOTS = rectSlots(2, 1, 6, 3)

data class TerminalPanes(
    val unit: Unit = Unit,
) : Terminal {
    override val enableQueue = true
    override val enableRightClick = false
    override val lines = 4
    override val beginLine = 1
    override val chestLines = 5
    override val title = "Correct all the panes!"

    override fun parse(items: List<ItemStack?>) = mapSlots(items, SLOTS, true) {
        if (it.metadata == 5) 1 else 0
    }

    private fun getSlot(state: Int) = if (state == 0) SlotType.PANES_OFF else SlotType.PANES_ON

    override fun draw(state: List<Int>): List<Terminal.SlotRenderInfo> = buildList {
        repeat(11) { add(SlotType.EMPTY) }
        (0..4).forEach { add(getSlot(state[it])) }
        repeat(4) { add(SlotType.EMPTY) }
        (5..9).forEach { add(getSlot(state[it])) }
        repeat(4) { add(SlotType.EMPTY) }
        (10..14).forEach { add(getSlot(state[it])) }
        repeat(2) { add(SlotType.EMPTY) }
    }

    override fun predict(
        state: List<Int>,
        slotID: Int,
        button: Int,
    ): Terminal.Prediction {
        val pos = when (slotID) {
            in 11..15 -> slotID - 11
            in 20..24 -> slotID - 15
            in 29..33 -> slotID - 19
            else -> return Terminal.Prediction(state, ClickType.NONE, button)
        }
        val wrong = state[pos] != 0
        val result = state.toMutableList()
        result[pos] = if (result[pos] == 0) 1 else 0
        return Terminal.Prediction(
            result,
            if (wrong) ClickType.WRONG_WITH_WINDOW_ID_UPDATE else ClickType.CORRECT,
            button,
        )
    }

    companion object : TerminalFactory<TerminalPanes> {
        override fun createIfMatch(title: String): TerminalPanes? {
            title == "Correct all the panes!" || return null
            return TerminalPanes()
        }
    }
}
