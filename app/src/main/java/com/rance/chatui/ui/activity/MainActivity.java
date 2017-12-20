package com.rance.chatui.ui.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jude.easyrecyclerview.EasyRecyclerView;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.rance.chatui.R;
import com.rance.chatui.adapter.ChatAdapter;
import com.rance.chatui.adapter.CommonFragmentPagerAdapter;
import com.rance.chatui.enity.DaoMaster;
import com.rance.chatui.enity.DaoSession;
import com.rance.chatui.enity.FullImageInfo;
import com.rance.chatui.enity.MessageInfo;
import com.rance.chatui.enity.MessageInfoDao;
import com.rance.chatui.ui.fragment.ChatEmotionFragment;
import com.rance.chatui.ui.fragment.ChatFunctionFragment;
import com.rance.chatui.util.Constants;
import com.rance.chatui.util.GlobalOnItemClickManagerUtils;
import com.rance.chatui.util.MediaManager;
import com.rance.chatui.widget.EmotionInputDetector;
import com.rance.chatui.widget.NoScrollViewPager;
import com.rance.chatui.widget.StateButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 作者：Rance on 2016/11/29 10:47
 * 邮箱：rance935@163.com
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.chat_list)
    EasyRecyclerView chatList;
    @BindView(R.id.emotion_voice)
    ImageView emotionVoice;
    @BindView(R.id.edit_text)
    EditText editText;
    @BindView(R.id.voice_text)
    TextView voiceText;

    View loadView;
    @BindView(R.id.emotion_button)
    ImageView emotionButton;
    @BindView(R.id.emotion_add)
    ImageView emotionAdd;
    @BindView(R.id.emotion_send)
    StateButton emotionSend;
    @BindView(R.id.viewpager)
    NoScrollViewPager viewpager;
    @BindView(R.id.emotion_layout)
    RelativeLayout emotionLayout;
    @BindView(R.id.rootLayout)
    View rootLayout;

    private EmotionInputDetector mDetector;

    private ChatAdapter chatAdapter;
    //录音相关
    int animationRes = 0;
    int res = 0;
    AnimationDrawable animationDrawable = null;
    private ImageView animView;
    Unbinder unbinder;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        handler = new Handler();
        initWidget();
    }

    private void initWidget() {
        List<Fragment> fragments = new ArrayList<>();
        ChatEmotionFragment chatEmotionFragment = new ChatEmotionFragment();
        fragments.add(chatEmotionFragment);
        ChatFunctionFragment chatFunctionFragment = new ChatFunctionFragment();
        fragments.add(chatFunctionFragment);
        CommonFragmentPagerAdapter adapter = new CommonFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        viewpager.setAdapter(adapter);
        viewpager.setCurrentItem(0);

        mDetector = EmotionInputDetector.with(this)
                .setEmotionView(emotionLayout)
                .setViewPager(viewpager)
                .bindToContent(chatList)
                .bindToEditText(editText)
                .bindToEmotionButton(emotionButton)
                .bindToAddButton(emotionAdd)
                .bindToSendButton(emotionSend)
                .bindToVoiceButton(emotionVoice)
                .bindToVoiceText(voiceText)
                .build();

        GlobalOnItemClickManagerUtils globalOnItemClickListener = GlobalOnItemClickManagerUtils.getInstance(this);
        globalOnItemClickListener.attachToEditText(editText);

        chatAdapter = new ChatAdapter(this);
        chatAdapter.addHeader(new RecyclerArrayAdapter.ItemView() {
            @Override
            public View onCreateView(ViewGroup parent) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.loading_chat_history_layout, parent, false);
                loadView = view.findViewById(R.id.content);
                loadView.setVisibility(View.GONE);
                return view;
            }

            @Override
            public void onBindView(View headerView) {

            }
        });
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatList.setLayoutManager(layoutManager);
        chatList.setAdapter(chatAdapter);
        chatList.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        int position = layoutManager.findFirstCompletelyVisibleItemPosition();
                        if (position == 0 && loadView.getVisibility() == View.GONE) {
                            onLoadingHistory();
                        }
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        chatAdapter.notifyDataSetChanged();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        mDetector.hideEmotionLayout(false);
                        mDetector.hideSoftInput();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        chatAdapter.addItemClickListener(itemClickListener);
        chatAdapter.addAll(loadData());
        scrollToBottom();
        initAutoScroll();
    }

    void onLoadingHistory() {
        if (loadView.isEnabled()) {
            loadView.setVisibility(View.VISIBLE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<MessageInfo> list = getHistory();
                    if (!list.isEmpty()) {
                        chatAdapter.insertAll(list, 0);
                    } else loadView.setEnabled(false);
                    finishLoadHistory();
                }
            }, 1500);
        }
    }

    void finishLoadHistory() {
        loadView.setVisibility(View.GONE);
    }

    void initAutoScroll() {
        rootLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (oldBottom > bottom) scrollToBottom();
            }
        });
    }

    /**
     * item点击事件
     */
    private ChatAdapter.onItemClickListener itemClickListener = new ChatAdapter.onItemClickListener() {
        @Override
        public void onHeaderClick(int position) {
            Toast.makeText(MainActivity.this, "onHeaderClick", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onImageClick(View view, int position) {
            int location[] = new int[2];
            view.getLocationOnScreen(location);
            FullImageInfo fullImageInfo = new FullImageInfo();
            fullImageInfo.setLocationX(location[0]);
            fullImageInfo.setLocationY(location[1]);
            fullImageInfo.setWidth(view.getWidth());
            fullImageInfo.setHeight(view.getHeight());
            fullImageInfo.setImageUrl(chatAdapter.getItem(position).getImageUrl());
            EventBus.getDefault().postSticky(fullImageInfo);
            startActivity(new Intent(MainActivity.this, FullImageActivity.class));
            overridePendingTransition(0, 0);
        }

        /**
         * TODO:如果是WIFI收到信息时自动下载，流量则点击播放时才下载语音文件，多次播放不会额外开销流量。
         * */
        @Override
        public void onVoiceClick(final ImageView imageView, final int position) {
            if (animView != null) {
                animView.setImageResource(res);
                animView = null;
            }
            switch (chatAdapter.getItem(position).getType()) {
                case Constants.CHAT_ITEM_TYPE_LEFT:
                    animationRes = R.drawable.voice_left;
                    res = R.mipmap.icon_voice_left3;
                    break;
                case Constants.CHAT_ITEM_TYPE_RIGHT:
                    animationRes = R.drawable.voice_right;
                    res = R.mipmap.icon_voice_right3;
                    break;
            }
            animView = imageView;
            animView.setImageResource(animationRes);
            animationDrawable = (AnimationDrawable) imageView.getDrawable();
            animationDrawable.start();
            MediaManager.playSound(chatAdapter.getItem(position).getFilepath(), new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    animView.setImageResource(res);
                }
            });
        }
    };
    MessageInfoDao dao;
    DaoMaster.DevOpenHelper dbHelper;
    DaoSession session;

    List<MessageInfo> loadData() {
        dbHelper = new DaoMaster.DevOpenHelper(this, "db");
        DaoMaster master = new DaoMaster(dbHelper.getWritableDatabase());
        session = master.newSession();
        dao = session.getMessageInfoDao();
        List<MessageInfo> list = getHistory();
        if (list.isEmpty()) {
            list = createData();
            dao.insertOrReplaceInTx(list);
        }
        return list;
    }

    @NonNull
    List<MessageInfo> getHistory() {
        List<MessageInfo> list = dao.queryBuilder().orderDesc(MessageInfoDao.Properties.Time).offset(chatAdapter.getCount()).limit(5).list();
        Collections.reverse(list);
        return list;
    }

    /**
     * 构造聊天数据
     */
    private List<MessageInfo> createData() {
        List<MessageInfo> messageInfos = new ArrayList<>();

        MessageInfo msg = new MessageInfo();
        msg.setContent("你好，欢迎使用Rance的聊天界面框架");
        msg.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        msg.setHeader("http://tupian.enterdesk.com/2014/mxy/11/2/1/12.jpg");
        messageInfos.add(msg);

        msg = new MessageInfo();
        msg.setFilepath("http://www.trueme.net/bb_midi/welcome.wav");
        msg.setVoiceTime(3000);
        msg.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        msg.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
        msg.setHeader("http://img.dongqiudi.com/uploads/avatar/2014/10/20/8MCTb0WBFG_thumb_1413805282863.jpg");
        messageInfos.add(msg);

        msg = new MessageInfo();
        msg.setImageUrl("http://img4.imgtn.bdimg.com/it/u=1800788429,176707229&fm=21&gp=0.jpg");
        msg.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        msg.setHeader("http://tupian.enterdesk.com/2014/mxy/11/2/1/12.jpg");
        messageInfos.add(msg);

        msg = new MessageInfo();
        msg.setContent("[微笑][色][色][色]");
        msg.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        msg.setSendState(Constants.CHAT_ITEM_SEND_ERROR);
        msg.setHeader("http://img.dongqiudi.com/uploads/avatar/2014/10/20/8MCTb0WBFG_thumb_1413805282863.jpg");
        messageInfos.add(msg);
        for (int i = 0; i < messageInfos.size(); i++) {
            messageInfos.get(i).setTime(System.currentTimeMillis() + i);
        }
        return messageInfos;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEventBus(final MessageInfo messageInfo) {
        messageInfo.setHeader("http://img.dongqiudi.com/uploads/avatar/2014/10/20/8MCTb0WBFG_thumb_1413805282863.jpg");
        messageInfo.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo.setSendState(Constants.CHAT_ITEM_SENDING);
        messageInfo.setTime(System.currentTimeMillis());
        addMessage(messageInfo);
        scrollToBottom();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                messageInfo.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
                dao.insertOrReplace(messageInfo);
                chatAdapter.notifyDataSetChanged();
            }
        }, 2000);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                MessageInfo message = new MessageInfo();
                message.setTime(System.currentTimeMillis());
                message.setContent("这是模拟消息回复");
                message.setType(Constants.CHAT_ITEM_TYPE_LEFT);
                message.setHeader("http://tupian.enterdesk.com/2014/mxy/11/2/1/12.jpg");
                addMessage(message);
                scrollToBottom();
                dao.insertOrReplace(message);
            }
        }, 3000);
    }

    private void addMessage(MessageInfo message) {
        chatAdapter.add(message);
    }

    private void scrollToBottom() {
        chatList.scrollToPosition(chatAdapter.getCount() + chatAdapter.getHeaderCount() - 1);
    }

    @Override
    public void onBackPressed() {
        if (!mDetector.interceptBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        session.clear();
        dbHelper.close();

        EventBus.getDefault().removeStickyEvent(this);
        EventBus.getDefault().unregister(this);
    }
}
