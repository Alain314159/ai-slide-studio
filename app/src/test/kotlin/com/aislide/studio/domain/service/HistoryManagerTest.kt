package com.aislide.studio.domain.service

import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HistoryManager] — the undo/redo stack.
 *
 * These tests verify:
 * 1. push/undo/redo cycle works correctly
 * 2. Capacity bounds (FIFO eviction)
 * 3. canUndo/canRedo states
 * 4. clear()
 * 5. No-op pushes (equal snapshots) are skipped
 * 6. New push clears the redo stack
 */
class HistoryManagerTest {

    private fun newSlide(title: String) = Slide(
        id = "test",
        index = 0,
        layout = SlideLayout.TITLE_AND_BULLETS,
        title = title
    )

    @Test
    fun `empty history cannot undo or redo`() {
        val history = HistoryManager<Slide>()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `push then undo returns previous state`() {
        val history = HistoryManager<Slide>()
        val state1 = newSlide("original")
        val state2 = newSlide("edited")

        history.push(state1)
        assertTrue(history.canUndo)

        val restored = history.undo(state2)
        assertEquals(state1, restored)
        assertTrue(history.canRedo)
    }

    @Test
    fun `undo with empty stack returns null`() {
        val history = HistoryManager<Slide>()
        assertNull(history.undo(newSlide("irrelevant")))
    }

    @Test
    fun `redo with empty stack returns null`() {
        val history = HistoryManager<Slide>()
        assertNull(history.redo(newSlide("irrelevant")))
    }

    @Test
    fun `undo then redo restores state`() {
        val history = HistoryManager<Slide>()
        val state1 = newSlide("v1")
        val state2 = newSlide("v2")
        val state3 = newSlide("v3")

        history.push(state1)
        history.push(state2)

        // undo → back to state2 (we were at state3)
        val restoredAfterUndo = history.undo(state3)
        assertEquals(state2, restoredAfterUndo)

        // redo → back to state3
        val restoredAfterRedo = history.redo(state2)
        assertEquals(state3, restoredAfterRedo)
    }

    @Test
    fun `push clears redo stack`() {
        val history = HistoryManager<Slide>()
        val s1 = newSlide("1")
        val s2 = newSlide("2")
        val s3 = newSlide("3")

        history.push(s1)
        history.undo(s2)  // now can redo s2

        assertTrue(history.canRedo)

        // user does a NEW action
        history.push(s3)

        assertFalse("Redo should be cleared after new push", history.canRedo)
    }

    @Test
    fun `equal snapshots are not pushed`() {
        val history = HistoryManager<Slide>()
        val s1 = newSlide("same")
        val s1Copy = s1.copy()  // structurally equal

        history.push(s1)
        history.push(s1Copy)  // should be skipped

        // Only one entry → undo returns null after single undo
        history.undo(s1)  // returns s1
        assertFalse(history.canUndo)
    }

    @Test
    fun `capacity bound evicts oldest`() {
        val history = HistoryManager<Slide>().apply { configure(3) }
        val s1 = newSlide("1")
        val s2 = newSlide("2")
        val s3 = newSlide("3")
        val s4 = newSlide("4")
        val current = newSlide("current")

        history.push(s1)
        history.push(s2)
        history.push(s3)
        history.push(s4)  // exceeds capacity → evicts s1

        // First undo should give s4 (most recent push)
        val first = history.undo(current)
        assertEquals(s4, first)

        // Second should give s3
        val second = history.undo(current)
        assertEquals(s3, second)

        // Third should give s2
        val third = history.undo(current)
        assertEquals(s2, third)

        // Fourth should be null (s1 was evicted)
        val fourth = history.undo(current)
        assertNull(fourth)
    }

    @Test
    fun `clear removes all history`() {
        val history = HistoryManager<Slide>()
        history.push(newSlide("1"))
        history.push(newSlide("2"))

        history.clear()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `multiple undos in sequence work`() {
        val history = HistoryManager<Slide>()
        val states = (1..5).map { newSlide(it.toString()) }

        // Push states 1..4, current is 5
        states.dropLast(1).forEach { history.push(it) }

        var current: Slide? = states.last()
        assertEquals(states[3], history.undo(current!!).also { current = it })
        assertEquals(states[2], history.undo(current!!).also { current = it })
        assertEquals(states[1], history.undo(current!!).also { current = it })
        assertEquals(states[0], history.undo(current!!).also { current = it })
        // No more
        assertNull(history.undo(current!!))
    }
}
