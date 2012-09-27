
package com.zy.unifiedsharing.utils.thirdplatform.ui;

import com.b5m.bang5mai.R;
import com.b5mandroid.utils.imageloader.AsyncImageManager;
import com.b5mandroid.utils.imageloader.ImageLoadListener;
import com.b5mandroid.utils.thirdplatform.ThirdPlatformHelper;
import com.b5mandroid.utils.thirdplatform.ThirdPlatformHelper.AsyncListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ShareContentEditDialog extends Dialog
{
    private TextView mTextNum;
    private Button sendButton;
    private Button closeButton;
    private LinearLayout clearButton;
    private EditText mEdit;
    private FrameLayout mPiclayout;
    private ImageView deletePicButton;
    private ImageView shareImageView;

    private ButtonClickListener clickListener;

    private ThirdPlatformHelper share;
    private AsyncListener shareListener;

    private int textMaxLength;
    private String title;
    private String content;
    private String picPath;

    private AsyncImageManager imageLoader;

    public ShareContentEditDialog(Context context, ThirdPlatformHelper share, AsyncListener shareListener, int textMaxLength, String title, String content, String picPath)
    {
        super(context, R.style.dialog3);
        clickListener = new ButtonClickListener();
        this.share = share;
        this.shareListener = shareListener;
        this.textMaxLength = textMaxLength;
        this.title = title;
        this.content = content;
        this.picPath = picPath;
        imageLoader = new AsyncImageManager();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_dialog_view);
        initView();
        bindAllViews();
        setupAllViewStatus();
    }

    private void initView()
    {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        getWindow().setLayout(metrics.widthPixels * 9 / 10, metrics.widthPixels * 9 / 10);
    }

    private void bindAllViews()
    {
        closeButton = (Button) this.findViewById(R.id.btnClose);
        sendButton = (Button) this.findViewById(R.id.btnSend);
        clearButton = (LinearLayout) this.findViewById(R.id.ll_text_limit_unit);
        mTextNum = (TextView) this.findViewById(R.id.tv_text_limit);
        deletePicButton = (ImageView) this.findViewById(R.id.ivDelPic);
        mEdit = (EditText) this.findViewById(R.id.etEdit);

        closeButton.setOnClickListener(clickListener);
        sendButton.setOnClickListener(clickListener);
        clearButton.setOnClickListener(clickListener);
        
        deletePicButton.setOnClickListener(clickListener);
        shareImageView = (ImageView) this.findViewById(R.id.ivImage);
        mPiclayout = (FrameLayout) findViewById(R.id.flPic);

    }

    /**
     *	[简要描述]:为所有视图元素设置正确的显示
     *	[详细描述]:
     *	@author	[lscm]
     *	@email	[emerson.zhang@b5m.com]
     *	@date	[Sep 6, 2012]
     */
    private void setupAllViewStatus()
    {
        mEdit.addTextChangedListener(new TextWatcher()
        {
            public void afterTextChanged(Editable s)
            {
            }

            public void beforeTextChanged(CharSequence s , int start , int count , int after)
            {
            }

            public void onTextChanged(CharSequence s , int start , int before , int count)
            {
                String mText = mEdit.getText().toString();
                int len = mText.length();
                if (len <= textMaxLength)
                {
                    len = textMaxLength - len;
                    mTextNum.setTextColor(R.color.text_num_gray);
                    if (!sendButton.isEnabled())
                        sendButton.setEnabled(true);
                }
                else
                {
                    len = len - textMaxLength;
                    mTextNum.setTextColor(Color.RED);
                    if (sendButton.isEnabled())
                        sendButton.setEnabled(false);
                }
                mTextNum.setText(String.valueOf(len));
            }
        });
        mEdit.setText(content);
        if (TextUtils.isEmpty(picPath))
        {
            mPiclayout.setVisibility(View.GONE);
        }
        else
        {
            mPiclayout.setVisibility(View.VISIBLE);
            // 加载图片
            imageLoader.getImage((new String[] { picPath }), new ImageListener());
        }
        
        ((TextView)findViewById(R.id.share_dialog_title)).setText(share.getPlatformName());
    }

    private class ImageListener implements ImageLoadListener
    {

        @Override
        public void onImageLoad(Bitmap bitmap)
        {
            shareImageView.setImageBitmap(bitmap);
        }

        @Override
        public void onError(Exception e)
        {

        }

    }

    private class ButtonClickListener implements android.view.View.OnClickListener
    {

        @Override
        public void onClick(View v)
        {

            switch (v.getId())
            {
                case R.id.btnClose:
                {
                    dismiss();
                    break;
                }

                case R.id.btnSend:
                {
                    content = mEdit.getText().toString();
                    share.shareContent(shareListener, "", content, picPath);
                    break;
                }

                case R.id.ll_text_limit_unit:
                {
                    Dialog dialog = new AlertDialog.Builder(getContext()).setTitle(R.string.attention).setMessage(R.string.delete_all).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog , int which)
                        {
                            mEdit.setText("");
                        }
                    }).setNegativeButton(R.string.cancel, null).create();
                    dialog.show();
                    break;
                }

                case R.id.ivDelPic:
                {
                    Dialog dialog = new AlertDialog.Builder(getContext()).setTitle(R.string.attention).setMessage(R.string.del_pic).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog , int which)
                        {
                            mPiclayout.setVisibility(View.GONE);
                            picPath = "";
                        }
                    }).setNegativeButton(R.string.cancel, null).create();
                    dialog.show();
                    break;
                }
                default:
                    break;
            }

        }
    }

}
