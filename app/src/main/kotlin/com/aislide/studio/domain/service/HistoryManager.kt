package com.aislide.studio.domain.service

import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Generic undo/redo stack. Snapshots are immutable values pushed after each
 * user-visible change; [undo] and [redo] move the pointer.
 *
 * Capacity is bounded to avoid unbounded memory growth (default 50 entries).
 * Beyond capacity, the oldest entry is dropped (FIFO eviction).
 *
 * Rule of kotlin skill: snapshots must be immutable. Mutating in place then
 * calling push() defeats the equality check that lets us skip no-op pushes.
 */
@Singleton
class HistoryManager<T> @Inject constructor() {

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    private var capacity: Int = 50

    fun configure(capacity: Int) {
        this.capacity = max(1, capacity)
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Push a snapshot. The current value should be pushed BEFORE applying a
     * change, so [undo] returns to the previous state.
     *
     * Thread-safe: synchronized to prevent concurrent corruption of the
     * underlying ArrayDeque (which is NOT thread-safe by default).
     */
    @Synchronized
    fun push(snapshot: T) {
        // Skip if same as last
        val last = undoStack.peekLast()
        if (last == snapshot) return
        undoStack.addLast(snapshot)
        if (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()  // any new action invalidates the redo branch
    }

    /**
     * Pops the last snapshot (returns to the previous state).
     * Pass the CURRENT state so we can push it to redo.
     */
    @Synchronized
    fun undo(current: T): T? {
        if (!canUndo) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        return previous
    }

    /**
     * Re-applies the last undone change. Pass CURRENT so we can push to undo.
     */
    @Synchronized
    fun redo(current: T): T? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        return next
    }

    @Synchronized
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    @Synchronized
    fun snapshotSize(): Int = undoStack.size

    @Synchronized
    fun redoSize(): Int = redoStack.size
}

/**
 * Per-slide history: keeps one [HistoryManager] per slide ID so undo doesn't
 * leak across slides (you can't undo slide 4's edits while on slide 2).
 */
@Singleton
class SlideHistoryManager @Inject constructor() {

    private val perSlide: MutableMap<String, HistoryManager<com.aislide.studio.data.model.Slide>> = mutableMapOf()
    private val lock = Any()

    @Synchronized
    fun get(slideId: String): HistoryManager<com.aislide.studio.data.model.Slide> =
        perSlide.getOrPut(slideId) { HistoryManager() }

    @Synchronized
    fun clearFor(slideId: String) {
        perSlide.remove(slideId)
    }
}
