
package com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android;

import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.objectpool.PoolableManager;

public class DrawingCachePoolManager implements PoolableManager<com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android.DrawingCache> {

    @Override
    public com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android.DrawingCache newInstance() {
        return null;
    }

    @Override
    public void onAcquired(com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android.DrawingCache element) {

    }

    @Override
    public void onReleased(DrawingCache element) {

    }

}
