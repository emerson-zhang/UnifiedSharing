package com.kaixin.connect.param;

import com.kaixin.connect.exception.KaixinAuthError;

import android.os.Bundle;
import android.text.TextUtils;


public abstract class BaseParameter {
	
	public abstract Bundle getParams() throws KaixinAuthError;
	
	public void checkNullParams (String... params) throws KaixinAuthError {
		
		for (String param : params) {
			if (TextUtils.isEmpty(param)) {
				String exceptionMsg = "required parameter shold not be null";
				throw new KaixinAuthError("checkNullParams", exceptionMsg, exceptionMsg);
			}
		}
	}
}
