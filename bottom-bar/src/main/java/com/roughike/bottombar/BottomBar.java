package com.roughike.bottombar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MenuRes;
import android.support.annotation.StyleRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roughike.bottombar.scrollsweetness.BottomNavigationBehavior;

import java.util.HashMap;

/*
 * BottomBar library for Android
 * Copyright (c) 2016 Iiro Krankka (http://github.com/roughike).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class BottomBar extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    private static final long ANIMATION_DURATION = 150;
    private static final int MAX_FIXED_TAB_COUNT = 3;

    private static final String STATE_CURRENT_SELECTED_TAB = "STATE_CURRENT_SELECTED_TAB";
    private static final String TAG_BOTTOM_BAR_VIEW_INACTIVE = "BOTTOM_BAR_VIEW_INACTIVE";
    private static final String TAG_BOTTOM_BAR_VIEW_ACTIVE = "BOTTOM_BAR_VIEW_ACTIVE";

    private Context mContext;
    private boolean mIgnoreTabletLayout;
    private boolean mIsTabletMode;
    private boolean mIsShy;
    private boolean mShyHeightAlreadyCalculated;
    private boolean mUseExtraOffset;

    private ViewGroup mUserContentContainer;
    private View mOuterContainer;
    private ViewGroup mItemContainer;

    private View mBackgroundView;
    private View mBackgroundOverlay;
    private View mShadowView;
    private View mTabletRightBorder;
    private View mPendingUserContentView;

    private int mPrimaryColor;
    private int mInActiveColor;
    private int mDarkBackgroundColor;
    private int mWhiteColor;

    private int mScreenWidth;
    private int mTwoDp;
    private int mTenDp;
    private int mMaxFixedItemWidth;

    private OnTabSelectedListener mListener;
    private OnMenuTabSelectedListener mMenuListener;

    private int mCurrentTabPosition;
    private boolean mIsShiftingMode;

    private Object mFragmentManager;
    private int mFragmentContainer;

    private BottomBarItemBase[] mItems;
    private HashMap<Integer, Integer> mColorMap;

    private int mCurrentBackgroundColor;
    private int mDefaultBackgroundColor;

    private boolean mIsDarkTheme;
    private int mCustomActiveTabColor = -1;

    private boolean mDrawBehindNavBar = true;
    private boolean mUseTopOffset = true;
    private boolean mUseOnlyStatusBarOffset;

    private int mPendingTextAppearance = -1;
    private Typeface mPendingTypeface;

    // For fragment state restoration
    private boolean mIsComingFromRestoredState;

    /**
     * Bind the BottomBar to your Activity, and inflate your layout here.
     * <p/>
     * Remember to also call {@link #onRestoreInstanceState(Bundle)} inside
     * of your {@link Activity#onSaveInstanceState(Bundle)} to restore the state.
     *
     * @param activity           an Activity to attach to.
     * @param savedInstanceState a Bundle for restoring the state on configuration change.
     * @return a BottomBar at the bottom of the screen.
     */
    public static BottomBar attach(Activity activity, Bundle savedInstanceState) {
        BottomBar bottomBar = new BottomBar(activity);
        bottomBar.onRestoreInstanceState(savedInstanceState);

        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        View oldLayout = contentView.getChildAt(0);
        contentView.removeView(oldLayout);

        bottomBar.setPendingUserContentView(oldLayout);
        contentView.addView(bottomBar, 0);

        return bottomBar;
    }

    private void setPendingUserContentView(View oldLayout) {
        mPendingUserContentView = oldLayout;
    }

    /**
     * Bind the BottomBar to the specified View's parent, and inflate
     * your layout there. Useful when the BottomBar overlaps some content
     * that shouldn't be overlapped.
     * <p/>
     * Remember to also call {@link #onRestoreInstanceState(Bundle)} inside
     * of your {@link Activity#onSaveInstanceState(Bundle)} to restore the state.
     *
     * @param view               a View, which parent we're going to attach to.
     * @param savedInstanceState a Bundle for restoring the state on configuration change.
     * @return a BottomBar at the bottom of the screen.
     */
    public static BottomBar attach(View view, Bundle savedInstanceState) {
        BottomBar bottomBar = new BottomBar(view.getContext());
        bottomBar.onRestoreInstanceState(savedInstanceState);

        ViewGroup contentView = (ViewGroup) view.getParent();

        if (contentView != null) {
            View oldLayout = contentView.getChildAt(0);
            contentView.removeView(oldLayout);

            bottomBar.setPendingUserContentView(oldLayout);
            contentView.addView(bottomBar, 0);
        } else {
            bottomBar.setPendingUserContentView(view);
        }

        return bottomBar;
    }

    /**
     * Deprecated. Breaks support for tablets.
     * Use {@link #attachShy(CoordinatorLayout, View, Bundle)} instead.
     */
    @Deprecated
    public static BottomBar attachShy(CoordinatorLayout coordinatorLayout, Bundle savedInstanceState) {
        return attachShy(coordinatorLayout, null, savedInstanceState);
    }

    /**
     * Adds the BottomBar inside of your CoordinatorLayout and shows / hides
     * it according to scroll state changes.
     * <p/>
     * Remember to also call {@link #onRestoreInstanceState(Bundle)} inside
     * of your {@link Activity#onSaveInstanceState(Bundle)} to restore the state.
     *
     * @param coordinatorLayout  a CoordinatorLayout for the BottomBar to add itself into
     * @param userContentView    the view (usually a NestedScrollView) that has your scrolling content.
     *                           Needed for tablet support.
     * @param savedInstanceState a Bundle for restoring the state on configuration change.
     * @return a BottomBar at the bottom of the screen.
     */
    public static BottomBar attachShy(CoordinatorLayout coordinatorLayout, View userContentView, Bundle savedInstanceState) {
        final BottomBar bottomBar = new BottomBar(coordinatorLayout.getContext());
        bottomBar.toughChildHood(ViewCompat.getFitsSystemWindows(coordinatorLayout));
        bottomBar.onRestoreInstanceState(savedInstanceState);

        if (userContentView != null && coordinatorLayout.getContext()
                .getResources().getBoolean(R.bool.bb_bottom_bar_is_tablet_mode)) {
            bottomBar.setPendingUserContentView(userContentView);
        }

        coordinatorLayout.addView(bottomBar);
        return bottomBar;
    }

    /**
     * Set tabs and fragments for this BottomBar. When setting more than 3 items,
     * only the icons will show by default, but the selected item
     * will have the text visible.
     *
     * @param fragmentManager   a FragmentManager for managing the Fragments.
     * @param containerResource id for the layout to inflate Fragments to.
     * @param fragmentItems     an array of {@link BottomBarFragment} objects.
     */
    public void setFragmentItems(android.app.FragmentManager fragmentManager, @IdRes int containerResource,
                                 BottomBarFragment... fragmentItems) {
        if (fragmentItems.length > 0) {
            int index = 0;

            for (BottomBarFragment fragmentItem : fragmentItems) {
                if (fragmentItem.getFragment() == null
                        && fragmentItem.getSupportFragment() != null) {
                    throw new IllegalArgumentException("Conflict: cannot use android.app.FragmentManager " +
                            "to handle a android.support.v4.app.Fragment object at position " + index +
                            ". If you want BottomBar to handle support Fragments, use getSupportFragment" +
                            "Manager() instead of getFragmentManager().");
                }

                index++;
            }
        }

        clearItems();
        mFragmentManager = fragmentManager;
        mFragmentContainer = containerResource;
        mItems = fragmentItems;
        updateItems(mItems);
    }

    /**
     * Set tabs and fragments for this BottomBar. When setting more than 3 items,
     * only the icons will show by default, but the selected item
     * will have the text visible.
     *
     * @param fragmentManager   a FragmentManager for managing the Fragments.
     * @param containerResource id for the layout to inflate Fragments to.
     * @param fragmentItems     an array of {@link BottomBarFragment} objects.
     */
    public void setFragmentItems(android.support.v4.app.FragmentManager fragmentManager, @IdRes int containerResource,
                                 BottomBarFragment... fragmentItems) {
        if (fragmentItems.length > 0) {
            int index = 0;

            for (BottomBarFragment fragmentItem : fragmentItems) {
                if (fragmentItem.getSupportFragment() == null
                        && fragmentItem.getFragment() != null) {
                    throw new IllegalArgumentException("Conflict: cannot use android.support.v4.app.FragmentManager " +
                            "to handle a android.app.Fragment object at position " + index +
                            ". If you want BottomBar to handle normal Fragments, use getFragment" +
                            "Manager() instead of getSupportFragmentManager().");
                }

                index++;
            }
        }
        clearItems();
        mFragmentManager = fragmentManager;
        mFragmentContainer = containerResource;
        mItems = fragmentItems;
        updateItems(mItems);
    }

    /**
     * Set tabs for this BottomBar. When setting more than 3 items,
     * only the icons will show by default, but the selected item
     * will have the text visible.
     *
     * @param bottomBarTabs an array of {@link BottomBarTab} objects.
     */
    public void setItems(BottomBarTab... bottomBarTabs) {
        clearItems();
        mItems = bottomBarTabs;
        updateItems(mItems);
    }

    /**
     * Set items from an XML menu resource file.
     *
     * @param menuRes  the menu resource to inflate items from.
     * @param listener listener for tab change events.
     */
    public void setItemsFromMenu(@MenuRes int menuRes, OnMenuTabSelectedListener listener) {
        clearItems();
        mItems = MiscUtils.inflateMenuFromResource((Activity) getContext(), menuRes);
        mMenuListener = listener;
        updateItems(mItems);
    }

    /**
     * Set a listener that gets fired when the selected tab changes.
     *
     * @param listener a listener for monitoring changes in tab selection.
     */
    public void setOnItemSelectedListener(OnTabSelectedListener listener) {
        mListener = listener;
    }

    /**
     * Select a tab at the specified position.
     *
     * @param position the position to select.
     */
    public void selectTabAtPosition(int position, boolean animate) {
        if (mItems == null || mItems.length == 0) {
            throw new UnsupportedOperationException("Can't select tab at " +
                    "position " + position + ". This BottomBar has no items set yet.");
        } else if (position > mItems.length - 1 || position < 0) {
            throw new IndexOutOfBoundsException("Can't select tab at position " +
                    position + ". This BottomBar has no items at that position.");
        }

        unselectTab(mItemContainer.findViewWithTag(TAG_BOTTOM_BAR_VIEW_ACTIVE), animate);
        selectTab(mItemContainer.getChildAt(position), animate);

        updateSelectedTab(position);
    }

    /**
     * Call this method in your Activity's onSaveInstanceState
     * to keep the BottomBar's state on configuration change.
     *
     * @param outState the Bundle to save data to.
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SELECTED_TAB, mCurrentTabPosition);

        if (mFragmentManager != null
                && mFragmentContainer != 0
                && mItems != null
                && mItems instanceof BottomBarFragment[]) {
            BottomBarFragment bottomBarFragment = (BottomBarFragment) mItems[mCurrentTabPosition];

            if (bottomBarFragment.getFragment() != null) {
                bottomBarFragment.getFragment().onSaveInstanceState(outState);
            } else if (bottomBarFragment.getSupportFragment() != null) {
                bottomBarFragment.getSupportFragment().onSaveInstanceState(outState);
            }
        }
    }

    /**
     * Map a background color for a Tab, that changes the whole BottomBar
     * background color when the Tab is selected.
     *
     * @param tabPosition zero-based index for the tab.
     * @param color       a hex color for the tab, such as 0xFF00FF00.
     */
    public void mapColorForTab(int tabPosition, int color) {
        if (mItems == null || mItems.length == 0) {
            throw new UnsupportedOperationException("You have no BottomBar Tabs set yet. " +
                    "Please set them first before calling the mapColorForTab method.");
        } else if (tabPosition > mItems.length - 1 || tabPosition < 0) {
            throw new IndexOutOfBoundsException("Cant map color for Tab " +
                    "index " + tabPosition + ". You have no BottomBar Tabs at that position.");
        }

        if (!mIsShiftingMode || mIsTabletMode) return;

        if (mColorMap == null) {
            mColorMap = new HashMap<>();
        }

        if (tabPosition == mCurrentTabPosition
                && mCurrentBackgroundColor != color) {
            mCurrentBackgroundColor = color;
            mBackgroundView.setBackgroundColor(color);
        }

        mColorMap.put(tabPosition, color);
    }

    /**
     * Map a background color for a Tab, that changes the whole BottomBar
     * background color when the Tab is selected.
     *
     * @param tabPosition zero-based index for the tab.
     * @param color       a hex color for the tab, such as "#00FF000".
     */
    public void mapColorForTab(int tabPosition, String color) {
        mapColorForTab(tabPosition, Color.parseColor(color));
    }

    /**
     * Use dark theme instead of the light one.
     * <p/>
     * NOTE: You might want to change your active tab color to something else
     * using {@link #setActiveTabColor(int)}, as the default primary color might
     * not have enough contrast for the dark background.
     *
     * @param darkThemeEnabled whether the dark the should be enabled or not.
     */
    public void useDarkTheme(boolean darkThemeEnabled) {
        if (!mIsDarkTheme && darkThemeEnabled
                && mItems != null && mItems.length > 0) {
            darkThemeMagic();

            for (int i = 0; i < mItemContainer.getChildCount(); i++) {
                View bottomBarTab = mItemContainer.getChildAt(i);
                ((ImageView) bottomBarTab.findViewById(R.id.bb_bottom_bar_icon))
                        .setColorFilter(mWhiteColor);

                if (i == mCurrentTabPosition) {
                    selectTab(bottomBarTab, false);
                } else {
                    unselectTab(bottomBarTab, false);
                }
            }
        }

        mIsDarkTheme = darkThemeEnabled;
    }

    /**
     * Set a custom color for an active tab when there's three
     * or less items.
     * <p/>
     * NOTE: This value is ignored on mobile devices if you have more than
     * three items.
     *
     * @param activeTabColor a hex color used for active tabs, such as "#00FF000".
     */
    public void setActiveTabColor(String activeTabColor) {
        setActiveTabColor(Color.parseColor(activeTabColor));
    }

    /**
     * Set a custom color for an active tab when there's three
     * or less items.
     * <p/>
     * NOTE: This value is ignored if you have more than three items.
     *
     * @param activeTabColor a hex color used for active tabs, such as 0xFF00FF00.
     */
    public void setActiveTabColor(int activeTabColor) {
        mCustomActiveTabColor = activeTabColor;
    }

    /**
     * Set a custom TypeFace for the tab titles.
     * The .ttf file should be located at "/src/main/assets".
     *
     * @param typeFacePath path for the custom typeface in the assets directory.
     */
    public void setTypeFace(String typeFacePath) {
        Typeface typeface = Typeface.createFromAsset(mContext.getAssets(),
                typeFacePath);

        if (mItemContainer != null && mItemContainer.getChildCount() > 0) {
            for (int i = 0; i < mItemContainer.getChildCount(); i++) {
                View bottomBarTab = mItemContainer.getChildAt(i);
                TextView title = (TextView) bottomBarTab.findViewById(R.id.bb_bottom_bar_title);
                title.setTypeface(typeface);
            }
        } else {
            mPendingTypeface = typeface;
        }
    }

    /**
     * Set a custom text appearance for the tab title.
     *
     * @param resId path to the custom text appearance.
     */
    public void setTextAppearance(@StyleRes int resId) {
        if (mItemContainer != null && mItemContainer.getChildCount() > 0) {
            for (int i = 0; i < mItemContainer.getChildCount(); i++) {
                View bottomBarTab = mItemContainer.getChildAt(i);
                TextView title = (TextView) bottomBarTab.findViewById(R.id.bb_bottom_bar_title);
                MiscUtils.setTextAppearance(title, resId);
            }
        } else {
            mPendingTextAppearance = resId;
        }
    }

    /**
     * Hide the shadow that's normally above the BottomBar.
     */
    public void hideShadow() {
        if (mShadowView != null) {
            mShadowView.setVisibility(GONE);
        }
    }

    /**
     * Prevent the BottomBar drawing behind the Navigation Bar and making
     * it transparent. Must be called before setting items.
     */
    public void noNavBarGoodness() {
        if (mItems != null) {
            throw new UnsupportedOperationException("This BottomBar already has items! " +
                    "You must call noNavBarGoodness() before setting the items, preferably " +
                    "right after attaching it to your layout.");
        }

        mDrawBehindNavBar = false;
    }

    /**
     * Force the BottomBar to behave exactly same on tablets and phones,
     * instead of showing a left menu on tablets.
     */
    public void noTabletGoodness() {
        if (mItems != null) {
            throw new UnsupportedOperationException("This BottomBar already has items! " +
                    "You must call noTabletGoodness() before setting the items, preferably " +
                    "right after attaching it to your layout.");
        }

        mIgnoreTabletLayout = true;
    }

    /**
     * Super ugly hacks
     * ----------------------------/
     */

    /**
     * If you get some unwanted extra padding in the top (such as
     * when using CoordinatorLayout), this fixes it.
     */
    public void noTopOffset() {
        mUseTopOffset = false;
    }

    /**
     * If your ActionBar gets inside the status bar for some reason,
     * this fixes it.
     */
    public void useOnlyStatusBarTopOffset() {
        mUseOnlyStatusBarOffset = true;
    }

    /**
     * ------------------------------------------- //
     */
    public BottomBar(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public BottomBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        mDarkBackgroundColor = ContextCompat.getColor(getContext(), R.color.bb_darkBackgroundColor);
        mWhiteColor = ContextCompat.getColor(getContext(), R.color.white);
        mPrimaryColor = MiscUtils.getColor(getContext(), R.attr.colorPrimary);
        mInActiveColor = ContextCompat.getColor(getContext(), R.color.bb_inActiveBottomBarItemColor);

        mScreenWidth = MiscUtils.getScreenWidth(mContext);
        mTwoDp = MiscUtils.dpToPixel(mContext, 2);
        mTenDp = MiscUtils.dpToPixel(mContext, 10);
        mMaxFixedItemWidth = MiscUtils.dpToPixel(mContext, 168);
    }

    private void initializeViews() {
        mIsTabletMode = !mIgnoreTabletLayout &&
                mContext.getResources().getBoolean(R.bool.bb_bottom_bar_is_tablet_mode);

        View rootView = View.inflate(mContext, mIsTabletMode ?
                        R.layout.bb_bottom_bar_item_container_tablet : R.layout.bb_bottom_bar_item_container,
                null);
        mTabletRightBorder = rootView.findViewById(R.id.bb_tablet_right_border);

        mUserContentContainer = (ViewGroup) rootView.findViewById(R.id.bb_user_content_container);
        mShadowView = rootView.findViewById(R.id.bb_bottom_bar_shadow);

        mOuterContainer = rootView.findViewById(R.id.bb_bottom_bar_outer_container);
        mItemContainer = (ViewGroup) rootView.findViewById(R.id.bb_bottom_bar_item_container);

        mBackgroundView = rootView.findViewById(R.id.bb_bottom_bar_background_view);
        mBackgroundOverlay = rootView.findViewById(R.id.bb_bottom_bar_background_overlay);

        if (mIsShy && mIgnoreTabletLayout) {
            mPendingUserContentView = null;
        }

        if (mPendingUserContentView != null) {
            ViewGroup.LayoutParams params = mPendingUserContentView.getLayoutParams();

            if (params == null) {
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }

            if (mIsTabletMode && mIsShy) {
                ((ViewGroup) mPendingUserContentView.getParent()).removeView(mPendingUserContentView);
            }

            mUserContentContainer.addView(mPendingUserContentView, 0, params);
            mPendingUserContentView = null;
        }

        if (mIsShy && !mIsTabletMode) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    if (!mShyHeightAlreadyCalculated) {
                        ((CoordinatorLayout.LayoutParams) getLayoutParams())
                                .setBehavior(new BottomNavigationBehavior(getOuterContainer().getHeight(), 0));
                    }

                    ViewTreeObserver obs = getViewTreeObserver();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        obs.removeOnGlobalLayoutListener(this);
                    } else {
                        obs.removeGlobalOnLayoutListener(this);
                    }
                }
            });
        }

        addView(rootView);
    }

    /**
     * Makes this BottomBar "shy". In other words, it hides on scroll.
     */
    private void toughChildHood(boolean useExtraOffset) {
        mIsShy = true;
        mUseExtraOffset = useExtraOffset;
    }

    protected boolean isShy() {
        return mIsShy;
    }

    protected void shyHeightAlreadyCalculated() {
        mShyHeightAlreadyCalculated = true;
    }

    protected boolean useExtraOffset() {
        return mUseExtraOffset;
    }

    protected ViewGroup getUserContainer() {
        return mUserContentContainer;
    }

    protected View getOuterContainer() {
        return mOuterContainer;
    }

    protected boolean drawBehindNavBar() {
        return mDrawBehindNavBar;
    }

    protected boolean useTopOffset() {
        return mUseTopOffset;
    }

    protected boolean useOnlyStatusbarOffset() {
        return mUseOnlyStatusBarOffset;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag().equals(TAG_BOTTOM_BAR_VIEW_INACTIVE)) {
            unselectTab(findViewWithTag(TAG_BOTTOM_BAR_VIEW_ACTIVE), true);
            selectTab(v, true);
            updateSelectedTab(findItemPosition(v));
        }
    }

    private void updateSelectedTab(int newPosition) {
        if (newPosition != mCurrentTabPosition) {
            mCurrentTabPosition = newPosition;

            if (mListener != null) {
                mListener.onItemSelected(mCurrentTabPosition);
            }

            if (mMenuListener != null && mItems instanceof BottomBarTab[]) {
                mMenuListener.onMenuItemSelected(((BottomBarTab) mItems[mCurrentTabPosition]).id);
            }

            updateCurrentFragment();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if ((mIsShiftingMode || mIsTabletMode) && v.getTag().equals(TAG_BOTTOM_BAR_VIEW_INACTIVE)) {
            Toast.makeText(mContext, mItems[findItemPosition(v)].getTitle(mContext), Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    private void updateItems(final BottomBarItemBase[] bottomBarItems) {
        if (mItemContainer == null) {
            initializeViews();
        }

        int index = 0;
        int biggestWidth = 0;
        mIsShiftingMode = MAX_FIXED_TAB_COUNT < bottomBarItems.length;

        if (!mIsTabletMode && mIsShiftingMode) {
            mDefaultBackgroundColor = mCurrentBackgroundColor = mPrimaryColor;
            mBackgroundView.setBackgroundColor(mDefaultBackgroundColor);

            if (mContext instanceof Activity) {
                navBarMagic((Activity) mContext, this);
            }
        } else if (mIsDarkTheme) {
            darkThemeMagic();
        }

        View[] viewsToAdd = new View[bottomBarItems.length];

        for (BottomBarItemBase bottomBarItemBase : bottomBarItems) {
            int layoutResource;

            if (mIsShiftingMode && !mIsTabletMode) {
                layoutResource = R.layout.bb_bottom_bar_item_shifting;
            } else {
                layoutResource = mIsTabletMode ?
                        R.layout.bb_bottom_bar_item_fixed_tablet : R.layout.bb_bottom_bar_item_fixed;
            }

            View bottomBarTab = View.inflate(mContext, layoutResource, null);
            ImageView icon = (ImageView) bottomBarTab.findViewById(R.id.bb_bottom_bar_icon);

            icon.setImageDrawable(bottomBarItemBase.getIcon(mContext));

            if (!mIsTabletMode) {
                TextView title = (TextView) bottomBarTab.findViewById(R.id.bb_bottom_bar_title);
                title.setText(bottomBarItemBase.getTitle(mContext));

                if (mPendingTextAppearance != -1) {
                    MiscUtils.setTextAppearance(title, mPendingTextAppearance);
                }

                if (mPendingTypeface != null) {
                    title.setTypeface(mPendingTypeface);
                }
            }

            if (mIsDarkTheme || (!mIsTabletMode && mIsShiftingMode)) {
                icon.setColorFilter(mWhiteColor);
            }

            if (bottomBarItemBase instanceof BottomBarTab) {
                bottomBarTab.setId(((BottomBarTab) bottomBarItemBase).id);
            }

            if (index == mCurrentTabPosition) {
                selectTab(bottomBarTab, false);
            } else {
                unselectTab(bottomBarTab, false);
            }

            if (!mIsTabletMode) {
                if (bottomBarTab.getWidth() > biggestWidth) {
                    biggestWidth = bottomBarTab.getWidth();
                }

                viewsToAdd[index] = bottomBarTab;
            } else {
                mItemContainer.addView(bottomBarTab);
            }

            bottomBarTab.setOnClickListener(this);
            bottomBarTab.setOnLongClickListener(this);
            index++;
        }

        if (!mIsTabletMode) {
            int proposedItemWidth = Math.min(
                    MiscUtils.dpToPixel(mContext, mScreenWidth / bottomBarItems.length),
                    mMaxFixedItemWidth
            );

            LinearLayout.LayoutParams params = new LinearLayout
                    .LayoutParams(proposedItemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

            for (View bottomBarView : viewsToAdd) {
                bottomBarView.setLayoutParams(params);
                mItemContainer.addView(bottomBarView);
            }
        }

        updateCurrentFragment();

        if (mPendingTextAppearance != -1) {
            mPendingTextAppearance = -1;
        }

        if (mPendingTypeface != null) {
            mPendingTypeface = null;
        }
    }

    private void darkThemeMagic() {
        if (!mIsTabletMode) {
            mBackgroundView.setBackgroundColor(mDarkBackgroundColor);
        } else {
            mItemContainer.setBackgroundColor(mDarkBackgroundColor);
            mTabletRightBorder.setBackgroundColor(ContextCompat.getColor(mContext, R.color.bb_tabletRightBorderDark));
        }
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentTabPosition = savedInstanceState.getInt(STATE_CURRENT_SELECTED_TAB, -1);

            if (mCurrentTabPosition == -1) {
                mCurrentTabPosition = 0;
                Log.e("BottomBar", "You must override the Activity's onSave" +
                        "InstanceState(Bundle outState) and call BottomBar.onSaveInstanc" +
                        "eState(outState) there to restore the state properly.");
            }

            mIsComingFromRestoredState = true;
        }
    }

    private void selectTab(View tab, boolean animate) {
        tab.setTag(TAG_BOTTOM_BAR_VIEW_ACTIVE);
        ImageView icon = (ImageView) tab.findViewById(R.id.bb_bottom_bar_icon);
        TextView title = (TextView) tab.findViewById(R.id.bb_bottom_bar_title);

        int tabPosition = findItemPosition(tab);

        if (!mIsShiftingMode || mIsTabletMode) {
            int activeColor = mCustomActiveTabColor != -1 ?
                    mCustomActiveTabColor : mPrimaryColor;
            icon.setColorFilter(activeColor);

            if (title != null) {
                title.setTextColor(activeColor);
            }
        }

        if (mIsDarkTheme) {
            if (title != null) {
                title.setAlpha(1.0f);
            }

            icon.setAlpha(1.0f);
        }

        if (title == null) {
            return;
        }

        int translationY = mIsShiftingMode ? mTenDp : mTwoDp;

        if (animate) {
            title.animate()
                    .setDuration(ANIMATION_DURATION)
                    .scaleX(1)
                    .scaleY(1)
                    .start();
            tab.animate()
                    .setDuration(ANIMATION_DURATION)
                    .translationY(-translationY)
                    .start();

            if (mIsShiftingMode) {
                icon.animate()
                        .setDuration(ANIMATION_DURATION)
                        .alpha(1.0f)
                        .start();
            }

            handleBackgroundColorChange(tabPosition, tab);
        } else {
            title.setScaleX(1);
            title.setScaleY(1);
            tab.setTranslationY(-translationY);

            if (mIsShiftingMode) {
                icon.setAlpha(1.0f);
            }
        }
    }

    private void unselectTab(View tab, boolean animate) {
        tab.setTag(TAG_BOTTOM_BAR_VIEW_INACTIVE);

        ImageView icon = (ImageView) tab.findViewById(R.id.bb_bottom_bar_icon);
        TextView title = (TextView) tab.findViewById(R.id.bb_bottom_bar_title);

        if (!mIsShiftingMode || mIsTabletMode) {
            int inActiveColor = mIsDarkTheme ? mWhiteColor : mInActiveColor;
            icon.setColorFilter(inActiveColor);

            if (title != null) {
                title.setTextColor(inActiveColor);
            }
        }

        if (mIsDarkTheme) {
            if (title != null) {
                title.setAlpha(0.6f);
            }

            icon.setAlpha(0.6f);
        }

        if (title == null) {
            return;
        }

        float scale = mIsShiftingMode ? 0 : 0.86f;

        if (animate) {
            title.animate()
                    .setDuration(ANIMATION_DURATION)
                    .scaleX(scale)
                    .scaleY(scale)
                    .start();
            tab.animate()
                    .setDuration(ANIMATION_DURATION)
                    .translationY(0)
                    .start();

            if (mIsShiftingMode) {
                icon.animate()
                        .setDuration(ANIMATION_DURATION)
                        .alpha(0.6f)
                        .start();
            }
        } else {
            title.setScaleX(scale);
            title.setScaleY(scale);
            tab.setTranslationY(0);

            if (mIsShiftingMode) {
                icon.setAlpha(0.6f);
            }
        }
    }

    private void handleBackgroundColorChange(int tabPosition, View tab) {
        if (!mIsShiftingMode || mIsTabletMode) return;

        if (mColorMap != null && mColorMap.containsKey(tabPosition)) {
            handleBackgroundColorChange(
                    tab, mColorMap.get(tabPosition));
        } else {
            handleBackgroundColorChange(tab, mDefaultBackgroundColor);
        }
    }

    private void handleBackgroundColorChange(View tab, int color) {
        MiscUtils.animateBGColorChange(tab,
                mBackgroundView,
                mBackgroundOverlay,
                color);
        mCurrentBackgroundColor = color;
    }

    private int findItemPosition(View viewToFind) {
        int position = 0;

        for (int i = 0; i < mItemContainer.getChildCount(); i++) {
            View candidate = mItemContainer.getChildAt(i);

            if (candidate.equals(viewToFind)) {
                position = i;
                break;
            }
        }

        return position;
    }

    private void updateCurrentFragment() {
        if (!mIsComingFromRestoredState && mFragmentManager != null
                && mFragmentContainer != 0
                && mItems != null
                && mItems instanceof BottomBarFragment[]) {
            BottomBarFragment newFragment = ((BottomBarFragment) mItems[mCurrentTabPosition]);

            if (mFragmentManager instanceof android.app.FragmentManager
                    && newFragment.getFragment() != null) {
                ((android.app.FragmentManager) mFragmentManager).beginTransaction()
                        .replace(mFragmentContainer, newFragment.getFragment())
                        .commit();
            } else if (mFragmentManager instanceof android.support.v4.app.FragmentManager
                    && newFragment.getSupportFragment() != null) {
                ((android.support.v4.app.FragmentManager) mFragmentManager).beginTransaction()
                        .replace(mFragmentContainer, newFragment.getSupportFragment())
                        .commit();
            }
        }

        mIsComingFromRestoredState = false;
    }

    private void clearItems() {
        if (mItemContainer != null) {
            int childCount = mItemContainer.getChildCount();

            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    mItemContainer.removeView(mItemContainer.getChildAt(i));
                }
            }
        }

        if (mFragmentManager != null) {
            mFragmentManager = null;
        }

        if (mFragmentContainer != 0) {
            mFragmentContainer = 0;
        }

        if (mItems != null) {
            mItems = null;
        }
    }

    private static void navBarMagic(Activity activity, final BottomBar bottomBar) {
        Resources res = activity.getResources();
        int softMenuIdentifier = res
                .getIdentifier("config_showNavigationBar", "bool", "android");
        int navBarIdentifier = res.getIdentifier("navigation_bar_height",
                "dimen", "android");
        int navBarHeight = 0;

        if (navBarIdentifier > 0) {
            navBarHeight = res.getDimensionPixelSize(navBarIdentifier);
        }

        if (!bottomBar.drawBehindNavBar()
                || navBarHeight == 0
                || (!(softMenuIdentifier > 0 && res.getBoolean(softMenuIdentifier))
                && ViewConfiguration.get(activity).hasPermanentMenuKey())) {
            return;
        }

        /**
         * Copy-paste coding made possible by:
         * http://stackoverflow.com/a/14871974/940036
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display d = activity.getWindowManager().getDefaultDisplay();

            DisplayMetrics realDisplayMetrics = new DisplayMetrics();
            d.getRealMetrics(realDisplayMetrics);

            int realHeight = realDisplayMetrics.heightPixels;
            int realWidth = realDisplayMetrics.widthPixels;

            DisplayMetrics displayMetrics = new DisplayMetrics();
            d.getMetrics(displayMetrics);

            int displayHeight = displayMetrics.heightPixels;
            int displayWidth = displayMetrics.widthPixels;

            boolean hasSoftwareKeys = (realWidth - displayWidth) > 0
                    || (realHeight - displayHeight) > 0;

            if (!hasSoftwareKeys) {
                return;
            }
        }
        /**
         * End of delicious copy-paste code
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.getWindow().getAttributes().flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;

            if (bottomBar.useTopOffset()) {
                int offset;
                int statusBarResource = res
                        .getIdentifier("status_bar_height", "dimen", "android");

                if (statusBarResource > 0) {
                    offset = res.getDimensionPixelSize(statusBarResource);
                } else {
                    offset = MiscUtils.dpToPixel(activity, 25);
                }

                if (!bottomBar.useOnlyStatusbarOffset()) {
                    TypedValue tv = new TypedValue();
                    if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                        offset += TypedValue.complexToDimensionPixelSize(tv.data,
                                res.getDisplayMetrics());
                    } else {
                        offset += MiscUtils.dpToPixel(activity, 56);
                    }
                }

                bottomBar.getUserContainer().setPadding(0, offset, 0, 0);
            }

            final View outerContainer = bottomBar.getOuterContainer();
            final int navBarHeightCopy = navBarHeight;
            bottomBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    bottomBar.shyHeightAlreadyCalculated();

                    int newHeight = outerContainer.getHeight() + navBarHeightCopy;
                    outerContainer.getLayoutParams().height = newHeight;

                    if (bottomBar.isShy()) {
                        int defaultOffset = bottomBar.useExtraOffset() ? navBarHeightCopy : 0;
                        bottomBar.setTranslationY(defaultOffset);
                        ((CoordinatorLayout.LayoutParams) bottomBar.getLayoutParams())
                                .setBehavior(new BottomNavigationBehavior(newHeight, defaultOffset));
                    }

                    ViewTreeObserver obs = outerContainer.getViewTreeObserver();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        obs.removeOnGlobalLayoutListener(this);
                    } else {
                        obs.removeGlobalOnLayoutListener(this);
                    }
                }
            });
        }
    }
}
