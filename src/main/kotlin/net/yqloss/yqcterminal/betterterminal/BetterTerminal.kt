package net.yqloss.yqcterminal.betterterminal

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.ItemStack
import net.yqloss.yqcterminal.betterterminal.terminal.*
import net.yqloss.yqcterminal.floor7.TerminalSolver


val MC by lazy { Minecraft.getInstance() }

private val TERMINAL_FACTORIES = listOf(
  TerminalOrder,
  TerminalStart,
  TerminalColor,
  TerminalPanes,
  TerminalRubix,
  TerminalAlign,
)

object BetterTerminal {
    data class QueueData(
        val slotID: Int,
        val button: Int,
        val noWindowIDUpdate: Boolean,
    )

    class TerminalData private constructor(
      val terminal: Terminal,
      val enableQueue: Boolean,
      var windowID: Int,
        // states always have one more element than queue
        // the first element is the actual state in vanilla chest GUI
        // the last element is the prediction being rendered
        // queue[i] operation: states[i] -> states[i + 1]
      private val states: ArrayDeque<List<Int>>,
      private val queue: ArrayDeque<QueueData>,
      var clickDelay: Int,
    ) {
        constructor(
          terminal: Terminal,
          enableQueue: Boolean,
          windowID: Int,
          state: List<Int>,
          clickDelay: Int,
        ) : this(
            terminal,
            enableQueue,
            windowID,
            ArrayDeque(listOf(state)),
            ArrayDeque(),
            clickDelay,
        )

        val state get() = states[states.size - 1]

        fun add(
            state: List<Int>,
            queueData: QueueData,
        ) {
            states += state
            queue += queueData
        }

        fun pop() {
            states.removeFirst()
            queue.removeFirst()
        }

        val isQueueEmpty get() = queue.isEmpty()

        val actualState get() = states[0]

        val nextOperation get() = queue[0]

        val queueSize get() = queue.size
    }

    data class ParsedState(
      val chest: ContainerScreen,
      val inventory: ChestMenu,
      val windowID: Int,
      val title: String,
      val items: List<ItemStack?>,
      val terminal: Terminal,
      val state: List<Int>?,
    )

  infix fun List<Int>?.equalTo(other: List<Int>?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    if (this.size != other.size) return false
    return this.withIndex().all { (index, value) -> value == other[index] || value == 114514 || other[index] == 114514 }
  }

  infix fun List<Int>?.notEqualTo(other: List<Int>?): Boolean = !(this equalTo other)

    val parsedState: ParsedState?
        get() {
            val chest = MC.screen as? ContainerScreen ?: return null
            val title = chest.title.string.trim()
            val terminal = TERMINAL_FACTORIES.firstNotNullOfOrNull { it.createIfMatch(title) } ?: return null
            val inventory = chest.menu
            val items = (0..<inventory.rowCount * 9).map {
              inventory.getSlot(it).item
            }
            return ParsedState(
                chest,
                inventory,
              inventory.containerId,
                title,
                items,
                terminal,
                terminal.parse(items),
            )
        }

    var data: TerminalData? = null

    fun reloadTerminal() {
      if (TerminalSolver.debug)
      Throwable("reload term").printStackTrace()
        data = null
    }

    fun switchToQueue(
      terminal: Terminal,
      windowID: Int,
      state: List<Int>,
    ) {
        this@BetterTerminal.data = TerminalData(
            terminal,
            terminal.enableQueue,
            windowID,
            state,
            -1,
        )
    }

    fun switchToNonQueue(
      terminal: Terminal,
      state: List<Int>,
    ) {
        this@BetterTerminal.data = TerminalData(
            terminal,
            false,
            -1,
            state,
            -1,
        )
    }

    fun switchLoading(terminal: Terminal) {
        this@BetterTerminal.data = TerminalData(
          TerminalLoading(terminal.lines, terminal.beginLine, terminal.chestLines, terminal.title),
            terminal.enableQueue,
            -1,
            listOf(),
            1,
        )
    }

    fun forceNonQueue() {
        val data = data ?: return
        switchToNonQueue(data.terminal, data.state)
    }

    private val randomDelay
        get(): Int {
          // random在哪
          return 0
//            var from = options.clickDelayFrom
//            val until = options.clickDelayUntil
//            if (from < 0) from = 0.0
//            return if (until <= from) from.floorInt else Random.nextDouble(from, until).floorInt
        }


    fun onQueueClick(
      data: TerminalData,
      slotID: Int,
      result: Terminal.Prediction,
    ) {
        val (click, noWindowIDUpdate) =
            when (result.clickType) {
                ClickType.CORRECT ->
                    true.also {
                    } to false

                ClickType.WRONG_WITH_WINDOW_ID_UPDATE ->
                    false.also {
                        if (it) ClickType.WRONG_WITH_WINDOW_ID_UPDATE else ClickType.CANCELED
                    } to false

                ClickType.WRONG_WITHOUT_WINDOW_ID_UPDATE ->
                    false.also {
                        if (it) ClickType.WRONG_WITHOUT_WINDOW_ID_UPDATE else ClickType.CANCELED
                    } to true

                ClickType.FAIL ->
                    false.also {
                        if (it) ClickType.FAIL else ClickType.CANCELED
                    } to true

                else -> false to false
            }

        if (click) {
            if (data.isQueueEmpty) {
                data.clickDelay = randomDelay
            }
            data.add(
                result.state,
                QueueData(slotID, result.button, noWindowIDUpdate),
            )
        }
    }

    fun onNonQueueClick(
      slotID: Int,
      result: Terminal.Prediction,
    ) {
        when (result.clickType) {
            ClickType.WRONG_WITH_WINDOW_ID_UPDATE, ClickType.WRONG_WITHOUT_WINDOW_ID_UPDATE -> {
                return
            }

            ClickType.FAIL -> return

            else -> {}
        }

        clickSlot(slotID, result.button)
    }

    fun onClick(
        slotID: Int,
        button: Int,
    ) {
        val data = data ?: return
        val button = if (data.terminal.enableRightClick) button else 0
        val result = data.terminal.predict(data.state, slotID, button)
        if (result.clickType === ClickType.NONE) return
        if (data.enableQueue) {
            onQueueClick(data, slotID, result)
        } else {
            onNonQueueClick(slotID, result)
        }
    }

    // inplace operation on the parameter
    // will not do anything in non queue mode
    // will change this@BetterTerminal.data only if state mismatch
    private fun ParsedState.tickAndPerformQueuedClicks(data: TerminalData?) {
        val data = data ?: return
        // queue mode
        if (data.clickDelay >= 0) {
            // state mismatch
            // resets this@BetterTerminal.data
            if (state !== null && state notEqualTo data.actualState) {
                  reloadTerminal()
                return
            }

            // time to click
            // does not change data.queue or data.states
            if (data.clickDelay == 0) {
                if (state === null) {
                    // the server has not updated the window
                    // check next tick
                    ++data.clickDelay
                } else if (!data.isQueueEmpty) {
                    val click = data.nextOperation
                  MC.schedule {
                    clickSlot(click.slotID, click.button)
                  }
                    // if the click is wrong in some terminals, window id and state do not change
                    // so we need to skip the click, or it will freeze
                    // + 1 because --data.clickDelay later
                    if (click.noWindowIDUpdate) {
                        data.pop()
                        data.clickDelay = randomDelay + 1
                    }
                    // clickDelay goes to -1 later so it will not be clicked twice
                }
            }

            --data.clickDelay
        }
    }

    // update this@BetterTerminal.data by the parameter and this@ParsedState
    // will not modify the parameter
    private fun ParsedState.updateStoredData(data: TerminalData?) {
        if (data?.terminal == terminal) {
            if (state !== null) {
                if (!data.enableQueue) {
                    // update the state
                    switchToNonQueue(terminal, state)
                } else if (data.isQueueEmpty) {
                    // update the state
                    switchToQueue(terminal, windowID, state)
                } else {
                    val equalID = windowID == data.windowID
                    val equalState = state equalTo data.actualState

                    when {
                      // next click
                        !equalID -> {
                          data.windowID = windowID
                            data.pop()
                            data.clickDelay = randomDelay
                            this@BetterTerminal.data = data
                        }

                        // got checked
                        equalID && !equalState -> switchToNonQueue(terminal, state)

                        // waiting for server to update the window
                        else -> this@BetterTerminal.data = data
                    }
                }
            } else {
                // wait for a complete window
                this@BetterTerminal.data = data
            }
        } else {
            if (state !== null) {
                switchToQueue(terminal, windowID, state)
            } else {
                // has not received a complete window
                switchLoading(terminal)
            }
        }
    }

    private fun updateChest(
        data: TerminalData?,
    ) {

        parsedState?.run {
            updateStoredData(data)
          tickAndPerformQueuedClicks(this@BetterTerminal.data)
        }
    }

  fun update() {
    val data = data
    this@BetterTerminal.data = null
    updateChest(data)
  }

    private fun clickSlot(
        slotID: Int,
        button: Int,
    ) {
        val chest = MC.screen as? ContainerScreen ?: return
      if (TerminalSolver.debug) {
        MC.gui.chat.addMessage(Component.literal("Clicked ${chest.menu.containerId} slot $slotID with button $button"))
        Throwable("click").printStackTrace()
      }
          MC.gameMode?.handleInventoryMouseClick(
            chest.menu.containerId,
            slotID,
            if (button == 1) 1 else 2,
            if (button == 1) net.minecraft.world.inventory.ClickType.PICKUP else net.minecraft.world.inventory.ClickType.CLONE,
            MC.player,
          )
    }

}
