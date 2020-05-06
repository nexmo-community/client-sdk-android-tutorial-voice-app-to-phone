package com.vonage.tutorial.voice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nexmo.client.NexmoAttachmentEvent
import com.nexmo.client.NexmoClient
import com.nexmo.client.NexmoConversation
import com.nexmo.client.NexmoDeletedEvent
import com.nexmo.client.NexmoDeliveredEvent
import com.nexmo.client.NexmoEvent
import com.nexmo.client.NexmoEventsPage
import com.nexmo.client.NexmoMessageEventListener
import com.nexmo.client.NexmoPageOrder
import com.nexmo.client.NexmoSeenEvent
import com.nexmo.client.NexmoTextEvent
import com.nexmo.client.NexmoTypingEvent
import com.nexmo.client.request_listener.NexmoApiError
import com.nexmo.client.request_listener.NexmoRequestListener
import com.vonage.tutorial.voice.extension.toLiveData

class ChatViewModel : ViewModel() {

    private val client = NexmoClient.get()

    private var conversation: NexmoConversation? = null

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage = _errorMessage.toLiveData()

    private val _userName = MutableLiveData<String>()
    val userName = _userName.toLiveData()

    private val _conversationEvents = MutableLiveData<List<NexmoEvent>?>()
    val conversationEvents = _conversationEvents.toLiveData()

    private val messageListener = object : NexmoMessageEventListener {
        override fun onTypingEvent(typingEvent: NexmoTypingEvent) {}

        override fun onAttachmentEvent(attachmentEvent: NexmoAttachmentEvent) {}

        override fun onTextEvent(textEvent: NexmoTextEvent) {
            updateConversation(textEvent)
        }

        override fun onSeenReceipt(seenEvent: NexmoSeenEvent) {}

        override fun onEventDeleted(deletedEvent: NexmoDeletedEvent) {}

        override fun onDeliveredReceipt(deliveredEvent: NexmoDeliveredEvent) {}
    }

    fun onInit() {
        getConversation()
        _userName.postValue(client.user.name)
    }

    private fun getConversation() {
        client.getConversation(Config.CONVERSATION_ID, object : NexmoRequestListener<NexmoConversation> {
            override fun onSuccess(conversation: NexmoConversation?) {
                this@ChatViewModel.conversation = conversation

                conversation?.let {
                    getConversationEvents(it)
                    conversation.addMessageEventListener(messageListener)
                }
            }

            override fun onError(apiError: NexmoApiError) {
                this@ChatViewModel.conversation = null
                _errorMessage.postValue("Error: Unable to load conversation ${apiError.message}")
            }
        })
    }

    private fun getConversationEvents(conversation: NexmoConversation) {
        conversation.getEvents(100, NexmoPageOrder.NexmoMPageOrderAsc, null,
            object : NexmoRequestListener<NexmoEventsPage> {
                override fun onSuccess(nexmoEventsPage: NexmoEventsPage?) {
                    nexmoEventsPage?.pageResponse?.data?.let {
                        _conversationEvents.postValue(it.toList())
                    }
                }

                override fun onError(apiError: NexmoApiError) {
                    _errorMessage.postValue("Error: Unable to load conversation events ${apiError.message}")
                }
            })
    }

    private fun updateConversation(textEvent: NexmoTextEvent) {
        val events = _conversationEvents.value?.toMutableList() ?: mutableListOf()
        events.add(textEvent)
        _conversationEvents.postValue(events)
    }

    fun onSendMessage(message: String) {
        if (conversation == null) {
            _errorMessage.postValue("Error: Conversation does not exist")
            return
        }

        conversation?.sendText(message, object : NexmoRequestListener<Void> {
            override fun onSuccess(p0: Void?) {
            }

            override fun onError(apiError: NexmoApiError) {
            }
        })
    }

    fun onBackPressed() {
        client.logout()
    }

    fun onLogout() {
        client.logout()
    }

    override fun onCleared() {
        conversation?.removeMessageEventListener(messageListener)
    }
}