/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weibo.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unifiedsharing.R;
import com.weibo.net.AsyncWeiboRunner.RequestListener;

/**
 * A dialog activity for sharing any text or image message to weibo. Three
 * parameters , accessToken, tokenSecret, consumer_key, are needed, otherwise a
 * WeiboException will be throwed.
 * 
 * ShareActivity should implement an interface, RequestListener which will
 * return the request result.
 * 
 * @author ZhangJie (zhangjie2@staff.sina.com.cn)
 */

public class ShareActivity extends Dialog implements OnClickListener, RequestListener {
    private TextView mTextNum;
    private Button mSend;
    private EditText mEdit;
    private FrameLayout mPiclayout;

    private String mPicPath = "";
    private String mContent = "";
    private String mAccessToken = "";
    private String mTokenSecret = "";

    public static final String EXTRA_WEIBO_CONTENT = "com.weibo.android.content";
    public static final String EXTRA_PIC_URI = "com.weibo.android.pic.uri";
    public static final String EXTRA_ACCESS_TOKEN = "com.weibo.android.accesstoken";
    public static final String EXTRA_TOKEN_SECRET = "com.weibo.android.token.secret";

    public static final int WEIBO_MAX_LENGTH = 140;

	public ShareActivity(Context context, String mPicPath, String mContent,
			String mAccessToken, String mTokenSecret) {
		super(context,R.style.dialog3);
		this.mPicPath = mPicPath;
		this.mContent = mContent;
		this.mAccessToken = mAccessToken;
		this.mTokenSecret = mTokenSecret;
		init();
	}
	
//	@Override
//	public void show() {
//		// TODO Auto-generated method stub
//		super.show();
//	}

	public void init() {
		setContentView(R.layout.share_mblog_view);
		
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		getWindow().setLayout(metrics.widthPixels*9/10, metrics.widthPixels*9/10);
		
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

//        mPicPath = in.getStringExtra(EXTRA_PIC_URI);
//        mContent = in.getStringExtra(EXTRA_WEIBO_CONTENT);
//        mAccessToken = in.getStringExtra(EXTRA_ACCESS_TOKEN);
//        mTokenSecret = in.getStringExtra(EXTRA_TOKEN_SECRET);

        AccessToken accessToken = new AccessToken(mAccessToken, mTokenSecret);
        Weibo weibo = Weibo.getInstance();
        weibo.setAccessToken(accessToken);

        Button close = (Button) this.findViewById(R.id.btnClose);
        close.setOnClickListener(this);
        mSend = (Button) this.findViewById(R.id.btnSend);
        mSend.setOnClickListener(this);
        LinearLayout total = (LinearLayout) this.findViewById(R.id.ll_text_limit_unit);
        total.setOnClickListener(this);
        mTextNum = (TextView) this.findViewById(R.id.tv_text_limit);
        
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        
        ImageView picture = (ImageView) this.findViewById(R.id.ivDelPic);
        picture.setOnClickListener(this);

        mEdit = (EditText) this.findViewById(R.id.etEdit);
        mEdit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String mText = mEdit.getText().toString();
                String mStr;
                int len = mText.length();
                if (len <= WEIBO_MAX_LENGTH) {
                    len = WEIBO_MAX_LENGTH - len;
                    mTextNum.setTextColor(R.color.text_num_gray);
                    if (!mSend.isEnabled())
                        mSend.setEnabled(true);
                } else {
                    len = len - WEIBO_MAX_LENGTH;

                    mTextNum.setTextColor(Color.RED);
                    if (mSend.isEnabled())
                        mSend.setEnabled(false);
                }
                mTextNum.setText(String.valueOf(len));
            }
        });
        mEdit.setText(mContent);
        mPiclayout = (FrameLayout) ShareActivity.this.findViewById(R.id.flPic);
        if (TextUtils.isEmpty(this.mPicPath)) {
            mPiclayout.setVisibility(View.GONE);
        } 
        else {
            mPiclayout.setVisibility(View.VISIBLE);
//            File file = new File(mPicPath);
            ImageView image = (ImageView) this.findViewById(R.id.ivImage);
//            System.out.println(mPicPath);
            new AsyncBitmapLoader().execute(image,mPicPath,image.getHeight());
            mPicPath=AsyncBitmapLoader.urlToFilename(mPicPath);
            
//            ImageView image = (ImageView) this.findViewById(R.id.ivImage);
//            if (file.exists()) {
//                Bitmap pic = BitmapFactory.decodeFile(mPicPath);
//                image.setImageBitmap(pic);
//            } 
//            else {
//            	new AsyncBitmapLoader().execute(image,mPicPath,mPiclayout.getHeight(),true);
////                mPiclayout.setVisibility(View.GONE);
//                mPicPath=AsyncBitmapLoader.urlToFilename(mPicPath);
//            }
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.btnClose) {
        	dismiss();
        }
        else if (viewId == R.id.btnSend) {
            Weibo weibo = Weibo.getInstance();
            try {
                if (!TextUtils.isEmpty((String) (weibo.getAccessToken().getToken()))) {
                    this.mContent = mEdit.getText().toString();
                    File file=new File(mPicPath);
                    if (!TextUtils.isEmpty(mPicPath)&&file.exists()) {
                        upload(weibo, Weibo.getAppKey(), this.mPicPath, this.mContent, "", "");
                    } else {
                        // Just update a text weibo!
                        update(weibo, Weibo.getAppKey(), mContent, "", "");
                    }
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.please_login), Toast.LENGTH_LONG);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (WeiboException e) {
                e.printStackTrace();
            }
        } else if (viewId == R.id.ll_text_limit_unit) {
            Dialog dialog = new AlertDialog.Builder(getContext()).setTitle(R.string.attention)
                    .setMessage(R.string.delete_all)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mEdit.setText("");
                        }
                    }).setNegativeButton(R.string.cancel, null).create();
            dialog.show();
        } else if (viewId == R.id.ivDelPic) {
            Dialog dialog = new AlertDialog.Builder(getContext()).setTitle(R.string.attention)
                    .setMessage(R.string.del_pic)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mPiclayout.setVisibility(View.GONE);
                        }
                    }).setNegativeButton(R.string.cancel, null).create();
            dialog.show();
        }
    }

    private String upload(Weibo weibo, String source, String file, String status, String lon,
            String lat) throws WeiboException {
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", source);
        bundle.add("pic", file);
        bundle.add("status", status);
        bundle.add("access_token", mAccessToken);
        if (!TextUtils.isEmpty(lon)) {
            bundle.add("lon", lon);
        }
        if (!TextUtils.isEmpty(lat)) {
            bundle.add("lat", lat);
        }
        String rlt = "";
        String url = Weibo.SERVER + "statuses/upload.json";
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(weibo);
        weiboRunner.request(getContext(), url, bundle, Utility.HTTPMETHOD_POST, this);

        return rlt;
    }

    private String update(Weibo weibo, String source, String status, String lon, String lat)
            throws MalformedURLException, IOException, WeiboException {
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", source);
        bundle.add("status", status);
        bundle.add("access_token", mAccessToken);
        if (!TextUtils.isEmpty(lon)) {
            bundle.add("lon", lon);
        }
        if (!TextUtils.isEmpty(lat)) {
            bundle.add("lat", lat);
        }
        String rlt = "";
        String url = Weibo.SERVER + "statuses/update.json";
        AsyncWeiboRunner weiboRunner = new AsyncWeiboRunner(weibo);
        weiboRunner.request(getContext(), url, bundle, Utility.HTTPMETHOD_POST, this);
        return rlt;
    }

    @Override
    public void onComplete(String response) {
//        Toast.makeText(getContext(), R.string.send_sucess, Toast.LENGTH_LONG).show();
        B5MToastUtil.getB5MToast(getContext(), R.string.send_sucess, Toast.LENGTH_SHORT, B5MToastUtil.TOAST_TYPE_OK).show();
        dismiss();
    }

    @Override
    public void onIOException(IOException e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(final WeiboException e) {
//        Toast.makeText(getContext(),String.format(getContext().getString(R.string.send_failed) + ":%s", e.getMessage()), Toast.LENGTH_LONG).show();
        B5MToastUtil.getB5MToast(getContext(), String.format(getContext().getString(R.string.send_failed) + ":%s", e.getMessage()), Toast.LENGTH_SHORT, B5MToastUtil.TOAST_TYPE_ERROR).show();
    }
}