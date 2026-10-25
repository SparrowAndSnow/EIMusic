package com.eimsound.eimusic.serialization

import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.PlayControlEvent
import com.eimsound.eimusic.together.TogetherProtocol
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val module = SerializersModule {
    polymorphic(ChannelMessage::class) {
        subclass(ChannelMessage.Text::class)
        subclass(ChannelMessage.System::class)
        subclass(ChannelMessage.PlayControl::class)
    }
    polymorphic(ChannelEvent::class){
        subclass(ChannelEvent.UserJoined::class)
        subclass(ChannelEvent.UserLeft::class)
        subclass(ChannelEvent.NewMessage::class)
        subclass(ChannelEvent.PlayStateChanged::class)
        subclass(ChannelEvent.RoomMetadataChanged::class)
    }
    polymorphic(PlayControlEvent::class){
        subclass(PlayControlEvent.Play::class)
        subclass(PlayControlEvent.Pause::class)
        subclass(PlayControlEvent.Stop::class)
        subclass(PlayControlEvent.Seek::class)
        subclass(PlayControlEvent.Next::class)
        subclass(PlayControlEvent.Previous::class)
        subclass(PlayControlEvent.ChangeTrack::class)
        subclass(PlayControlEvent.ChangeTrackList::class)
        subclass(PlayControlEvent.QueueChange::class)
        subclass(PlayControlEvent.AddToQueue::class)
        subclass(PlayControlEvent.RemoveFromQueue::class)
        subclass(PlayControlEvent.PlayModeChanged::class)
    }
    polymorphic(TogetherProtocol::class){
        subclass(TogetherProtocol.ServerEvent::class)
        subclass(TogetherProtocol.MessageReceived::class)
        subclass(TogetherProtocol.CommandAck::class)
        subclass(TogetherProtocol.ErrorResponse::class)
        subclass(TogetherProtocol.JoinChannel::class)
        subclass(TogetherProtocol.LeaveChannel::class)
        subclass(TogetherProtocol.SendChat::class)
        subclass(TogetherProtocol.PlayControlAction::class)
        subclass(TogetherProtocol.Heartbeat::class)
    }

}