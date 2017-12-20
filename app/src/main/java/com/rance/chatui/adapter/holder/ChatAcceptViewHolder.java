package com.rance.chatui.adapter.holder;

import android.os.Handler;
import android.view.ViewGroup;

import com.rance.chatui.R;
import com.rance.chatui.adapter.ChatAdapter;

/**
 * 作者：Rance on 2016/11/29 10:47
 * 邮箱：rance935@163.com
 */
public class ChatAcceptViewHolder extends BaseChatViewHolder {
    public ChatAcceptViewHolder(ViewGroup parent, ChatAdapter.onItemClickListener onItemClickListener, Handler handler) {
        super(parent, onItemClickListener, handler, R.layout.item_chat_accept);
    }
}
