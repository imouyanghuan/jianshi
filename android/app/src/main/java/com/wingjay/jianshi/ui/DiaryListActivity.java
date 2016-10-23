package com.wingjay.jianshi.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.wingjay.jianshi.R;
import com.wingjay.jianshi.db.model.Diary;
import com.wingjay.jianshi.db.service.DiaryService;
import com.wingjay.jianshi.global.JianShiApplication;
import com.wingjay.jianshi.ui.adapter.DiaryListAdapter;
import com.wingjay.jianshi.ui.base.BaseActivity;
import com.wingjay.jianshi.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class DiaryListActivity extends BaseActivity implements DiaryListAdapter.RecyclerClickListener{

  private final List<Diary> diaryList = new ArrayList<>();
  private DiaryListAdapter adapter;

  @InjectView(R.id.diary_list)
  RecyclerView diaryListView;
  @Inject
  DiaryService diaryService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_diary_list);
    JianShiApplication.getAppComponent().inject(this);
    diaryListView.setHasFixedSize(true);
    diaryListView.setLayoutManager(new LinearLayoutManager(DiaryListActivity.this));

    // get all local diaries
    adapter = new DiaryListAdapter(DiaryListActivity.this, diaryList);
    adapter.setRecyclerClickListener(this);
    diaryListView.setAdapter(adapter);
    diaryService.getDiaryList()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<List<Diary>>() {
          @Override
          public void call(List<Diary> diaries) {
            diaryList.clear();
            diaryList.addAll(diaries);
            adapter.notifyDataSetChanged();
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ConstantUtil.REQUEST_CODE_VIEW_DIARY_FROM_LIST
        && resultCode == RESULT_OK) {
      adapter.notifyItemRangeChanged(0, diaryList.size());
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onItemClick(int position) {
    startActivity(ViewActivity.createIntent(this, diaryList.get(position).getUuid()));
  }

  @Override
  public void onItemLongClick(final int position) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.do_you_want_to_delete_diary)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Diary diary = diaryList.get(position);
            diary.setTime_removed(System.currentTimeMillis());
            diaryService.saveDiary(diary)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                  @Override
                  public void call(Void aVoid) {
                    diaryList.remove(position);
                    adapter.notifyItemRemoved(position);
                  }
                });
          }
        })
        .setNegativeButton(R.string.no, null)
        .create()
        .show();
  }
}
