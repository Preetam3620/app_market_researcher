package com.example.app_market_researcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChatGptFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_gpt, container, false)
        val textView = view.findViewById<TextView>(R.id.tvChatGpt)
        textView.text = "ChatGPT Mode Active"
        return view
    }
}
