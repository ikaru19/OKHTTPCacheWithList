package com.ikaru.httpreswithlist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class UserAdapter extends BaseQuickAdapter<User, BaseViewHolder> {

    public UserAdapter(@Nullable List<User> data) {
        super(R.layout.item_user, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, User item) {
            helper.setText(R.id.txtUsername , item.getName())
                    .setText(R.id.txtFullname , item.getRealName());
    }

    public void refill(List<User> data){
        super.mData = data;
        this.notifyDataSetChanged();
    }
}
