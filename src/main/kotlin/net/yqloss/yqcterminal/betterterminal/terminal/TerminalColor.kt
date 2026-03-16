package net.yqloss.yqcterminal.betterterminal.terminal

import com.odtheking.odin.utils.hasGlint
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.yqloss.yqcterminal.betterterminal.*

private val REGEX = Regex("Select all the (.*) items!")

private val SLOTS = rectSlots(1, 1, 7, 4)

data class TerminalColor(
    val meta: String,
) : Terminal {
    override val enableQueue = true
    override val enableRightClick = false
    override val lines = 5
    override val beginLine = 1
    override val chestLines = 6
    override val title = "Select all the $meta items!"

    override fun parse(items: List<ItemStack?>) = mapSlots(items, SLOTS, false) {

        if (it.item?.name?.string?.lowercase()?.startsWith(meta) == true || when (meta) {
          "black" -> it.item == Items.INK_SAC
          "blue" -> it.item == Items.LAPIS_LAZULI
          "brown" -> it.item == Items.COCOA_BEANS
          "white" -> it.item == Items.BONE_MEAL
          else -> false
        }) {
            if (it.hasGlint()) -1 else 1
        } else {
            0
        }
    }

    private fun getSlot(state: Int) = when (state) {
        1 -> SlotType.COLOR_CORRECT
        -1 -> SlotType.COLOR_CLICKED
        else -> SlotType.COLOR_WRONG
    }

    override fun draw(state: List<Int>): List<Terminal.SlotRenderInfo> = buildList {
        repeat(10) { add(SlotType.EMPTY) }
        (0..6).forEach { add(getSlot(state[it])) }
        repeat(2) { add(SlotType.EMPTY) }
        (7..13).forEach { add(getSlot(state[it])) }
        repeat(2) { add(SlotType.EMPTY) }
        (14..20).forEach { add(getSlot(state[it])) }
        repeat(2) { add(SlotType.EMPTY) }
        (21..27).forEach { add(getSlot(state[it])) }
        repeat(1) { add(SlotType.EMPTY) }
    }

    override fun predict(
        state: List<Int>,
        slotID: Int,
        button: Int,
    ): Terminal.Prediction {
        val pos = when (slotID) {
            in 10..16 -> slotID - 10
            in 19..25 -> slotID - 12
            in 28..34 -> slotID - 14
            in 37..43 -> slotID - 16
            else -> return Terminal.Prediction(state, ClickType.NONE, button)
        }
        val result = state.toMutableList()
        return Terminal.Prediction(
            result,
            when (result[pos]) {
                1 -> {
                    result[pos] = -1
                    ClickType.CORRECT
                }

                -1 -> ClickType.WRONG_WITHOUT_WINDOW_ID_UPDATE

                else -> ClickType.FAIL
            },
            button,
        )
    }

  override fun auto(state: List<Int>, lastClick: Int): List<Int> {
    return getOrderedClicks(
      draw(state),
      lastClick
    ) { it.type == SlotType.COLOR_CORRECT }
  }

    companion object : TerminalFactory<TerminalColor> {
        override fun createIfMatch(title: String): TerminalColor? {
            val result = REGEX.matchEntire(title) ?: return null
            return TerminalColor(result.groupValues[1].replace("_", " ").lowercase().let {
              when (it) {
                "silver" -> "light gray"
                else -> it
              }
            })
        }
    }
}
