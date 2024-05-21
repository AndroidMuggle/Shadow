/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.sample.host;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.shadow.core.common.InstalledApk;
import com.tencent.shadow.core.loader.blocs.CreateResourceBloc;
import com.tencent.shadow.dynamic.host.EnterCallback;
import com.tencent.shadow.sample.constant.Constant;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;


public class PluginLoadActivity extends Activity {

    private ViewGroup mViewGroup;

    private Handler mHandler = new Handler();

    private InstalledApk mInstalledApk;

    public Resources mShadowResources;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        mViewGroup = findViewById(R.id.container);

        startPlugin();
    }


    public void startPlugin() {

        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {
                HostApplication.getApp().loadPluginManager(PluginHelper.getInstance().pluginManagerFile);

                Bundle bundle = new Bundle();
                bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, getIntent().getStringExtra(Constant.KEY_PLUGIN_PART_KEY));
                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, getIntent().getStringExtra(Constant.KEY_ACTIVITY_CLASSNAME));

                HostApplication.getApp().getPluginManager()
                        .enter(PluginLoadActivity.this, Constant.FROM_ID_REFLECTION_PLUGIN, bundle, new EnterCallback() {
                            @Override
                            public void onShowLoadingView(final View view) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mViewGroup.addView(view);
                                    }
                                });
                            }

                            @Override
                            public void onCloseLoadingView() {
//                                finish();
                            }

                            @Override
                            public void onEnterComplete() {

                            }

                            @Override
                            public void onEnterReturn(Object o) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
                                mInstalledApk = (InstalledApk) o;

                                mShadowResources = CreateResourceBloc.INSTANCE.create(mInstalledApk.apkFilePath, PluginLoadActivity.this.getApplicationContext());

                                DexClassLoader classLoader = new DexClassLoader(mInstalledApk.apkFilePath,
                                        mInstalledApk.oDexPath,
                                        mInstalledApk.libraryPath,
                                        getClassLoader());
                                Class<?> clazz = classLoader.loadClass("com.tencent.shadow.sample.plugin.app.lib.usecases.reflection.ReflectionView");
                                Constructor<?> constructor = clazz.getConstructor();
                                Method method = clazz.getMethod("getLayoutId");
                                int layoutId = (int) method.invoke(constructor.newInstance());
                                mViewGroup.post(() -> {
                                    LayoutInflater.from(mViewGroup.getContext()).inflate(layoutId, mViewGroup, true);
                                });
                            }
                        });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HostApplication.getApp().getPluginManager().enter(this, Constant.FROM_ID_CLOSE, null, null);
        mViewGroup.removeAllViews();
    }

    @Override
    public Resources getResources() {
        if (null != mShadowResources) {
            return mShadowResources;
        } else {
            return super.getResources();
        }
    }
}
