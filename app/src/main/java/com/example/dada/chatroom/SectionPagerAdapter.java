package com.example.dada.chatroom;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dada on 2017/11/3.
 */

class SectionPagerAdapter extends FragmentPagerAdapter{
    private List<String> titleList; //标题列表数组
    public SectionPagerAdapter(FragmentManager fm) {
        super(fm);
        titleList = new ArrayList<String>();// 每个页面的Title数据
        titleList.add("加友要求");
        titleList.add("聊天");
        titleList.add("好友列表");
    }

    @Override
    public Fragment getItem(int position) {

        switch (position){
            case 0:
                RequestFragment requestFragment = new RequestFragment();
                return requestFragment;
            case 1:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;
            case 2:
                FriendsFragment friendsFragment = new FriendsFragment();
                return friendsFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;//有三個Tabs
    }

    public  CharSequence getPageTitle(int position){
        /*switch (position){
            case 0:
                return "Requests";
            case 1:
                return "Chats";
            case 2:
                return "Friends";
            default:
                return null;
        }*/
        return titleList.get(position);







    }
}
