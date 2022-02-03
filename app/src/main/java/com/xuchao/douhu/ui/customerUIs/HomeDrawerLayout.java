package com.xuchao.douhu.ui.customerUIs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;



import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * HomeDrawerLayout acts as a top-level container for window content that allows for
 * interactive "drawer" views to be pulled out from one or both vertical edges of the window.
 *
 * <p>Drawer positioning and layout is controlled using the <code>android:layout_gravity</code>
 * attribute on child views corresponding to which side of the view you want the drawer
 * to emerge from: left or right (or start/end on platform versions that support layout direction.)
 * Note that you can only have one drawer view for each vertical edge of the window. If your
 * layout configures more than one drawer view per vertical edge of the window, an exception will
 * be thrown at runtime.
 * </p>
 *
 * <p>To use a HomeDrawerLayout, position your primary content view as the first child with
 * width and height of <code>match_parent</code> and no <code>layout_gravity></code>.
 * Add drawers as child views after the main content view and set the <code>layout_gravity</code>
 * appropriately. Drawers commonly use <code>match_parent</code> for height with a fixed width.</p>
 *
 * <p>{@link DrawerListener} can be used to monitor the state and motion of drawer views.
 * Avoid performing expensive operations such as layout during animation as it can cause
 * stuttering; try to perform expensive operations during the {@link #STATE_IDLE} state.
 * {@link SimpleDrawerListener} offers default/no-op implementations of each callback method.</p>
 *
 * <p>As per the <a href="{@docRoot}design/patterns/navigation-drawer.html">Android Design
 * guide</a>, any drawers positioned to the left/start should
 * always contain content for navigating around the application, whereas any drawers
 * positioned to the right/end should always contain actions to take on the current content.
 * This preserves the same navigation left, actions right structure present in the Action Bar
 * and elsewhere.</p>
 *
 * <p>For more information about how to use HomeDrawerLayout, read <a
 * href="{@docRoot}training/implementing-navigation/nav-drawer.html">Creating a Navigation
 * Drawer</a>.</p>
 * <p>
 * <p>
 *     1.修改原生DrawerLayout为垂直方向控件
 *     2.调整上下抽屉为满屏{@link #MIN_DRAWER_MARGIN}
 *     3.调整点击屏幕边缘预显示的抽屉宽度{@link #setEdgeSize(int)}
 * Based on Google open source project HomeDrawerLayout changes
 * Created by INode on 2020/6/11.
 */
public class HomeDrawerLayout extends ViewGroup {
    private static final String TAG = "HomeDrawerLayout";

    private static final int[] THEME_ATTRS = {
            android.R.attr.colorPrimaryDark
    };

    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    /**
     * Indicates that any drawers are in an idle, settled state. No animation is in progress.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * Indicates that a drawer is currently being dragged by the user.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * Indicates that a drawer is in the process of settling to a final position.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED_OPEN,
            LOCK_MODE_UNDEFINED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LockMode {
    }

    /**
     * The drawer is unlocked.
     */
    public static final int LOCK_MODE_UNLOCKED = 0;

    /**
     * The drawer is locked closed. The user may not open it, though
     * the app may open it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;

    /**
     * The drawer is locked open. The user may not close it, though the app
     * may close it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_OPEN = 2;

    /**
     * The drawer's lock state is reset to default.
     */
    public static final int LOCK_MODE_UNDEFINED = 3;

    @IntDef(value = {Gravity.TOP, Gravity.BOTTOM, GravityCompat.START, GravityCompat.END},
            flag = true)
    @Retention(RetentionPolicy.SOURCE)
    private @interface EdgeGravity {
    }


    /**
     * 这里调整是否满屏，也就是抽屉拉出后预留大小
     */
    private static final int MIN_DRAWER_MARGIN = 1; // dp
    private static final int DRAWER_ELEVATION = 10; //dp

    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;

    /**
     * Length of time to delay before peeking the drawer.
     */
    private static final int PEEK_DELAY = 160; // ms

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * Experimental feature.
     */
    private static final boolean ALLOW_EDGE_LOCK = false;

    private static final boolean CHILDREN_DISALLOW_INTERCEPT = true;

    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;

    static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };

    /**
     * Whether we can use NO_HIDE_DESCENDANTS accessibility importance.
     */
    static final boolean CAN_HIDE_DESCENDANTS = Build.VERSION.SDK_INT >= 19;

    /**
     * Whether the drawer shadow comes from setting elevation on the drawer.
     */
    private static final boolean SET_DRAWER_SHADOW_FROM_ELEVATION =
            Build.VERSION.SDK_INT >= 21;

    private final ChildAccessibilityDelegate mChildAccessibilityDelegate =
            new ChildAccessibilityDelegate();
    private float mDrawerElevation;

    private int mMinDrawerMargin;

    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private float mScrimOpacity;
    private Paint mScrimPaint = new Paint();

    private final ViewDragHelper mTopDragger;
    private final ViewDragHelper mBottomDragger;
    private final ViewDragCallback mTopCallback;
    private final ViewDragCallback mBottomCallback;
    private int mDrawerState;
    private boolean mInLayout;
    private boolean mFirstLayout = true;

    private @LockMode
    int mLockModeTop = LOCK_MODE_UNDEFINED;
    private @LockMode
    int mLockModeBottom = LOCK_MODE_UNDEFINED;
    private @LockMode
    int mLockModeStart = LOCK_MODE_UNDEFINED;
    private @LockMode
    int mLockModeEnd = LOCK_MODE_UNDEFINED;

    private boolean mDisallowInterceptRequested;
    private boolean mChildrenCanceledTouch;

    private @Nullable
    DrawerListener mListener;
    private List<DrawerListener> mListeners;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private Drawable mStatusBarBackground;
    private Drawable mShadowTopResolved;
    private Drawable mShadowBottomResolved;

    private CharSequence mTitleTop;
    private CharSequence mTitleBottom;

    private Object mLastInsets;
    private boolean mDrawStatusBarBackground;

    /**
     * Shadow drawables for different gravity
     */
    private Drawable mShadowStart = null;
    private Drawable mShadowEnd = null;
    private Drawable mShadowTop = null;
    private Drawable mShadowBottom = null;

    private final ArrayList<View> mNonDrawerViews;

    private Rect mChildHitRect;
    private Matrix mChildInvertedMatrix;

    /**
     * Listener for monitoring events about drawers.
     */
    public interface DrawerListener {
        /**
         * Called when a drawer's position changes.
         *
         * @param drawerView  The child view that was moved
         * @param slideOffset The new offset of this drawer within its range, from 0-1
         */
        void onDrawerSlide(@NonNull View drawerView, float slideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param drawerView Drawer view that is now open
         */
        void onDrawerOpened(@NonNull View drawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param drawerView Drawer view that is now closed
         */
        void onDrawerClosed(@NonNull View drawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param newState The new drawer motion state
         */
        void onDrawerStateChanged(@State int newState);
    }

    /**
     * Stub/no-op implementations of all methods of {@link DrawerListener}.
     * Override this if you only care about a few of the available callback methods.
     */
    public abstract static class SimpleDrawerListener implements DrawerListener {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(View drawerView) {
        }

        @Override
        public void onDrawerClosed(View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    }

    public HomeDrawerLayout(@NonNull Context context) {
        this(context, null);
    }

    public HomeDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        final float density = getResources().getDisplayMetrics().density;
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        final float minVel = MIN_FLING_VELOCITY * density;

        mTopCallback = new ViewDragCallback(Gravity.TOP);
        mBottomCallback = new ViewDragCallback(Gravity.BOTTOM);

        mTopDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mTopCallback);
        mTopDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_TOP);
        mTopDragger.setMinVelocity(minVel);
        mTopCallback.setDragger(mTopDragger);

        mBottomDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mBottomCallback);
        mBottomDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);
        mBottomDragger.setMinVelocity(minVel);
        mBottomCallback.setDragger(mBottomDragger);

        setEdgeSize(50);

        // So that we can catch the back button
        setFocusableInTouchMode(true);

        ViewCompat.setImportantForAccessibility(this,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        setMotionEventSplittingEnabled(false);
        if (ViewCompat.getFitsSystemWindows(this)) {
            if (Build.VERSION.SDK_INT >= 21) {
                setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                        final HomeDrawerLayout drawerLayout = (HomeDrawerLayout) view;
                        drawerLayout.setChildInsets(insets, insets.getSystemWindowInsetTop() > 0);
                        return insets.consumeSystemWindowInsets();
                    }
                });
                setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                final TypedArray a = context.obtainStyledAttributes(THEME_ATTRS);
                try {
                    mStatusBarBackground = a.getDrawable(0);
                } finally {
                    a.recycle();
                }
            } else {
                mStatusBarBackground = null;
            }
        }

        mDrawerElevation = DRAWER_ELEVATION * density;

        mNonDrawerViews = new ArrayList<View>();
    }

    /**
     * Sets the base elevation of the drawer(s) relative to the parent, in pixels. Note that the
     * elevation change is only supported in API 21 and above.
     *
     * @param elevation The base depth position of the view, in pixels.
     */
    public void setDrawerElevation(float elevation) {
        mDrawerElevation = elevation;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (isDrawerView(child)) {
                ViewCompat.setElevation(child, mDrawerElevation);
            }
        }
    }

    /**
     * The base elevation of the drawer(s) relative to the parent, in pixels. Note that the
     * elevation change is only supported in API 21 and above. For unsupported API levels, 0 will
     * be returned as the elevation.
     *
     * @return The base depth position of the view, in pixels.
     */
    public float getDrawerElevation() {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return mDrawerElevation;
        }
        return 0f;
    }

    /**
     * @hide Internal use only; called to apply window insets when configured
     * with fitsSystemWindows="true"
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setChildInsets(Object insets, boolean draw) {
        mLastInsets = insets;
        mDrawStatusBarBackground = draw;
        setWillNotDraw(!draw && getBackground() == null);
        requestLayout();
    }

    /**
     * Set a simple drawable used for the left or right shadow. The drawable provided must have a
     * nonzero intrinsic width. For API 21 and above, an elevation will be set on the drawer
     * instead of using the provided shadow drawable.
     *
     * <p>Note that for better support for both left-to-right and right-to-left layout
     * directions, a drawable for RTL layout (in additional to the one in LTR layout) can be
     * defined with a resource qualifier "ldrtl" for API 17 and above with the gravity
     * {@link GravityCompat#START}. Alternatively, for API 23 and above, the drawable can
     * auto-mirrored such that the drawable will be mirrored in RTL layout.</p>
     *
     * @param shadowDrawable Shadow drawable to use at the edge of a drawer
     * @param gravity        Which drawer the shadow should apply to
     */
    public void setDrawerShadow(Drawable shadowDrawable, @EdgeGravity int gravity) {
        /*
         * TODO Someone someday might want to set more complex drawables here.
         * They're probably nuts, but we might want to consider registering callbacks,
         * setting states, etc. properly.
         */
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            // No op. Drawer shadow will come from setting an elevation on the drawer.
            return;
        }
        if ((gravity & GravityCompat.START) == GravityCompat.START) {
            mShadowStart = shadowDrawable;
        } else if ((gravity & GravityCompat.END) == GravityCompat.END) {
            mShadowEnd = shadowDrawable;
        } else if ((gravity & Gravity.TOP) == Gravity.TOP) {
            mShadowTop = shadowDrawable;
        } else if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            mShadowBottom = shadowDrawable;
        } else {
            return;
        }
        resolveShadowDrawables();
        invalidate();
    }

    /**
     * Set a simple drawable used for the left or right shadow. The drawable provided must have a
     * nonzero intrinsic width. For API 21 and above, an elevation will be set on the drawer
     * instead of using the provided shadow drawable.
     *
     * <p>Note that for better support for both left-to-right and right-to-left layout
     * directions, a drawable for RTL layout (in additional to the one in LTR layout) can be
     * defined with a resource qualifier "ldrtl" for API 17 and above with the gravity
     * {@link GravityCompat#START}. Alternatively, for API 23 and above, the drawable can
     * auto-mirrored such that the drawable will be mirrored in RTL layout.</p>
     *
     * @param resId   Resource id of a shadow drawable to use at the edge of a drawer
     * @param gravity Which drawer the shadow should apply to
     */
    public void setDrawerShadow(@DrawableRes int resId, @EdgeGravity int gravity) {
        setDrawerShadow(ContextCompat.getDrawable(getContext(), resId), gravity);
    }

    /**
     * Set a color to use for the scrim that obscures primary content while a drawer is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     */
    public void setScrimColor(@ColorInt int color) {
        mScrimColor = color;
        invalidate();
    }

    /**
     * Set a listener to be notified of drawer events. Note that this method is deprecated
     * and you should use {@link #addDrawerListener(DrawerListener)} to add a listener and
     * {@link #removeDrawerListener(DrawerListener)} to remove a registered listener.
     *
     * @param listener Listener to notify when drawer events occur
     * @see DrawerListener
     * @see #addDrawerListener(DrawerListener)
     * @see #removeDrawerListener(DrawerListener)
     * @deprecated Use {@link #addDrawerListener(DrawerListener)}
     */
    @Deprecated
    public void setDrawerListener(DrawerListener listener) {
        // The logic in this method emulates what we had before support for multiple
        // registered listeners.
        if (mListener != null) {
            removeDrawerListener(mListener);
        }
        if (listener != null) {
            addDrawerListener(listener);
        }
        // Update the deprecated field so that we can remove the passed listener the next
        // time we're called
        mListener = listener;
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur.
     * @see #removeDrawerListener(DrawerListener)
     */
    public void addDrawerListener(@NonNull DrawerListener listener) {
        if (listener == null) {
            return;
        }
        if (mListeners == null) {
            mListeners = new ArrayList<DrawerListener>();
        }
        mListeners.add(listener);
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of drawer
     * events.
     *
     * @param listener Listener to remove from being notified of drawer events
     * @see #addDrawerListener(DrawerListener)
     */
    public void removeDrawerListener(@NonNull DrawerListener listener) {
        if (listener == null) {
            return;
        }
        if (mListeners == null) {
            // This can happen if this method is called before the first call to addDrawerListener
            return;
        }
        mListeners.remove(listener);
    }

    /**
     * Enable or disable interaction with all drawers.
     *
     * <p>This allows the application to restrict the user's ability to open or close
     * any drawer within this layout. HomeDrawerLayout will still respond to calls to
     * {@link #openDrawer(int)}, {@link #closeDrawer(int)} and friends if a drawer is locked.</p>
     *
     * <p>Locking drawers open or closed will implicitly open or close
     * any drawers as appropriate.</p>
     *
     * @param lockMode The new lock mode for the given drawer. One of {@link #LOCK_MODE_UNLOCKED},
     *                 {@link #LOCK_MODE_LOCKED_CLOSED} or {@link #LOCK_MODE_LOCKED_OPEN}.
     */
    public void setDrawerLockMode(@LockMode int lockMode) {
        setDrawerLockMode(lockMode, Gravity.TOP);
        setDrawerLockMode(lockMode, Gravity.BOTTOM);
    }

    /**
     * Enable or disable interaction with the given drawer.
     *
     * <p>This allows the application to restrict the user's ability to open or close
     * the given drawer. HomeDrawerLayout will still respond to calls to {@link #openDrawer(int)},
     * {@link #closeDrawer(int)} and friends if a drawer is locked.</p>
     *
     * <p>Locking a drawer open or closed will implicitly open or close
     * that drawer as appropriate.</p>
     *
     * @param lockMode    The new lock mode for the given drawer. One of {@link #LOCK_MODE_UNLOCKED},
     *                    {@link #LOCK_MODE_LOCKED_CLOSED} or {@link #LOCK_MODE_LOCKED_OPEN}.
     * @param edgeGravity Gravity.TOP, RIGHT, START or END.
     *                    Expresses which drawer to change the mode for.
     * @see #LOCK_MODE_UNLOCKED
     * @see #LOCK_MODE_LOCKED_CLOSED
     * @see #LOCK_MODE_LOCKED_OPEN
     */
    public void setDrawerLockMode(@LockMode int lockMode, @EdgeGravity int edgeGravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(edgeGravity,
                ViewCompat.getLayoutDirection(this));

        switch (edgeGravity) {
            case Gravity.TOP:
                mLockModeTop = lockMode;
                break;
            case Gravity.BOTTOM:
                mLockModeBottom = lockMode;
                break;
            case GravityCompat.START:
                mLockModeStart = lockMode;
                break;
            case GravityCompat.END:
                mLockModeEnd = lockMode;
                break;
        }

        if (lockMode != LOCK_MODE_UNLOCKED) {
            // Cancel interaction in progress
            final ViewDragHelper helper = absGravity == Gravity.TOP ? mTopDragger : mBottomDragger;
            helper.cancel();
        }
        switch (lockMode) {
            case LOCK_MODE_LOCKED_OPEN:
                final View toOpen = findDrawerWithGravity(absGravity);
                if (toOpen != null) {
                    openDrawer(toOpen);
                }
                break;
            case LOCK_MODE_LOCKED_CLOSED:
                final View toClose = findDrawerWithGravity(absGravity);
                if (toClose != null) {
                    closeDrawer(toClose);
                }
                break;
            // default: do nothing
            case LOCK_MODE_UNDEFINED:
                break;
            case LOCK_MODE_UNLOCKED:
                break;
        }
    }

    /**
     * 调整点击屏幕边缘预显示的抽屉宽度
     */
    public void setEdgeSize(int edgeSize) {
        setObjField(mTopDragger, "mEdgeSize", edgeSize);
        setObjField(mBottomDragger, "mEdgeSize", edgeSize);
    }

    public static void setObjField(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Enable or disable interaction with the given drawer.
     *
     * <p>This allows the application to restrict the user's ability to open or close
     * the given drawer. HomeDrawerLayout will still respond to calls to {@link #openDrawer(int)},
     * {@link #closeDrawer(int)} and friends if a drawer is locked.</p>
     *
     * <p>Locking a drawer open or closed will implicitly open or close
     * that drawer as appropriate.</p>
     *
     * @param lockMode   The new lock mode for the given drawer. One of {@link #LOCK_MODE_UNLOCKED},
     *                   {@link #LOCK_MODE_LOCKED_CLOSED} or {@link #LOCK_MODE_LOCKED_OPEN}.
     * @param drawerView The drawer view to change the lock mode for
     * @see #LOCK_MODE_UNLOCKED
     * @see #LOCK_MODE_LOCKED_CLOSED
     * @see #LOCK_MODE_LOCKED_OPEN
     */
    @SuppressLint("WrongConstant")
    public void setDrawerLockMode(@LockMode int lockMode, @NonNull View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a "
                    + "drawer with appropriate layout_gravity");
        }
        final int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        setDrawerLockMode(lockMode, gravity);
    }

    /**
     * Check the lock mode of the drawer with the given gravity.
     *
     * @param edgeGravity Gravity of the drawer to check
     * @return one of {@link #LOCK_MODE_UNLOCKED}, {@link #LOCK_MODE_LOCKED_CLOSED} or
     * {@link #LOCK_MODE_LOCKED_OPEN}.
     */
    @LockMode
    public int getDrawerLockMode(@EdgeGravity int edgeGravity) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);

        switch (edgeGravity) {
            case Gravity.TOP:
                if (mLockModeTop != LOCK_MODE_UNDEFINED) {
                    return mLockModeTop;
                }
                int leftLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeStart : mLockModeEnd;
                if (leftLockMode != LOCK_MODE_UNDEFINED) {
                    return leftLockMode;
                }
                break;
            case Gravity.BOTTOM:
                if (mLockModeBottom != LOCK_MODE_UNDEFINED) {
                    return mLockModeBottom;
                }
                int rightLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeEnd : mLockModeStart;
                if (rightLockMode != LOCK_MODE_UNDEFINED) {
                    return rightLockMode;
                }
                break;
            case GravityCompat.START:
                if (mLockModeStart != LOCK_MODE_UNDEFINED) {
                    return mLockModeStart;
                }
                int startLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeTop : mLockModeBottom;
                if (startLockMode != LOCK_MODE_UNDEFINED) {
                    return startLockMode;
                }
                break;
            case GravityCompat.END:
                if (mLockModeEnd != LOCK_MODE_UNDEFINED) {
                    return mLockModeEnd;
                }
                int endLockMode = (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR)
                        ? mLockModeBottom : mLockModeTop;
                if (endLockMode != LOCK_MODE_UNDEFINED) {
                    return endLockMode;
                }
                break;
        }

        return LOCK_MODE_UNLOCKED;
    }

    /**
     * Check the lock mode of the given drawer view.
     *
     * @param drawerView Drawer view to check lock mode
     * @return one of {@link #LOCK_MODE_UNLOCKED}, {@link #LOCK_MODE_LOCKED_CLOSED} or
     * {@link #LOCK_MODE_LOCKED_OPEN}.
     */

    @SuppressLint("WrongConstant")
    @LockMode
    public int getDrawerLockMode(@NonNull View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a drawer");
        }
        final int drawerGravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return getDrawerLockMode(drawerGravity);
    }

    /**
     * Sets the title of the drawer with the given gravity.
     * <p>
     * When accessibility is turned on, this is the title that will be used to
     * identify the drawer to the active accessibility service.
     *
     * @param edgeGravity Gravity.TOP, RIGHT, START or END. Expresses which
     *                    drawer to set the title for.
     * @param title       The title for the drawer.
     */
    public void setDrawerTitle(@EdgeGravity int edgeGravity, @Nullable CharSequence title) {
        final int absGravity = GravityCompat.getAbsoluteGravity(
                edgeGravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.TOP) {
            mTitleTop = title;
        } else if (absGravity == Gravity.BOTTOM) {
            mTitleBottom = title;
        }
    }

    /**
     * Returns the title of the drawer with the given gravity.
     *
     * @param edgeGravity Gravity.TOP, RIGHT, START or END. Expresses which
     *                    drawer to return the title for.
     * @return The title of the drawer, or null if none set.
     * @see #setDrawerTitle(int, CharSequence)
     */
    @Nullable
    public CharSequence getDrawerTitle(@EdgeGravity int edgeGravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(
                edgeGravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.TOP) {
            return mTitleTop;
        } else if (absGravity == Gravity.BOTTOM) {
            return mTitleBottom;
        }
        return null;
    }

    /**
     * Returns true if x and y coord in HomeDrawerLayout's coordinate space are inside the bounds of the
     * child's coordinate space.
     */
    private boolean isInBoundsOfChild(float x, float y, View child) {
        if (mChildHitRect == null) {
            mChildHitRect = new Rect();
        }
        child.getHitRect(mChildHitRect);
        return mChildHitRect.contains((int) x, (int) y);
    }

    /**
     * Copied from ViewGroup#dispatchTransformedGenericPointerEvent(MotionEvent, View) then modified
     * in order to make calls that are otherwise too visibility restricted to make.
     */
    private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, View child) {
        boolean handled;
        final Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            handled = child.dispatchGenericMotionEvent(transformedEvent);
            transformedEvent.recycle();
        } else {
            final float offsetX = getScrollX() - child.getLeft();
            final float offsetY = getScrollY() - child.getTop();
            event.offsetLocation(offsetX, offsetY);
            handled = child.dispatchGenericMotionEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return handled;
    }

    /**
     * Copied from ViewGroup#getTransformedMotionEvent(MotionEvent, View) then  modified in order to
     * make calls that are otherwise too visibility restricted to make.
     */
    private MotionEvent getTransformedMotionEvent(MotionEvent event, View child) {
        final float offsetX = getScrollX() - child.getLeft();
        final float offsetY = getScrollY() - child.getTop();
        final MotionEvent transformedEvent = MotionEvent.obtain(event);
        transformedEvent.offsetLocation(offsetX, offsetY);
        final Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            if (mChildInvertedMatrix == null) {
                mChildInvertedMatrix = new Matrix();
            }
            childMatrix.invert(mChildInvertedMatrix);
            transformedEvent.transform(mChildInvertedMatrix);
        }
        return transformedEvent;
    }

    /**
     * Resolve the shared state of all drawers from the component ViewDragHelpers.
     * Should be called whenever a ViewDragHelper's state changes.
     */
    void updateDrawerState(int forGravity, @State int activeState, View activeDrawer) {
        final int leftState = mTopDragger.getViewDragState();
        final int rightState = mBottomDragger.getViewDragState();

        final int state;
        if (leftState == STATE_DRAGGING || rightState == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (leftState == STATE_SETTLING || rightState == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0) {
                dispatchOnDrawerClosed(activeDrawer);
            } else if (lp.onScreen == 1) {
                dispatchOnDrawerOpened(activeDrawer);
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;

            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerStateChanged(state);
                }
            }
        }
    }

    boolean ignoreDrawerOpenLockMode = false;

    public boolean isIgnoreDrawerOpenLockMode() {
        return ignoreDrawerOpenLockMode;
    }

    public void setIgnoreDrawerOpenLockMode(boolean ignoreDrawerOpenLockMode) {
        this.ignoreDrawerOpenLockMode = ignoreDrawerOpenLockMode;
    }

    void dispatchOnDrawerClosed(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & LayoutParams.FLAG_IS_OPENED) == 1) {
            lp.openState = 0;

            if (!ignoreDrawerOpenLockMode) {
                setDrawerLockMode(LOCK_MODE_UNDEFINED);
            }
            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerClosed(drawerView);
                }
            }

            updateChildrenImportantForAccessibility(drawerView, false);

            // Only send WINDOW_STATE_CHANGE if the host has window focus. This
            // may change if support for multiple foreground windows (e.g. IME)
            // improves.
            if (hasWindowFocus()) {
                final View rootView = getRootView();
                if (rootView != null) {
                    rootView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                }
            }
        }
    }

    void dispatchOnDrawerOpened(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & LayoutParams.FLAG_IS_OPENED) == 0) {
            lp.openState = LayoutParams.FLAG_IS_OPENED;
            if (!ignoreDrawerOpenLockMode) {
                if (isDrawerOpen(Gravity.TOP)) {
                    setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED, Gravity.BOTTOM);
                } else {
                    setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED, Gravity.TOP);
                }
            }
            if (mListeners != null) {
                // Notify the listeners. Do that from the end of the list so that if a listener
                // removes itself as the result of being called, it won't mess up with our iteration
                int listenerCount = mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    mListeners.get(i).onDrawerOpened(drawerView);
                }
            }

            updateChildrenImportantForAccessibility(drawerView, true);

            // Only send WINDOW_STATE_CHANGE if the host has window focus.
            if (hasWindowFocus()) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
            drawerView.requestFocus();
        }
    }

    private void updateChildrenImportantForAccessibility(View drawerView, boolean isDrawerOpen) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if ((!isDrawerOpen && !isDrawerView(child)) || (isDrawerOpen && child == drawerView)) {
                // Drawer is closed and this is a content view or this is an
                // open drawer view, so it should be visible.
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else {
                ViewCompat.setImportantForAccessibility(child,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }
    }

    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListeners != null) {
            // Notify the listeners. Do that from the end of the list so that if a listener
            // removes itself as the result of being called, it won't mess up with our iteration
            int listenerCount = mListeners.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerSlide(drawerView, slideOffset);
            }
        }
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen) {
            return;
        }

        lp.onScreen = slideOffset;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    /**
     * @return the absolute gravity of the child drawerView, resolved according
     * to the current layout direction
     */
    int getDrawerViewAbsoluteGravity(View drawerView) {
        final int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
    }

    boolean checkDrawerViewAbsoluteGravity(View drawerView, int checkFor) {
        final int absGravity = getDrawerViewAbsoluteGravity(drawerView);
        return (absGravity & checkFor) == checkFor;
    }

    View findOpenDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams childLp = (LayoutParams) child.getLayoutParams();
            if ((childLp.openState & LayoutParams.FLAG_IS_OPENED) == 1) {
                return child;
            }
        }
        return null;
    }

    void moveDrawerToOffset(View drawerView, float slideOffset) {
        final float oldOffset = getDrawerViewOffset(drawerView);
        final int width = drawerView.getWidth();
        final int oldPos = (int) (width * oldOffset);
        final int newPos = (int) (width * slideOffset);
        final int dx = newPos - oldPos;

        drawerView.offsetLeftAndRight(
                checkDrawerViewAbsoluteGravity(drawerView, Gravity.TOP) ? dx : -dx);
        setDrawerViewOffset(drawerView, slideOffset);
    }

    /**
     * @param gravity the gravity of the child to return. If specified as a
     *                relative value, it will be resolved according to the current
     *                layout direction.
     * @return the drawer with the specified gravity
     */
    View findDrawerWithGravity(int gravity) {
        final int absHorizGravity = GravityCompat.getAbsoluteGravity(
                gravity, ViewCompat.getLayoutDirection(this)) & Gravity.VERTICAL_GRAVITY_MASK;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final int childAbsGravity = getDrawerViewAbsoluteGravity(child);
            if ((childAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) == absHorizGravity) {
                return child;
            }
        }
        return null;
    }

    /**
     * Simple gravity to string - only supports LEFT and RIGHT for debugging output.
     *
     * @param gravity Absolute gravity value
     * @return LEFT or RIGHT as appropriate, or a hex string
     */
    static String gravityToString(@EdgeGravity int gravity) {
        if ((gravity & Gravity.TOP) == Gravity.TOP) {
            return "TOP";
        }
        if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            return "BOTTOM";
        }
        return Integer.toHexString(gravity);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            if (isInEditMode()) {
                // Don't crash the layout editor. Consume all of the space if specified
                // or pick a magic number from thin air otherwise.
                // TODO Better communication with tools of this bogus state.
                // It will crash on a real device.
                if (widthMode == MeasureSpec.AT_MOST) {
                    widthMode = MeasureSpec.EXACTLY;
                } else if (widthMode == MeasureSpec.UNSPECIFIED) {
                    widthMode = MeasureSpec.EXACTLY;
                    widthSize = 300;
                }
                if (heightMode == MeasureSpec.AT_MOST) {
                    heightMode = MeasureSpec.EXACTLY;
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    heightMode = MeasureSpec.EXACTLY;
                    heightSize = 300;
                }
            } else {
                throw new IllegalArgumentException(
                        "HomeDrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        final boolean applyInsets = mLastInsets != null && ViewCompat.getFitsSystemWindows(this);
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        // Only one drawer is permitted along each vertical edge (left / right). These two booleans
        // are tracking the presence of the edge drawers.
        boolean hasDrawerOnTopEdge = false;
        boolean hasDrawerOnBottomEdge = false;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (applyInsets) {
                final int cgrav = GravityCompat.getAbsoluteGravity(lp.gravity, layoutDirection);
                if (ViewCompat.getFitsSystemWindows(child)) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        WindowInsets wi = (WindowInsets) mLastInsets;
                        if (cgrav == Gravity.TOP) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(),
                                    wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(),
                                    0);
                        } else if (cgrav == Gravity.BOTTOM) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(), 0,
                                    wi.getSystemWindowInsetRight(),
                                    wi.getSystemWindowInsetBottom());
                        }
                        child.dispatchApplyWindowInsets(wi);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 21) {
                        WindowInsets wi = (WindowInsets) mLastInsets;
                        if (cgrav == Gravity.TOP) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(),
                                    wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(),
                                    0);
                        } else if (cgrav == Gravity.BOTTOM) {
                            wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(), 0,
                                    wi.getSystemWindowInsetRight(),
                                    wi.getSystemWindowInsetBottom());
                        }
                        lp.leftMargin = wi.getSystemWindowInsetLeft();
                        lp.topMargin = wi.getSystemWindowInsetTop();
                        lp.rightMargin = wi.getSystemWindowInsetRight();
                        lp.bottomMargin = wi.getSystemWindowInsetBottom();
                    }
                }
            }

            if (isContentView(child)) {
                // Content views get measured at exactly the layout's size.
                final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else if (isDrawerView(child)) {
                if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
                    if (ViewCompat.getElevation(child) != mDrawerElevation) {
                        ViewCompat.setElevation(child, mDrawerElevation);
                    }
                }
                final @EdgeGravity int childGravity =
                        getDrawerViewAbsoluteGravity(child) & Gravity.VERTICAL_GRAVITY_MASK;
                // Note that the isDrawerView check guarantees that childGravity here is either
                // LEFT or RIGHT
                boolean isTopEdgeDrawer = (childGravity == Gravity.TOP);
                if ((isTopEdgeDrawer && hasDrawerOnTopEdge)
                        || (!isTopEdgeDrawer && hasDrawerOnBottomEdge)) {
                    throw new IllegalStateException("Child drawer has absolute gravity "
                            + gravityToString(childGravity) + " but this " + TAG + " already has a "
                            + "drawer view along that edge");
                }
                if (isTopEdgeDrawer) {
                    hasDrawerOnTopEdge = true;
                } else {
                    hasDrawerOnBottomEdge = true;
                }
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        lp.leftMargin + lp.rightMargin,
                        lp.width);
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        mMinDrawerMargin + lp.topMargin + lp.bottomMargin,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            } else {
                throw new IllegalStateException("Child " + child + " at index " + i
                        + " does not have a valid layout_gravity - must be Gravity.TOP, "
                        + "Gravity.BOTTOM or Gravity.NO_GRAVITY");
            }
        }
    }

    private void resolveShadowDrawables() {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return;
        }
        mShadowTopResolved = resolveLeftShadow();
        mShadowBottomResolved = resolveRightShadow();
    }

    private Drawable resolveLeftShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        // Prefer shadows defined with start/end gravity over left and right.
        if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
            if (mShadowStart != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowStart, layoutDirection);
                return mShadowStart;
            }
        } else {
            if (mShadowEnd != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowEnd, layoutDirection);
                return mShadowEnd;
            }
        }
        return mShadowTop;
    }

    private Drawable resolveRightShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
            if (mShadowEnd != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowEnd, layoutDirection);
                return mShadowEnd;
            }
        } else {
            if (mShadowStart != null) {
                // Correct drawable layout direction, if needed.
                mirror(mShadowStart, layoutDirection);
                return mShadowStart;
            }
        }
        return mShadowBottom;
    }

    /**
     * Change the layout direction of the given drawable.
     * Return true if auto-mirror is supported and drawable's layout direction can be changed.
     * Otherwise, return false.
     */
    private boolean mirror(Drawable drawable, int layoutDirection) {
        if (drawable == null || !DrawableCompat.isAutoMirrored(drawable)) {
            return false;
        }

        DrawableCompat.setLayoutDirection(drawable, layoutDirection);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        final int height = b - t;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isContentView(child)) {
                child.layout(lp.leftMargin, lp.topMargin,
                        lp.leftMargin + child.getMeasuredWidth(),
                        lp.topMargin + child.getMeasuredHeight());
            } else { // Drawer, if it wasn't onMeasure would have thrown an exception.
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                int childTop;

                final float newOffset;
                if (checkDrawerViewAbsoluteGravity(child, Gravity.TOP)) {
                    childTop = -childHeight + (int) (childHeight * lp.onScreen);
                    newOffset = (float) (childHeight + childTop) / childHeight;
                } else { // Right; onMeasure checked for us.
                    childTop = height - (int) (childHeight * lp.onScreen);
                    newOffset = (float) (height - childTop) / childHeight;
                }

                final boolean changeOffset = newOffset != lp.onScreen;

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (vgrav) {
                    default:
                    case Gravity.TOP: {
                        child.layout(lp.leftMargin, childTop, lp.leftMargin + childWidth,
                                childTop + childHeight);
                        break;
                    }

                    case Gravity.BOTTOM: {
                        final int width = r - l;
                        child.layout(width - lp.rightMargin - child.getMeasuredWidth(),
                                childTop,
                                width - lp.rightMargin,
                                childTop + childHeight);
                        break;
                    }

                    case Gravity.CENTER_VERTICAL: {
                        final int width = r - l;
                        int childLeft = (width - childWidth) / 2;

                        // Offset for margins. If things don't fit right because of
                        // bad measurement before, oh well.
                        if (childLeft < lp.topMargin) {
                            childLeft = lp.topMargin;
                        } else if (childLeft + childWidth > width - lp.rightMargin) {
                            childLeft = width - lp.rightMargin - childWidth;
                        }
                        child.layout(childLeft, childTop, childLeft + childWidth,
                                childTop + childHeight);
                        break;
                    }
                }

                if (changeOffset) {
                    setDrawerViewOffset(child, newOffset);
                }

                final int newVisibility = lp.onScreen > 0 ? VISIBLE : INVISIBLE;
                if (child.getVisibility() != newVisibility) {
                    child.setVisibility(newVisibility);
                }
            }
        }
        mInLayout = false;
        mFirstLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mScrimOpacity = scrimOpacity;

        boolean topDraggerSettling = mTopDragger.continueSettling(true);
        boolean bottomDraggerSettling = mBottomDragger.continueSettling(true);
        if (topDraggerSettling || bottomDraggerSettling) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this HomeDrawerLayout fitsSystemWindows.
     *
     * @param bg Background drawable to draw behind the status bar
     */
    public void setStatusBarBackground(@Nullable Drawable bg) {
        mStatusBarBackground = bg;
        invalidate();
    }

    /**
     * Gets the drawable used to draw in the insets area for the status bar.
     *
     * @return The status bar background drawable, or null if none set
     */
    @Nullable
    public Drawable getStatusBarBackgroundDrawable() {
        return mStatusBarBackground;
    }

    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this HomeDrawerLayout fitsSystemWindows.
     *
     * @param resId Resource id of a background drawable to draw behind the status bar
     */
    public void setStatusBarBackground(int resId) {
        mStatusBarBackground = resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null;
        invalidate();
    }

    /**
     * Set a drawable to draw in the insets area for the status bar.
     * Note that this will only be activated if this HomeDrawerLayout fitsSystemWindows.
     *
     * @param color Color to use as a background drawable to draw behind the status bar
     *              in 0xAARRGGBB format.
     */
    public void setStatusBarBackgroundColor(@ColorInt int color) {
        mStatusBarBackground = new ColorDrawable(color);
        invalidate();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        resolveShadowDrawables();
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (mDrawStatusBarBackground && mStatusBarBackground != null) {
            final int inset;
            if (Build.VERSION.SDK_INT >= 21) {
                inset = mLastInsets != null
                        ? ((WindowInsets) mLastInsets).getSystemWindowInsetTop() : 0;
            } else {
                inset = 0;
            }
            if (inset > 0) {
                mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                mStatusBarBackground.draw(c);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int height = getHeight();
        final boolean drawingContent = isContentView(child);
        int clipLeft = 0, clipRight = getWidth();

        final int restoreCount = canvas.save();
        if (drawingContent) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                if (v == child || v.getVisibility() != VISIBLE
                        || !hasOpaqueBackground(v) || !isDrawerView(v)
                        || v.getHeight() < height) {
                    continue;
                }

                if (checkDrawerViewAbsoluteGravity(v, Gravity.TOP)) {
                    final int vright = v.getRight();
                    if (vright > clipLeft) clipLeft = vright;
                } else {
                    final int vleft = v.getLeft();
                    if (vleft < clipRight) clipRight = vleft;
                }
            }
            canvas.clipRect(clipLeft, 0, clipRight, getHeight());
        }
        final boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);

        if (mScrimOpacity > 0 && drawingContent) {
            final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * mScrimOpacity);
            final int color = imag << 24 | (mScrimColor & 0xffffff);
            mScrimPaint.setColor(color);

            canvas.drawRect(clipLeft, 0, clipRight, getHeight(), mScrimPaint);
        } else if (mShadowTopResolved != null
                && checkDrawerViewAbsoluteGravity(child, Gravity.TOP)) {
            final int shadowWidth = mShadowTopResolved.getIntrinsicWidth();
            final int childRight = child.getRight();
            final int drawerPeekDistance = mTopDragger.getEdgeSize();
            final float alpha =
                    Math.max(0, Math.min((float) childRight / drawerPeekDistance, 1.f));
            mShadowTopResolved.setBounds(childRight, child.getTop(),
                    childRight + shadowWidth, child.getBottom());
            mShadowTopResolved.setAlpha((int) (0xff * alpha));
            mShadowTopResolved.draw(canvas);
        } else if (mShadowBottomResolved != null
                && checkDrawerViewAbsoluteGravity(child, Gravity.BOTTOM)) {
            final int shadowWidth = mShadowBottomResolved.getIntrinsicWidth();
            final int childLeft = child.getLeft();
            final int showing = getWidth() - childLeft;
            final int drawerPeekDistance = mBottomDragger.getEdgeSize();
            final float alpha =
                    Math.max(0, Math.min((float) showing / drawerPeekDistance, 1.f));
            mShadowBottomResolved.setBounds(childLeft - shadowWidth, child.getTop(),
                    childLeft, child.getBottom());
            mShadowBottomResolved.setAlpha((int) (0xff * alpha));
            mShadowBottomResolved.draw(canvas);
        }
        return result;
    }

    boolean isContentView(View child) {
        return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
    }

    boolean isDrawerView(View child) {
        final int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
        final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                ViewCompat.getLayoutDirection(child));
        if ((absGravity & Gravity.TOP) != 0) {
            // This child is a left-edge drawer
            return true;
        }
        if ((absGravity & Gravity.BOTTOM) != 0) {
            // This child is a right-edge drawer
            return true;
        }
        return false;
    }

    boolean isTouch = true;

    @SuppressWarnings("ShortCircuitBoolean")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        isTouch = true;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Point point = new Point();
            getDisplay().getSize(point);
            if (isDrawerOpen(Gravity.TOP)) {
                if (ev.getRawY() < point.y * 0.8) {
                    isTouch = false;
                    return super.onInterceptTouchEvent(ev);
                }
                isTouch = true;
            } else if (isDrawerOpen(Gravity.BOTTOM)) {
                if (ev.getRawY() > point.y * 0.2) {
                    isTouch = false;
                    return super.onInterceptTouchEvent(ev);
                }
                isTouch = true;
            }
        }
        if (isTouch) {
            final int action = ev.getActionMasked();

            // "|" used deliberately here; both methods should be invoked.
            final boolean interceptForDrag = mTopDragger.shouldInterceptTouchEvent(ev)
                    | mBottomDragger.shouldInterceptTouchEvent(ev);

            boolean interceptForTap = false;

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    final float x = ev.getX();
                    final float y = ev.getY();
                    mInitialMotionX = x;
                    mInitialMotionY = y;
                    if (mScrimOpacity > 0) {
                        final View child = mTopDragger.findTopChildUnder((int) x, (int) y);
                        if (child != null && isContentView(child)) {
                            interceptForTap = true;
                        }
                    }
                    mDisallowInterceptRequested = false;
                    mChildrenCanceledTouch = false;
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    // If we cross the touch slop, don't perform the delayed peek for an edge touch.
                    if (mTopDragger.checkTouchSlop(ViewDragHelper.DIRECTION_ALL)) {
                        mTopCallback.removeCallbacks();
                        mBottomCallback.removeCallbacks();
                    }
                    break;
                }

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    closeDrawers(true);
                    mDisallowInterceptRequested = false;
                    mChildrenCanceledTouch = false;
                }
            }

            return interceptForDrag || interceptForTap || hasPeekingDrawer() || mChildrenCanceledTouch;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {

        // If this is not a pointer event, or if this is an hover exit, or we are not displaying
        // that the content view can't be interacted with, then don't override and do anything
        // special.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0
                || event.getAction() == MotionEvent.ACTION_HOVER_EXIT
                || mScrimOpacity <= 0) {
            return super.dispatchGenericMotionEvent(event);
        }

        final int childrenCount = getChildCount();
        if (childrenCount != 0) {
            final float x = event.getX();
            final float y = event.getY();

            // Walk through children from top to bottom.
            for (int i = childrenCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);

                // If the event is out of bounds or the child is the content view, don't dispatch
                // to it.
                if (!isInBoundsOfChild(x, y, child) || isContentView(child)) {
                    continue;
                }

                // If a child handles it, return true.
                if (dispatchTransformedGenericPointerEvent(event, child)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mTopDragger.processTouchEvent(ev);
        mBottomDragger.processTouchEvent(ev);

        final int action = ev.getAction();
        boolean wantTouchEvents = true;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                boolean peekingOnly = true;
                final View touchedView = mTopDragger.findTopChildUnder((int) x, (int) y);
                if (touchedView != null && isContentView(touchedView)) {
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mTopDragger.getTouchSlop();
                    if (dx * dx + dy * dy < slop * slop) {
                        // Taps close a dimmed open drawer but only if it isn't locked open.
                        final View openDrawer = findOpenDrawer();
                        if (openDrawer != null) {
                            peekingOnly = getDrawerLockMode(openDrawer) == LOCK_MODE_LOCKED_OPEN;
                        }
                    }
                }
                closeDrawers(peekingOnly);
                mDisallowInterceptRequested = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                closeDrawers(true);
                mDisallowInterceptRequested = false;
                mChildrenCanceledTouch = false;
                break;
            }
        }

        return wantTouchEvents;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (CHILDREN_DISALLOW_INTERCEPT
                || (!mTopDragger.isEdgeTouched(ViewDragHelper.EDGE_TOP)
                && !mBottomDragger.isEdgeTouched(ViewDragHelper.EDGE_BOTTOM))) {
            // If we have an edge touch we want to skip this and track it for later instead.
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
        mDisallowInterceptRequested = disallowIntercept;
        if (disallowIntercept) {
            closeDrawers(true);
        }
    }

    /**
     * Close all currently open drawer views by animating them out of view.
     */
    public void closeDrawers() {
        closeDrawers(false);
    }

    void closeDrawers(boolean peekingOnly) {
        boolean needsInvalidate = false;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (!isDrawerView(child) || (peekingOnly && !lp.isPeeking)) {
                continue;
            }
            final int childHeight = child.getHeight();

            if (checkDrawerViewAbsoluteGravity(child, Gravity.TOP)) {
                needsInvalidate |= mTopDragger.smoothSlideViewTo(child,
                        child.getLeft(), -childHeight);
            } else {
                needsInvalidate |= mBottomDragger.smoothSlideViewTo(child,
                        child.getLeft(), getHeight());
            }

            lp.isPeeking = false;
        }

        mTopCallback.removeCallbacks();
        mBottomCallback.removeCallbacks();

        if (needsInvalidate) {
            invalidate();
        }
    }

    /**
     * Open the specified drawer view by animating it into view.
     *
     * @param drawerView Drawer view to open
     */
    public void openDrawer(@NonNull View drawerView) {
        openDrawer(drawerView, true);
    }

    /**
     * Open the specified drawer view.
     *
     * @param drawerView Drawer view to open
     * @param animate    Whether opening of the drawer should be animated.
     */
    public void openDrawer(@NonNull View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }

        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (mFirstLayout) {
            lp.onScreen = 1.f;
            lp.openState = LayoutParams.FLAG_IS_OPENED;

            updateChildrenImportantForAccessibility(drawerView, true);
        } else if (animate) {
            lp.openState |= LayoutParams.FLAG_IS_OPENING;

            if (checkDrawerViewAbsoluteGravity(drawerView, Gravity.TOP)) {
                mTopDragger.smoothSlideViewTo(drawerView, drawerView.getLeft(), 0);
            } else {
                mBottomDragger.smoothSlideViewTo(drawerView, drawerView.getLeft(),
                        getHeight() - drawerView.getHeight());
            }
        } else {
            moveDrawerToOffset(drawerView, 1.f);
            updateDrawerState(lp.gravity, STATE_IDLE, drawerView);
            drawerView.setVisibility(VISIBLE);
        }
        invalidate();
    }

    /**
     * Open the specified drawer by animating it out of view.
     *
     * @param gravity Gravity.TOP to move the top drawer or Gravity.BOTTOM for the bottom.
     *                GravityCompat.START or GravityCompat.END may also be used.
     */
    public void openDrawer(@EdgeGravity int gravity) {
        openDrawer(gravity, true);
    }

    /**
     * Open the specified drawer.
     *
     * @param gravity Gravity.TOP to move the left drawer or Gravity.BOTTOM for the right.
     *                GravityCompat.START or GravityCompat.END may also be used.
     * @param animate Whether opening of the drawer should be animated.
     */
    public void openDrawer(@EdgeGravity int gravity, boolean animate) {
        final View drawerView = findDrawerWithGravity(gravity);
        if (drawerView == null) {
            throw new IllegalArgumentException("No drawer view found with gravity "
                    + gravityToString(gravity));
        }
        openDrawer(drawerView, animate);
    }

    /**
     * Close the specified drawer view by animating it into view.
     *
     * @param drawerView Drawer view to close
     */
    public void closeDrawer(@NonNull View drawerView) {
        closeDrawer(drawerView, true);
    }

    /**
     * Close the specified drawer view.
     *
     * @param drawerView Drawer view to close
     * @param animate    Whether closing of the drawer should be animated.
     */
    public void closeDrawer(@NonNull View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }

        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (mFirstLayout) {
            lp.onScreen = 0.f;
            lp.openState = 0;
        } else if (animate) {
            lp.openState |= LayoutParams.FLAG_IS_CLOSING;

            if (checkDrawerViewAbsoluteGravity(drawerView, Gravity.TOP)) {
                mTopDragger.smoothSlideViewTo(drawerView, drawerView.getLeft(),
                        -drawerView.getHeight());
            } else {
                mBottomDragger.smoothSlideViewTo(drawerView, drawerView.getLeft(), getHeight());
            }
        } else {
            moveDrawerToOffset(drawerView, 0.f);
            updateDrawerState(lp.gravity, STATE_IDLE, drawerView);
            drawerView.setVisibility(INVISIBLE);
        }
        invalidate();
    }

    /**
     * Close the specified drawer by animating it out of view.
     *
     * @param gravity Gravity.TOP to move the top drawer or Gravity.BOTTOM for the bottom.
     *                GravityCompat.START or GravityCompat.END may also be used.
     */
    public void closeDrawer(@EdgeGravity int gravity) {
        closeDrawer(gravity, true);
    }

    /**
     * Close the specified drawer.
     *
     * @param gravity Gravity.TOP to move the left drawer or Gravity.BOTTOM for the right.
     *                GravityCompat.START or GravityCompat.END may also be used.
     * @param animate Whether closing of the drawer should be animated.
     */
    public void closeDrawer(@EdgeGravity int gravity, boolean animate) {
        final View drawerView = findDrawerWithGravity(gravity);
        if (drawerView == null) {
            throw new IllegalArgumentException("No drawer view found with gravity "
                    + gravityToString(gravity));
        }
        closeDrawer(drawerView, animate);
    }

    /**
     * Check if the given drawer view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state. To check for partial visibility use
     * {@link #isDrawerVisible(android.view.View)}.
     *
     * @param drawer Drawer view to check
     * @return true if the given drawer view is in an open state
     * @see #isDrawerVisible(android.view.View)
     */
    public boolean isDrawerOpen(@NonNull View drawer) {
        if (!isDrawerView(drawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        LayoutParams drawerLp = (LayoutParams) drawer.getLayoutParams();
        return (drawerLp.openState & LayoutParams.FLAG_IS_OPENED) == 1;
    }

    /**
     * Check if the given drawer view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state. If there is no drawer with the given gravity this method
     * will return false.
     *
     * @param drawerGravity Gravity of the drawer to check
     * @return true if the given drawer view is in an open state
     */
    public boolean isDrawerOpen(@EdgeGravity int drawerGravity) {
        final View drawerView = findDrawerWithGravity(drawerGravity);
        if (drawerView != null) {
            return isDrawerOpen(drawerView);
        }
        return false;
    }

    /**
     * Check if a given drawer view is currently visible on-screen. The drawer
     * may be only peeking onto the screen, fully extended, or anywhere inbetween.
     *
     * @param drawer Drawer view to check
     * @return true if the given drawer is visible on-screen
     * @see #isDrawerOpen(android.view.View)
     */
    public boolean isDrawerVisible(@NonNull View drawer) {
        if (!isDrawerView(drawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        return ((LayoutParams) drawer.getLayoutParams()).onScreen > 0;
    }

    /**
     * Check if a given drawer view is currently visible on-screen. The drawer
     * may be only peeking onto the screen, fully extended, or anywhere in between.
     * If there is no drawer with the given gravity this method will return false.
     *
     * @param drawerGravity Gravity of the drawer to check
     * @return true if the given drawer is visible on-screen
     */
    public boolean isDrawerVisible(@EdgeGravity int drawerGravity) {
        final View drawerView = findDrawerWithGravity(drawerGravity);
        if (drawerView != null) {
            return isDrawerVisible(drawerView);
        }
        return false;
    }

    private boolean hasPeekingDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.isPeeking) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof ViewGroup.MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        // Only the views in the open drawers are focusables. Add normal child views when
        // no drawers are opened.
        final int childCount = getChildCount();
        boolean isDrawerOpen = false;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (isDrawerView(child)) {
                if (isDrawerOpen(child)) {
                    isDrawerOpen = true;
                    child.addFocusables(views, direction, focusableMode);
                }
            } else {
                mNonDrawerViews.add(child);
            }
        }

        if (!isDrawerOpen) {
            final int nonDrawerViewsCount = mNonDrawerViews.size();
            for (int i = 0; i < nonDrawerViewsCount; ++i) {
                final View child = mNonDrawerViews.get(i);
                if (child.getVisibility() == View.VISIBLE) {
                    child.addFocusables(views, direction, focusableMode);
                }
            }
        }

        mNonDrawerViews.clear();
    }

    private boolean hasVisibleDrawer() {
        return findVisibleDrawer() != null;
    }

    View findVisibleDrawer() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (isDrawerView(child) && isDrawerVisible(child)) {
                return child;
            }
        }
        return null;
    }

    void cancelChildViewTouch() {
        // Cancel child touches
        if (!mChildrenCanceledTouch) {
            final long now = SystemClock.uptimeMillis();
            final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).dispatchTouchEvent(cancelEvent);
            }
            cancelEvent.recycle();
            mChildrenCanceledTouch = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && hasVisibleDrawer()) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final View visibleDrawer = findVisibleDrawer();
            if (visibleDrawer != null && getDrawerLockMode(visibleDrawer) == LOCK_MODE_UNLOCKED) {
                closeDrawers();
            }
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
            final View toOpen = findDrawerWithGravity(ss.openDrawerGravity);
            if (toOpen != null) {
                openDrawer(toOpen);
            }
        }

        if (ss.lockModeTop != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeTop, Gravity.TOP);
        }
        if (ss.lockModeBottom != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeBottom, Gravity.BOTTOM);
        }
        if (ss.lockModeStart != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeStart, GravityCompat.START);
        }
        if (ss.lockModeEnd != LOCK_MODE_UNDEFINED) {
            setDrawerLockMode(ss.lockModeEnd, GravityCompat.END);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            // Is the current child fully opened (that is, not closing)?
            boolean isOpenedAndNotClosing = (lp.openState == LayoutParams.FLAG_IS_OPENED);
            // Is the current child opening?
            boolean isClosedAndOpening = (lp.openState == LayoutParams.FLAG_IS_OPENING);
            if (isOpenedAndNotClosing || isClosedAndOpening) {
                // If one of the conditions above holds, save the child's gravity
                // so that we open that child during state restore.
                ss.openDrawerGravity = lp.gravity;
                break;
            }
        }

        ss.lockModeTop = mLockModeTop;
        ss.lockModeBottom = mLockModeBottom;
        ss.lockModeStart = mLockModeStart;
        ss.lockModeEnd = mLockModeEnd;

        return ss;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        final View openDrawer = findOpenDrawer();
        if (openDrawer != null || isDrawerView(child)) {
            // A drawer is already open or the new view is a drawer, so the
            // new view should start out hidden.
            ViewCompat.setImportantForAccessibility(child,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        } else {
            // Otherwise this is a content view and no drawer is open, so the
            // new view should start out visible.
            ViewCompat.setImportantForAccessibility(child,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        // We only need a delegate here if the framework doesn't understand
        // NO_HIDE_DESCENDANTS importance.
        if (!CAN_HIDE_DESCENDANTS) {
            ViewCompat.setAccessibilityDelegate(child, mChildAccessibilityDelegate);
        }
    }

    static boolean includeChildForAccessibility(View child) {
        // If the child is not important for accessibility we make
        // sure this hides the entire subtree rooted at it as the
        // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDATS is not
        // supported on older platforms but we want to hide the entire
        // content and not opened drawers if a drawer is opened.
        return ViewCompat.getImportantForAccessibility(child)
                != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                && ViewCompat.getImportantForAccessibility(child)
                != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
    }

    /**
     * State persisted across instances
     */
    protected static class SavedState extends AbsSavedState {
        int openDrawerGravity = Gravity.NO_GRAVITY;
        @LockMode
        int lockModeTop;
        @LockMode
        int lockModeBottom;
        @LockMode
        int lockModeStart;
        @LockMode
        int lockModeEnd;

        public SavedState(@NonNull Parcel in, @Nullable ClassLoader loader) {
            super(in, loader);
            openDrawerGravity = in.readInt();
            lockModeTop = in.readInt();
            lockModeBottom = in.readInt();
            lockModeStart = in.readInt();
            lockModeEnd = in.readInt();
        }

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(openDrawerGravity);
            dest.writeInt(lockModeTop);
            dest.writeInt(lockModeBottom);
            dest.writeInt(lockModeStart);
            dest.writeInt(lockModeEnd);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private final int mAbsGravity;
        private ViewDragHelper mDragger;

        private final Runnable mPeekRunnable = new Runnable() {
            @Override
            public void run() {
                peekDrawer();
            }
        };

        ViewDragCallback(int gravity) {
            mAbsGravity = gravity;
        }

        public void setDragger(ViewDragHelper dragger) {
            mDragger = dragger;
        }

        public void removeCallbacks() {
            HomeDrawerLayout.this.removeCallbacks(mPeekRunnable);
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // Only capture views where the gravity matches what we're looking for.
            // This lets us use two ViewDragHelpers, one for each side drawer.
            return isDrawerView(child) && checkDrawerViewAbsoluteGravity(child, mAbsGravity)
                    && getDrawerLockMode(child) == LOCK_MODE_UNLOCKED;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(mAbsGravity, state, mDragger.getCapturedView());
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;
            final int childHeight = changedView.getHeight();

            // This reverses the positioning shown in onLayout.
            if (checkDrawerViewAbsoluteGravity(changedView, Gravity.TOP)) {
                offset = (float) (childHeight + top) / childHeight;
            } else {
                final int height = getHeight();
                offset = (float) (height - top) / childHeight;
            }
            setDrawerViewOffset(changedView, offset);
            changedView.setVisibility(offset == 0 ? INVISIBLE : VISIBLE);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            final LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
            lp.isPeeking = false;

            closeOtherDrawer();
        }

        private void closeOtherDrawer() {
            final int otherGrav = mAbsGravity == Gravity.TOP ? Gravity.BOTTOM : Gravity.TOP;
            final View toClose = findDrawerWithGravity(otherGrav);
            if (toClose != null) {
                closeDrawer(toClose);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (releasedChild == null) return;
            // Offset is how open the drawer is, therefore left/right values
            // are reversed from one another.
            final float offset = getDrawerViewOffset(releasedChild);
            final int childHeight = releasedChild.getHeight();

            int top;
            if (checkDrawerViewAbsoluteGravity(releasedChild, Gravity.TOP)) {
                top = yvel > 0 || yvel == 0 && offset > 0.5f ? 0 : -childHeight;
            } else {
                final int height = getHeight();
                top = yvel < 0 || yvel == 0 && offset > 0.5f ? height - childHeight : height;
            }

            mDragger.settleCapturedViewAt(releasedChild.getLeft(), top);
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            postDelayed(mPeekRunnable, PEEK_DELAY);
        }

        void peekDrawer() {
            final View toCapture;
            final int childTop;
            final int peekDistance = mDragger.getEdgeSize();
            final boolean topEdge = mAbsGravity == Gravity.TOP;
            if (topEdge) {
                toCapture = findDrawerWithGravity(Gravity.TOP);
                childTop = (toCapture != null ? -toCapture.getHeight() : 0) + peekDistance;
            } else {
                toCapture = findDrawerWithGravity(Gravity.BOTTOM);
                childTop = getHeight() - peekDistance;
            }
            // Only peek if it would mean making the drawer more visible and the drawer isn't locked
            if (toCapture != null && ((topEdge && toCapture.getTop() < childTop)
                    || (!topEdge && toCapture.getTop() > childTop))
                    && getDrawerLockMode(toCapture) == LOCK_MODE_UNLOCKED) {
                final LayoutParams lp = (LayoutParams) toCapture.getLayoutParams();
                mDragger.smoothSlideViewTo(toCapture, toCapture.getLeft(), childTop);
                lp.isPeeking = true;
                invalidate();

                closeOtherDrawer();
                //取消触摸事件会导致内部自定义控件无法触摸
                //cancelChildViewTouch();
            }
        }

        @Override
        public boolean onEdgeLock(int edgeFlags) {
            if (ALLOW_EDGE_LOCK) {
                final View drawer = findDrawerWithGravity(mAbsGravity);
                if (drawer != null && !isDrawerOpen(drawer)) {
                    closeDrawer(drawer);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            final View toCapture;
            if ((edgeFlags & ViewDragHelper.EDGE_TOP) == ViewDragHelper.EDGE_TOP) {
                toCapture = findDrawerWithGravity(Gravity.TOP);
            } else {
                toCapture = findDrawerWithGravity(Gravity.BOTTOM);
            }

            if (toCapture != null && getDrawerLockMode(toCapture) == LOCK_MODE_UNLOCKED) {
                mDragger.captureChildView(toCapture, pointerId);
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return super.getViewHorizontalDragRange(child);
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return isDrawerView(child) ? child.getHeight() : 0;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (checkDrawerViewAbsoluteGravity(child, Gravity.TOP)) {
                return Math.max(-child.getHeight(), Math.min(top, 0));
            } else {
                final int height = getHeight();
                return Math.max(height - child.getHeight(), Math.min(top, height));
            }
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int FLAG_IS_OPENED = 0x1;
        private static final int FLAG_IS_OPENING = 0x2;
        private static final int FLAG_IS_CLOSING = 0x4;

        public int gravity = Gravity.NO_GRAVITY;
        float onScreen;
        boolean isPeeking;
        int openState;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            if (CAN_HIDE_DESCENDANTS) {
                super.onInitializeAccessibilityNodeInfo(host, info);
            } else {
                // Obtain a node for the host, then manually generate the list
                // of children to only include non-obscured views.
                final AccessibilityNodeInfoCompat superNode =
                        AccessibilityNodeInfoCompat.obtain(info);
                super.onInitializeAccessibilityNodeInfo(host, superNode);

                info.setSource(host);
                final ViewParent parent = ViewCompat.getParentForAccessibility(host);
                if (parent instanceof View) {
                    info.setParent((View) parent);
                }
                copyNodeInfoNoChildren(info, superNode);
                superNode.recycle();

                addChildrenForAccessibility(info, (ViewGroup) host);
            }

            info.setClassName(HomeDrawerLayout.class.getName());

            // This view reports itself as focusable so that it can intercept
            // the back button, but we should prevent this view from reporting
            // itself as focusable to accessibility services.
            info.setFocusable(false);
            info.setFocused(false);
            info.removeAction(AccessibilityActionCompat.ACTION_FOCUS);
            info.removeAction(AccessibilityActionCompat.ACTION_CLEAR_FOCUS);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);

            event.setClassName(HomeDrawerLayout.class.getName());
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            // Special case to handle window state change events. As far as
            // accessibility services are concerned, state changes from
            // HomeDrawerLayout invalidate the entire contents of the screen (like
            // an Activity or Dialog) and they should announce the title of the
            // new content.
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                final List<CharSequence> eventText = event.getText();
                final View visibleDrawer = findVisibleDrawer();
                if (visibleDrawer != null) {
                    final int edgeGravity = getDrawerViewAbsoluteGravity(visibleDrawer);
                    final CharSequence title = getDrawerTitle(edgeGravity);
                    if (title != null) {
                        eventText.add(title);
                    }
                }

                return true;
            }

            return super.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                                                       AccessibilityEvent event) {
            if (CAN_HIDE_DESCENDANTS || includeChildForAccessibility(child)) {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
            return false;
        }

        private void addChildrenForAccessibility(AccessibilityNodeInfoCompat info, ViewGroup v) {
            final int childCount = v.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = v.getChildAt(i);
                if (includeChildForAccessibility(child)) {
                    info.addChild(child);
                }
            }
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                                            AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;

            src.getBoundsInParent(rect);
            dest.setBoundsInParent(rect);

            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());
        }
    }

    static final class ChildAccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public void onInitializeAccessibilityNodeInfo(View child,
                                                      AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(child, info);

            if (!includeChildForAccessibility(child)) {
                // If we are ignoring the sub-tree rooted at the child,
                // break the connection to the rest of the node tree.
                // For details refer to includeChildForAccessibility.
                info.setParent(null);
            }
        }
    }
}