package com.example.receiptsswipe.custom

interface RecyclerElement {
    fun compare(new: RecyclerElement): Boolean
    fun isItemTheSame(new: RecyclerElement): Boolean = this.javaClass == new.javaClass
}

inline fun <reified T> RecyclerElement.comparator(new: RecyclerElement): Boolean {
    return if (this is T) this == new as T else throw IllegalStateException("Unchecked common recycler types match")
}