
package com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android;

import android.graphics.Typeface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.xuchao.douhu.flame.master.flame.danmaku.controller.DanmakuFilters;
import com.xuchao.douhu.flame.master.flame.danmaku.controller.DanmakuFilters.IDanmakuFilter;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.AbsDanmakuSync;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.AbsDisplayer;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.AlphaValue;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.BaseDanmaku;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.GlobalFlagValues;
import com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.IDanmakus;

public class DanmakuContext implements Cloneable {

    public static DanmakuContext create() {
        return new DanmakuContext();
    }

    public enum DanmakuConfigTag {
        FT_DANMAKU_VISIBILITY, FB_DANMAKU_VISIBILITY, L2R_DANMAKU_VISIBILITY, R2L_DANMAKU_VISIBILIY, SPECIAL_DANMAKU_VISIBILITY, TYPEFACE, TRANSPARENCY, SCALE_TEXTSIZE, MAXIMUM_NUMS_IN_SCREEN, DANMAKU_STYLE, DANMAKU_BOLD, COLOR_VALUE_WHITE_LIST, USER_ID_BLACK_LIST, USER_HASH_BLACK_LIST, SCROLL_SPEED_FACTOR, BLOCK_GUEST_DANMAKU, DUPLICATE_MERGING_ENABLED, MAXIMUN_LINES, OVERLAPPING_ENABLE, ALIGN_BOTTOM, DANMAKU_MARGIN, DANMAKU_SYNC;

        public boolean isVisibilityRelatedTag() {
            return this.equals(FT_DANMAKU_VISIBILITY) || this.equals(FB_DANMAKU_VISIBILITY)
                    || this.equals(L2R_DANMAKU_VISIBILITY) || this.equals(R2L_DANMAKU_VISIBILIY)
                    || this.equals(SPECIAL_DANMAKU_VISIBILITY) || this.equals(COLOR_VALUE_WHITE_LIST)
                    || this.equals(USER_ID_BLACK_LIST);
        }
    }
    private int mUpdateRate = 16;
    public int getFrameUpdateRate(){
        return mUpdateRate;
    }

    public void setFrameUpateRate(int rate){
        mUpdateRate = rate;
    }
    /**
     * ????????????
     */
    public Typeface mFont = null;

    /**
     * paint alpha:0-255
     */
    public int transparency = AlphaValue.MAX;

    public float scaleTextSize = 1.0f;

    public int margin = 0;

    /**
     * ????????????????????????
     */
    public boolean FTDanmakuVisibility = true;

    public boolean FBDanmakuVisibility = true;

    public boolean L2RDanmakuVisibility = true;

    public boolean R2LDanmakuVisibility = true;

    public boolean SpecialDanmakuVisibility = true;
    
    List<Integer> mFilterTypes = new ArrayList<Integer>();

    /**
     * ?????????????????? -1 ??????????????????????????? 0 ????????? n ??????????????????n?????????
     */
    public int maximumNumsInScreen = -1;

    /**
     * ????????????????????????
     */
    public float scrollSpeedFactor = 1.0f;

    public AbsDanmakuSync danmakuSync;

    List<Integer> mColorValueWhiteList = new ArrayList<Integer>();
    
    List<Integer> mUserIdBlackList = new ArrayList<Integer>(); 
    
    List<String> mUserHashBlackList = new ArrayList<String>();

    private List<WeakReference<ConfigChangedCallback>> mCallbackList;

    private boolean mBlockGuestDanmaku = false;

    private boolean mDuplicateMergingEnable = false;

    private boolean mIsAlignBottom = false;

    private BaseCacheStuffer mCacheStuffer;

    private boolean mIsMaxLinesLimited;

    private boolean mIsPreventOverlappingEnabled;

    public AbsDisplayer mDisplayer = new AndroidDisplayer();

    public GlobalFlagValues mGlobalFlagValues = new GlobalFlagValues();

    public DanmakuFilters mDanmakuFilters = new DanmakuFilters();

    public DanmakuFactory mDanmakuFactory = DanmakuFactory.create();

    public com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android.CachingPolicy cachingPolicy = com.xuchao.douhu.flame.master.flame.danmaku.danmaku.model.android.CachingPolicy.POLICY_DEFAULT;

    private IDanmakus.BaseComparator mBaseComparator;

    public IDanmakus.BaseComparator getBaseComparator() {
        return mBaseComparator;
    }

    public void setBaseComparator(IDanmakus.BaseComparator baseComparator) {
        this.mBaseComparator = baseComparator;
    }

    public AbsDisplayer getDisplayer() {
        return mDisplayer;
    }

    /**
     * 0 ?????? Choreographer??????DrawHandler???????????? <br />
     * 1 "DFM Update"?????????????????? <br />
     * 2 DrawHandler?????????????????????
     *
     * Note: ?????????{@link android.os.Build.VERSION_CODES#JELLY_BEAN}??????, 0????????????2????????????
     */
    public byte updateMethod = 0;

    /**
     * set typeface
     * 
     * @param font
     */
    public DanmakuContext setTypeface(Typeface font) {
        if (mFont != font) {
            mFont = font;
            mDisplayer.clearTextHeightCache();
            mDisplayer.setTypeFace(font);
            notifyConfigureChanged(DanmakuConfigTag.TYPEFACE);
        }
        return this;
    }

    public DanmakuContext setDanmakuTransparency(float p) {
        int newTransparency = (int) (p * AlphaValue.MAX);
        if (newTransparency != transparency) {
            transparency = newTransparency;
            mDisplayer.setTransparency(newTransparency);
            notifyConfigureChanged(DanmakuConfigTag.TRANSPARENCY, p);
        }
        return this;
    }

    public DanmakuContext setScaleTextSize(float p) {
        if (scaleTextSize != p) {
            scaleTextSize = p;
            mDisplayer.clearTextHeightCache();
            mDisplayer.setScaleTextSizeFactor(p);
            mGlobalFlagValues.updateMeasureFlag();
            mGlobalFlagValues.updateVisibleFlag();
            notifyConfigureChanged(DanmakuConfigTag.SCALE_TEXTSIZE, p);
        }
        return this;
    }

    public DanmakuContext setDanmakuMargin(int m) {
        if (margin != m) {
            margin = m;
            mDisplayer.setMargin(m);
            mGlobalFlagValues.updateFilterFlag();
            mGlobalFlagValues.updateVisibleFlag();
            notifyConfigureChanged(DanmakuConfigTag.DANMAKU_MARGIN, m);
        }
        return this;
    }

    public DanmakuContext setMarginTop(int m) {
        mDisplayer.setAllMarginTop(m);
        return this;
    }

    /**
     * @return ????????????????????????
     */
    public boolean getFTDanmakuVisibility() {
        return FTDanmakuVisibility;
    }

    /**
     * ??????????????????????????????
     * 
     * @param visible
     */
    public DanmakuContext setFTDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_FIX_TOP);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        mGlobalFlagValues.updateFilterFlag();
        if (FTDanmakuVisibility != visible) {
            FTDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.FT_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    private <T> void setFilterData(String tag, T data) {
        setFilterData(tag, data, true);
    }

    private <T> void setFilterData(String tag, T data, boolean primary) {
        @SuppressWarnings("unchecked")
        IDanmakuFilter<T> filter = (IDanmakuFilter<T>) mDanmakuFilters.get(tag, primary);
        filter.setData(data);
    }

    private void setDanmakuVisible(boolean visible, int type) {
        if (visible) {
            mFilterTypes.remove(Integer.valueOf(type));
        } else if (!mFilterTypes.contains(Integer.valueOf(type))) {
            mFilterTypes.add(type);
        }
    }

    /**
     * @return ????????????????????????
     */
    public boolean getFBDanmakuVisibility() {
        return FBDanmakuVisibility;
    }

    /**
     * ??????????????????????????????
     * 
     * @param visible
     */
    public DanmakuContext setFBDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_FIX_BOTTOM);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        mGlobalFlagValues.updateFilterFlag();
        if (FBDanmakuVisibility != visible) {
            FBDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.FB_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * @return ??????????????????????????????
     */
    public boolean getL2RDanmakuVisibility() {
        return L2RDanmakuVisibility;
    }

    /**
     * ????????????????????????????????????
     * 
     * @param visible
     */
    public DanmakuContext setL2RDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SCROLL_LR);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        mGlobalFlagValues.updateFilterFlag();
        if(L2RDanmakuVisibility != visible){
            L2RDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.L2R_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * @return ??????????????????????????????
     */
    public boolean getR2LDanmakuVisibility() {
        return R2LDanmakuVisibility;
    }

    /**
     * ????????????????????????????????????
     * 
     * @param visible
     */
    public DanmakuContext setR2LDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SCROLL_RL);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        mGlobalFlagValues.updateFilterFlag();
        if (R2LDanmakuVisibility != visible) {
            R2LDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.R2L_DANMAKU_VISIBILIY, visible);
        }
        return this;
    }

    /**
     * @return ????????????????????????
     */
    public boolean getSpecialDanmakuVisibility() {
        return SpecialDanmakuVisibility;
    }

    /**
     * ??????????????????????????????
     * 
     * @param visible
     */
    public DanmakuContext setSpecialDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SPECIAL);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        mGlobalFlagValues.updateFilterFlag();
        if (SpecialDanmakuVisibility != visible) {
            SpecialDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.SPECIAL_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * ???????????????????????? -1?????? 0?????????
     * 
     * @param maxSize
     * @return
     */
    public DanmakuContext setMaximumVisibleSizeInScreen(int maxSize) {
        maximumNumsInScreen = maxSize;
        // ?????????
        if (maxSize == 0) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER);
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_ELAPSED_TIME_FILTER);
            notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
            return this;
        }
        // ????????????
        if (maxSize == -1) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER);
            mDanmakuFilters.registerFilter(DanmakuFilters.TAG_ELAPSED_TIME_FILTER);
            notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
            return this;
        }
        setFilterData(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER, maxSize);
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
        return this;
    }

    /**
     * ??????????????????
     * 
     * @param style DANMAKU_STYLE_NONE DANMAKU_STYLE_SHADOW or
     *            DANMAKU_STYLE_STROKEN or DANMAKU_STYLE_PROJECTION
     * @param values
     *        DANMAKU_STYLE_SHADOW ??????????????????values??????????????????
     *        DANMAKU_STYLE_STROKEN ??????????????????values??????????????????
     *        DANMAKU_STYLE_PROJECTION
     *            ??????????????????values??????offsetX, offsetY, alpha
     *                offsetX/offsetY: x/y ?????????????????????
     *                alpha: ??????????????? [0...255]
     * @return
     */
    public DanmakuContext setDanmakuStyle(int style, float... values) {
        mDisplayer.setDanmakuStyle(style, values);
        notifyConfigureChanged(DanmakuConfigTag.DANMAKU_STYLE, style, values);
        return this;
    }

    /**
     * ????????????????????????,?????????????????????
     * 
     * @param bold
     * @return
     */
    public DanmakuContext setDanmakuBold(boolean bold) {
        mDisplayer.setFakeBoldText(bold);
        notifyConfigureChanged(DanmakuConfigTag.DANMAKU_BOLD, bold);
        return this;
    }
    
    /**
     * ?????????????????????????????????
     * @param colors
     * @return
     */
    public DanmakuContext setColorValueWhiteList(Integer... colors) {
        mColorValueWhiteList.clear();
        if (colors == null || colors.length == 0) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_TEXT_COLOR_DANMAKU_FILTER);
        } else {
            Collections.addAll(mColorValueWhiteList, colors);
            setFilterData(DanmakuFilters.TAG_TEXT_COLOR_DANMAKU_FILTER, mColorValueWhiteList);
        }
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.COLOR_VALUE_WHITE_LIST, mColorValueWhiteList);
        return this;
    }
    
    public List<Integer> getColorValueWhiteList(){
        return mColorValueWhiteList;
    }
    
    /**
     * ????????????????????????hash
     * @param hashes 
     * @return
     */
    public DanmakuContext setUserHashBlackList(String... hashes) {
        mUserHashBlackList.clear();
        if (hashes == null || hashes.length == 0) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_USER_HASH_FILTER);
        } else {
            Collections.addAll(mUserHashBlackList, hashes);
            setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        }
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    public DanmakuContext removeUserHashBlackList(String... hashes){
        if(hashes == null || hashes.length == 0) {
            return this;
        }
        for (String hash : hashes) {
            mUserHashBlackList.remove(hash);
        }
        setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    /**
     * ??????????????????
     * @param hashes
     * @return
     */
    public DanmakuContext addUserHashBlackList(String... hashes){
        if(hashes == null || hashes.length == 0) {
            return this;
        }
        Collections.addAll(mUserHashBlackList, hashes);
        setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    public List<String> getUserHashBlackList(){
        return mUserHashBlackList;
    }
    
    
    /**
     * ????????????????????????id , 0 ??????????????????
     * @param ids 
     * @return
     */
    public DanmakuContext setUserIdBlackList(Integer... ids) {
        mUserIdBlackList.clear();
        if (ids == null || ids.length == 0) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_USER_ID_FILTER);
        } else {
            Collections.addAll(mUserIdBlackList, ids);
            setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        }
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    public DanmakuContext removeUserIdBlackList(Integer... ids){
        if(ids == null || ids.length == 0) {
            return this;
        }
        for (Integer id : ids) {
            mUserIdBlackList.remove(id);
        }
        setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    /**
     * ??????????????????
     * @param ids
     * @return
     */
    public DanmakuContext addUserIdBlackList(Integer... ids){
        if(ids == null || ids.length == 0) {
            return this;
        }
        Collections.addAll(mUserIdBlackList, ids);
        setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    public List<Integer> getUserIdBlackList(){
        return mUserIdBlackList;
    }
    
    /**
     * ??????????????????????????????
     * @param block true?????????false?????????
     * @return
     */
    public DanmakuContext blockGuestDanmaku(boolean block) {
        if (mBlockGuestDanmaku != block) {
            mBlockGuestDanmaku = block;
            if (block) {
                setFilterData(DanmakuFilters.TAG_GUEST_FILTER, block);
            } else {
                mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_GUEST_FILTER);
            }
            mGlobalFlagValues.updateFilterFlag();
            notifyConfigureChanged(DanmakuConfigTag.BLOCK_GUEST_DANMAKU, block);
        }
        return this;
    }
    
    /**
     * ??????????????????????????????,????????????????????????
     * @param p
     * @return
     */
    public DanmakuContext setScrollSpeedFactor(float p){
        if (scrollSpeedFactor != p) {
            scrollSpeedFactor = p;
            mDanmakuFactory.updateDurationFactor(p);
            mGlobalFlagValues.updateMeasureFlag();
            mGlobalFlagValues.updateVisibleFlag();
            notifyConfigureChanged(DanmakuConfigTag.SCROLL_SPEED_FACTOR, p);
        }
        return this;
    }
    
    /**
     * ????????????????????????????????????
     * @param enable
     * @return
     */
    public DanmakuContext setDuplicateMergingEnabled(boolean enable) {
        if (mDuplicateMergingEnable != enable) {
            mDuplicateMergingEnable = enable;
            mGlobalFlagValues.updateFilterFlag();
            notifyConfigureChanged(DanmakuConfigTag.DUPLICATE_MERGING_ENABLED, enable);
        }
        return this;
    }

    public boolean isDuplicateMergingEnabled() {
        return mDuplicateMergingEnable;
    }

    public DanmakuContext alignBottom(boolean enable) {
        if (mIsAlignBottom != enable) {
            mIsAlignBottom = enable;
            notifyConfigureChanged(DanmakuConfigTag.ALIGN_BOTTOM, enable);
            mGlobalFlagValues.updateVisibleFlag();
        }
        return this;
    }

    public boolean isAlignBottom() {
        return mIsAlignBottom;
    }

    /**
     * ????????????????????????
     * @param pairs map<K,V> ??????null??????????????????
     * K = (BaseDanmaku.TYPE_SCROLL_RL|BaseDanmaku.TYPE_SCROLL_LR|BaseDanmaku.TYPE_FIX_TOP|BaseDanmaku.TYPE_FIX_BOTTOM)
     * V = ????????????
     * @return
     */
    public DanmakuContext setMaximumLines(Map<Integer, Integer> pairs) {
        mIsMaxLinesLimited = (pairs != null);
        if (pairs == null) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_MAXIMUN_LINES_FILTER, false);
        } else {
            setFilterData(DanmakuFilters.TAG_MAXIMUN_LINES_FILTER, pairs, false);
        }
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.MAXIMUN_LINES, pairs);
        return this;
    }

    @Deprecated
    public DanmakuContext setOverlapping(Map<Integer, Boolean> pairs) {
        return preventOverlapping(pairs);
    }

    /**
     * ?????????????????????
     * @param pairs map<K,V> ??????null??????????????????,?????????????????????
     * K = (BaseDanmaku.TYPE_SCROLL_RL|BaseDanmaku.TYPE_SCROLL_LR|BaseDanmaku.TYPE_FIX_TOP|BaseDanmaku.TYPE_FIX_BOTTOM)
     * V = true|false ????????????
     * @return
     */
    public DanmakuContext preventOverlapping(Map<Integer, Boolean> pairs) {
        mIsPreventOverlappingEnabled = (pairs != null);
        if (pairs == null) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_OVERLAPPING_FILTER, false);
        } else {
            setFilterData(DanmakuFilters.TAG_OVERLAPPING_FILTER, pairs, false);
        }
        mGlobalFlagValues.updateFilterFlag();
        notifyConfigureChanged(DanmakuConfigTag.OVERLAPPING_ENABLE, pairs);
        return this;
    }

    public boolean isMaxLinesLimited() {
        return mIsMaxLinesLimited;
    }

    public boolean isPreventOverlappingEnabled() {
        return mIsPreventOverlappingEnabled;
    }

    /**
     * ??????????????????????????????????????????{@link SimpleTextCacheStuffer}????????????????????????, ?????????????????????????????????{@link SpannedCacheStuffer}
     * ???????????????????????????????????????{@link SimpleTextCacheStuffer}|{@link SpannedCacheStuffer}
     * @param cacheStuffer
     * @param cacheStufferAdapter
     */
    public DanmakuContext setCacheStuffer(BaseCacheStuffer cacheStuffer, BaseCacheStuffer.Proxy cacheStufferAdapter) {
        this.mCacheStuffer = cacheStuffer;
        if (this.mCacheStuffer != null) {
            this.mCacheStuffer.setProxy(cacheStufferAdapter);
            mDisplayer.setCacheStuffer(this.mCacheStuffer);
        }
        return this;
    }

    public DanmakuContext setDanmakuSync(AbsDanmakuSync danmakuSync) {
        this.danmakuSync = danmakuSync;
        return this;
    }

    public DanmakuContext setCachingPolicy(CachingPolicy cachingPolicy) {
        this.cachingPolicy = cachingPolicy;
        return this;
    }
    
    public interface ConfigChangedCallback {
        public boolean onDanmakuConfigChanged(DanmakuContext config, DanmakuConfigTag tag,
                Object... value);
    }

    public void registerConfigChangedCallback(ConfigChangedCallback listener) {
        if (listener == null || mCallbackList == null) {
            mCallbackList = Collections.synchronizedList(new ArrayList<WeakReference<ConfigChangedCallback>>());
        }
        for (WeakReference<ConfigChangedCallback> configReferer : mCallbackList) {
            if (listener.equals(configReferer.get())) {
                return;
            }
        }
        mCallbackList.add(new WeakReference<ConfigChangedCallback>(listener));
    }

    public void unregisterConfigChangedCallback(ConfigChangedCallback listener) {
        if (listener == null || mCallbackList == null)
            return;
        for (WeakReference<ConfigChangedCallback> configReferer : mCallbackList) {
            if (listener.equals(configReferer.get())) {
                mCallbackList.remove(listener);
                return;
            }
        }
    }

    public void unregisterAllConfigChangedCallbacks() {
        if (mCallbackList != null) {
            mCallbackList.clear();
            mCallbackList = null;
        }
    }

    private void notifyConfigureChanged(DanmakuConfigTag tag, Object... values) {
        if (mCallbackList != null) {
            for (WeakReference<ConfigChangedCallback> configReferer : mCallbackList) {
                ConfigChangedCallback cb = configReferer.get();
                if (cb != null) {
                    cb.onDanmakuConfigChanged(this, tag, values);
                }
            }
        }
    }

    public DanmakuContext registerFilter(DanmakuFilters.BaseDanmakuFilter filter) {
        mDanmakuFilters.registerFilter(filter);
        mGlobalFlagValues.updateFilterFlag();
        return this;
    }

    public DanmakuContext unregisterFilter(DanmakuFilters.BaseDanmakuFilter filter) {
        mDanmakuFilters.unregisterFilter(filter);
        mGlobalFlagValues.updateFilterFlag();
        return this;
    }

    public DanmakuContext resetContext() {
        mDisplayer = new AndroidDisplayer();
        mGlobalFlagValues = new GlobalFlagValues();
//        mDanmakuFilters = new DanmakuFilters();
        mDanmakuFilters.clear();
        mDanmakuFactory = DanmakuFactory.create();
        return this;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
