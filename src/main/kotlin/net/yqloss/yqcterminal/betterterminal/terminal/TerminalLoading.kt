package net.yqloss.yqcterminal.betterterminal.terminal

import net.minecraft.world.item.ItemStack
import net.yqloss.yqcterminal.betterterminal.ClickType
import net.yqloss.yqcterminal.betterterminal.Terminal

data class TerminalLoading(
    override val lines: Int,
    override val beginLine: Int,
    override val chestLines: Int,
    override val title: String,
) : Terminal {
    override val enableQueue = false
    override val enableRightClick = false

    override fun parse(items: List<ItemStack?>) = listOf<Int>()

    override fun draw(state: List<Int>) = listOf<Terminal.SlotRenderInfo>()

    override fun predict(
        state: List<Int>,
        slotID: Int,
        button: Int,
    ) = Terminal.Prediction(state, ClickType.NONE, button)
}
