
package com.zy.unifiedsharing.utils.thirdplatform;

import android.os.Bundle;

public interface ThirdPlatformHelper
{
    /**
     * [简要描述]:获取平台名称
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 11, 2012]
     * @return 平台名称
     */
    public String getPlatformName();

    /**
     * [简要描述]:分享内容，如果没有授权，会转向授权页面
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 11, 2012]
     * @param asyncListener 异步调用监听
     * @param title 分享内容的标题
     * @param content 分享的内容
     * @param imageUrl 分享的图片url
     */
    public void shareContent(AsyncListener asyncListener , String title , String content , String imageUrl);

    /**
     * [简要描述]:授权
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 11, 2012]
     * @param authListener 异步监听
     */
    public void auth(ThirdPlatformAuthListener authListener);

    /**
     * [简要描述]:清除授权信息
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 11, 2012]
     */
    public void clearAuthInformation();

    /**
     * [简要描述]:是否已被授权
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @date [Sep 11, 2012]
     * @return true为已被授权；false未被授权
     */
    public boolean hasAuthed();

    /**
     *	[简要描述]:获取用户信息，如果没有授权，会转向授权过程
     *	[详细描述]:
     *	@author	[Emerson Zhang]
     *	@email	[emerson.zhang@b5m.com]
     *	@date	[Sep 11, 2012]
     *	@param asyncListener
     */
    public void getUserInfo(AsyncListener asyncListener);
    
    /**
     * [简要描述]:异步获取信息的监听 [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @version [版本号,Sep 11, 2012]
     * @since [comb5mandroid]
     */
    public interface AsyncListener
    {
        /**
         * [简要描述]:异步调用开始 [详细描述]:
         * 
         * @author [Emerson Zhang]
         * @email [emerson.zhang@b5m.com]
         * @date [Sep 11, 2012]
         */
        public void onRequestStart();

        /**
         * [简要描述]:异步调用成功结束
         * [详细描述]:
         * 
         * @author [Emerson]
         * @email [emerson.zhang@b5m.com]
         * @date [Sep 11, 2012]
         * @param response 原始返回信息
         * @param obj 返回对象，需要根据不同平台自行转换
         */
        public void onRequestComplete(String response , Object obj);

        /**
         * [简要描述]:异步调用出错
         * [详细描述]:
         * 
         * @author [Emerson]
         * @email [emerson.zhang@b5m.com]
         * @date [Sep 11, 2012]
         * @param e
         */
        public void onRequestError(Exception e);

        /**
         * [简要描述]:调用过程中出现异常
         * [详细描述]:
         * 
         * @author [Emerson Zhang]
         * @email [emerson.zhang@b5m.com]
         * @date [Sep 11, 2012]
         * @param e
         */
        public void onRequestException(Exception e);

        /**
         * [简要描述]:异步调用被取消
         * [详细描述]:
         * 
         * @author [Emerson Zhang]
         * @email [emerson.zhang@b5m.com]
         * @date [Sep 11, 2012]
         */
        public void onRequestCancel();
    }

    /**
     * [简要描述]:
     * [详细描述]:
     * 
     * @author [Emerson Zhang]
     * @email [emerson.zhang@b5m.com]
     * @version [版本号,Sep 11, 2012]
     * @see [ThirdPlatformAuthListener]
     * @package [com.b5mandroid.utils.thirdplatform]
     * @since [comb5mandroid]
     */
    public interface ThirdPlatformAuthListener
    {
        public void onAuthComplete(Bundle bundle);

        public void onAuthException(Exception e);

        public void onAuthCancel();
    }
}
