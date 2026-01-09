package com.example.nutriority.ui.workout

interface ItemMoveCallbackListener {
    /**
     * Called when an item is being moved from one position to another.
     * Returns true if the move was successful, false otherwise.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onDragDropped()
}
