/*
 *  MIT License
 *
 *  Copyright (c) 2019 Webtrekk GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

package com.example.webtrekk.samplejava;

import android.app.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.work.Constraints;

import webtrekk.android.sdk.ExceptionType;
import webtrekk.android.sdk.Logger;
import webtrekk.android.sdk.Webtrekk;
import webtrekk.android.sdk.WebtrekkConfiguration;

public class SampleApplication extends Application {
    final static String WEBSITE_NAME = "Baeldung";

    @Override
    public void onCreate() {
        super.onCreate();

        List<String> ids = new ArrayList<>();
        ids.add("1");
        String stringIds = BuildConfig.TRACK_IDS;
        String domain = BuildConfig.DOMEIN;
        List<String> elements = Arrays.asList(stringIds.split(","));
        WebtrekkConfiguration webtrekkConfiguration = new WebtrekkConfiguration.Builder(elements, domain)
                .setWorkManagerConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true).build())
                .setLogLevel(Logger.Level.BASIC)
                .enableCrashTracking(ExceptionType.ALL)
                .build();

        Webtrekk.getInstance().init(this, webtrekkConfiguration);
    }
}
