package com.example.racketsensefinal

class CircularFifoBuffer<T>(private val capacity: Int) : Iterable<T> {
    private val buffer = mutableListOf<T>()
    private var head = 0

    fun add(element: T) {
        if (buffer.size < capacity) {
            buffer.add(element)
        } else {
            buffer[head] = element
            head = (head + 1) % capacity
        }
    }

    override fun iterator(): Iterator<T> {
        val iterator = mutableListOf<T>()
        iterator.addAll(buffer.subList(head, buffer.size))
        iterator.addAll(buffer.subList(0, head))
        return iterator.iterator()
    }
}
