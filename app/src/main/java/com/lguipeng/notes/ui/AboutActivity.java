package com.lguipeng.notes.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lguipeng.notes.BuildConfig;
import com.lguipeng.notes.R;
import com.lguipeng.notes.adpater.MaterialSimpleListAdapter;
import com.lguipeng.notes.model.MaterialSimpleListItem;
import com.lguipeng.notes.module.DataModule;
import com.lguipeng.notes.utils.SnackbarUtils;
import com.lguipeng.notes.utils.TimeUtils;
import com.lguipeng.notes.utils.WXUtils;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by lgp on 2015/5/25.
 */
public class AboutActivity extends BaseActivity implements View.OnClickListener{
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.version_text)
    TextView versionTextView;
    @BindView(R.id.blog_btn)
    Button blogButton;
    @BindView(R.id.project_home_btn)
    Button projectHomeButton;
    private int clickCount = 0;
    private long lastClickTime = 0;
    private final static String WEIBO_PACKAGENAME = "com.sina.weibo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVersionText();
        blogButton.setOnClickListener(this);
        projectHomeButton.setOnClickListener(this);
    }

    @Override
    protected int getLayoutView() {
        return R.layout.activity_about;
    }

    @Override
    protected List<Object> getModules() {
        return Arrays.<Object>asList(new DataModule());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.blog_btn:
                startViewAction(BuildConfig.BLOG_URL);
                break;
            case R.id.project_home_btn:
                startViewAction(BuildConfig.PROJECT_URL);
                break;
            default:
                break;
        }
    }

    @Override
    protected void initToolbar(){
        super.initToolbar(toolbar);
        toolbar.setTitle(R.string.about);

    }

    @OnClick(R.id.version_text)
    void versionClick(View view){
        if (clickCount < 3){
            if (TimeUtils.getCurrentTimeInLong() - lastClickTime < 500 || lastClickTime <= 0){
                clickCount ++;
                if (clickCount >= 3){
                    startViewAction(BuildConfig.ABOUT_APP_URL);
                    clickCount = 0;
                    lastClickTime = 0;
                    return;
                }
            }else {
                clickCount = 0;
                lastClickTime = 0;
                return;
            }
            lastClickTime = TimeUtils.getCurrentTimeInLong();
        }
    }

    private void initVersionText(){
        versionTextView.setText("v" + getVersion(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.share:
               showShareDialog();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private String getVersion(Context ctx){
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            return pi.versionName;
        }catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return "1.0.0";
    }

    private void startViewAction(String uriStr){
        try {
            Uri uri = Uri.parse(uriStr);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void share(String packages, Uri uri){
        Intent intent=new Intent(Intent.ACTION_SEND);
        if (uri != null){
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }else {
            intent.setType("text/plain");
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, getString(R.string.download_url), BuildConfig.APP_DOWNLOAD_URL));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!TextUtils.isEmpty(packages))
            intent.setPackage(packages);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private byte[] getLogoBitmapArray(){
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        return WXUtils.bmpToByteArray(bitmap, false);
    }

    private void shareToWeChat(int scene){
        IWXAPI api = WXAPIFactory.createWXAPI(this, BuildConfig.WECHAT_ID, true);
        if (!api.isWXAppInstalled()){
            SnackbarUtils.show(this, R.string.not_install_app);
        }
        api.registerApp(BuildConfig.WECHAT_ID);
        WXWebpageObject object = new WXWebpageObject();
        object.webpageUrl = "http://www.wandoujia.com/apps/com.lguipeng.notes";
        WXMediaMessage msg = new WXMediaMessage(object);
        msg.mediaObject = object;
        msg.thumbData = getLogoBitmapArray();
        msg.title = getString(R.string.app_desc);
        msg.description = getString(R.string.share_text, "", "");
        SendMessageToWX.Req request = new SendMessageToWX.Req();
        request.message = msg;
        request.scene = scene;
        api.sendReq(request);
        api.unregisterApp();
    }

    private void shareToWeChatTimeline(){
        shareToWeChat(SendMessageToWX.Req.WXSceneTimeline);
    }

    private void shareToWeChatSession(){
        shareToWeChat(SendMessageToWX.Req.WXSceneSession);
    }

    private void shareToWeChatFavorite(){
        shareToWeChat(SendMessageToWX.Req.WXSceneFavorite);
    }

    private void shareToWeibo(){
        if (isInstallApplication(WEIBO_PACKAGENAME)){
            share(WEIBO_PACKAGENAME, null);
        }else {
            SnackbarUtils.show(this, R.string.not_install_app);
        }
    }

    private boolean isInstallApplication(String packageName){
        try {
            PackageManager pm = this.getPackageManager();
            pm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void showShareDialog(){
        AlertDialog.Builder builder = generateDialogBuilder();
        builder.setTitle(getString(R.string.share));
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(this);
        String[] array = getResources().getStringArray(R.array.share_dialog_text);
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(array[0])
                .icon(R.drawable.ic_wx_logo)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(array[1])
                .icon(R.drawable.ic_wx_moments)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(array[2])
                .icon(R.drawable.ic_wx_collect)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(array[3])
                .icon(R.drawable.ic_sina_logo)
                .build());
        adapter.add(new MaterialSimpleListItem.Builder(this)
                .content(array[4])
                .icon(R.drawable.ic_share_more)
                .build());
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        shareToWeChatSession();
                        break;
                    case 1:
                        shareToWeChatTimeline();
                        break;
                    case 2:
                        shareToWeChatFavorite();
                        break;
                    case 3:
                        shareToWeibo();
                        break;
                    default:
                        share("", null);
                }
            }
        });
        builder.show();
    }

}
