package com.aislide.studio.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A simple drag-to-reorder list for Compose.
 *
 * Each item can be long-pressed and dragged to reorder. While dragging:
 * - The dragged item moves with the finger
 * - When it crosses another item's midpoint, items swap positions
 *
 * Usage:
 * ```
 * ReorderableList(
 *     items = myList,
 *     key = { it.id },
 *     onMove = { from, to -> viewModel.moveItem(from, to) }
 * ) { item ->
 *     ItemCard(item)
 * }
 * ```
 *
 * Note: this is a simplified implementation. For production, consider
 * using `org.burnoutcrew.composereorderable` library which handles edge cases
 * (auto-scroll, multi-touch, etc.) more robustly.
 */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = items, key = { key(it) }) { item ->
                val currentIndex = items.indexOf(item)
                val isDragging = currentIndex == draggedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(items.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    draggedIndex = currentIndex
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggedIndex >= 0) {
                                        dragOffset += dragAmount.y
                                        // Check if we should swap with neighbor
                                        val currentInfo = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == draggedIndex }
                                        if (currentInfo != null) {
                                            val draggedCenter = currentInfo.offset + currentInfo.size / 2 + dragOffset
                                            val neighbor = currentInfo.let { info ->
                                                if (dragOffset > 0) {
                                                    // Dragging down: check next item
                                                    listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == info.index + 1 }
                                                } else {
                                                    // Dragging up: check previous item
                                                    listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == info.index - 1 }
                                                }
                                            }
                                            if (neighbor != null) {
                                                val neighborCenter = neighbor.offset + neighbor.size / 2
                                                val shouldSwap = if (dragOffset > 0) {
                                                    draggedCenter > neighborCenter
                                                } else {
                                                    draggedCenter < neighborCenter
                                                }
                                                if (shouldSwap) {
                                                    onMove(draggedIndex, neighbor.index)
                                                    draggedIndex = neighbor.index
                                                    // Adjust offset to keep visual continuity
                                                    dragOffset -= (neighbor.offset - currentInfo.offset)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (isDragging) dragOffset.roundToInt() else 0
                            )
                        }
                ) {
                    itemContent(item, isDragging)
                }
            }
        }
    }
}
