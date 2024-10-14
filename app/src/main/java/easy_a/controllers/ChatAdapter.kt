package easy_a.controllers

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import easy_a.application.R
import io.noties.markwon.Markwon

data class ChatMessage(
    val message: String,
    val isUserMessage: Boolean,
    val timeSent: String,
    val imageBitmap: Bitmap? // Nullable Bitmap
)

class ChatAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    companion object {
        const val VIEW_TYPE_USER = 0
        const val VIEW_TYPE_BOT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUserMessage) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = messages[position]

        if (holder is UserViewHolder) {
            // Render the user's message with Markdown
            Markwon.create(context).setMarkdown(holder.messageTextView, chatMessage.message)
            holder.timestampTextView.text = chatMessage.timeSent

            // Set the image if available
            if (chatMessage.imageBitmap != null) {
                holder.imageView?.setImageBitmap(chatMessage.imageBitmap)
                holder.imageView?.visibility = View.VISIBLE
            } else {
                holder.imageView?.visibility = View.GONE
            }
        } else if (holder is BotViewHolder) {
            // Render the bot's response with Markdown
            Markwon.create(context).setMarkdown(holder.messageTextView, chatMessage.message)
            holder.timestampTextView.text = chatMessage.timeSent
        }
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    fun addMessage(message: String, isUserMessage: Boolean, timeSent: String, imageBitmap: Bitmap?=null) {
        val chatMessage = ChatMessage(message, isUserMessage, timeSent, imageBitmap)
        messages.add(chatMessage)
        notifyItemInserted(messages.size - 1)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val imageView: ImageView? = itemView.findViewById(R.id.imageView)
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }
}