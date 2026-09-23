package com.eimsound.eimusic.events

interface Event<T>{
    val source: EventSource
    val targets: Set<EventSource>
}

interface EventSource{
    object System : EventSource
    object User: EventSource
    object Player : EventSource
    object Remote : EventSource
}