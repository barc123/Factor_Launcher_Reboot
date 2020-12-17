package com.factor.launcher.fragments;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.factor.launcher.R;
import com.factor.launcher.databinding.FragmentHomeScreenBinding;
import com.factor.launcher.managers.AppListManager;
import com.factor.launcher.models.UserApp;
import com.factor.launcher.receivers.AppActionReceiver;
import com.factor.launcher.receivers.NotificationBroadcastReceiver;
import com.factor.launcher.receivers.PackageActionsReceiver;
import com.factor.launcher.util.Constants;
import com.factor.launcher.util.OnBackPressedCallBack;
import com.reddit.indicatorfastscroll.FastScrollItemIndicator;
import com.valkriaine.factor.bouncyRecyclerViewUtil.OnOverPullListener;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.util.Objects;


public class HomeScreenFragment extends Fragment implements OnBackPressedCallBack
{
    private FragmentHomeScreenBinding binding;

    private WallpaperManager wm;

    private AppListManager appListManager;

    private boolean isLiveWallpaper = false;

    private final float WIDGET_SCREEN_THRESHOLD = 0.3f;

    private boolean isWidgetMode = false;

    private WidgetFragment widgetFragment;

    public HomeScreenFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        wm = WallpaperManager.getInstance(requireContext());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_screen, container, false);
        initializeComponents();
        return binding.getRoot();
    }

    //handle back button press
    @Override
    public boolean onBackPressed()
    {
        if (binding.homePager.getCurrentItem() == 1)
        {
            if (appListManager.isDisplayingHidden())
            {
                binding.appsList.setAdapter(appListManager.setDisplayHidden(false));
                return true;
            }
            binding.appsList.scrollToPosition(0);
            binding.homePager.setCurrentItem(0, true);
            return true;
        }
        else if (binding.homePager.getCurrentItem() == 0)
        {
            if (isWidgetMode)
            {
                binding.widgetBlur.setBlurEnabled(false);
                binding.widgetBackground.animate().alpha(0).start();
                binding.tilesList.setAlpha(1);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .remove(widgetFragment)
                        .commit();

                isWidgetMode = false;
                return true;
            }
            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(requireContext())
            {
                @Override protected int getVerticalSnapPreference()
                {
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };
            smoothScroller.setTargetPosition(0);
            Objects.requireNonNull((ChipsLayoutManager)binding.tilesList.getLayoutManager())
                    .smoothScrollToPosition(binding.tilesList, new RecyclerView.State(), 0);
            return true;
        }
        else
            return true;
    }


    //initialize views and listeners
    private void initializeComponents()
    {
        //initialize resources
        //***************************************************************************************************************************************************
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        int paddingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 105, getResources().getDisplayMetrics());
        int paddingHorizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        int paddingBottom300 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        int paddingBottom150 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
        int paddingBottomOnSearch = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1000, getResources().getDisplayMetrics());
        int appListPaddingTop100 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());



        //initialize widget fragment
        //***************************************************************************************************************************************************
        widgetFragment = new WidgetFragment();



        //get system wallpaper
        //***************************************************************************************************************************************************
        checkLiveWallpaper();




        //initialize data manager
        //***************************************************************************************************************************************************
        appListManager = new AppListManager(this.requireActivity(), binding.backgroundHost, isLiveWallpaper);




        //register broadcast receivers
        //***************************************************************************************************************************************************
        new AppActionReceiver(appListManager, binding);
        PackageActionsReceiver packageActionsReceiver = new PackageActionsReceiver(appListManager);
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        requireActivity().registerReceiver(packageActionsReceiver, filter);

        IntentFilter notificationFilter = new IntentFilter();
        notificationFilter.addAction(Constants.NOTIFICATION_INTENT_ACTION_CLEAR);
        notificationFilter.addAction(Constants.NOTIFICATION_INTENT_ACTION_POST);
        requireActivity().registerReceiver(new NotificationBroadcastReceiver(appListManager), notificationFilter);




        //home pager
        //***************************************************************************************************************************************************
        binding.homePager.addView(binding.tilesPage, 0);
        binding.homePager.addView(binding.drawerPage, 1);




        //app drawer
        //***************************************************************************************************************************************************
        binding.appsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.appsList.setAdapter(appListManager.adapter);
        binding.homePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                float xOffset = position + positionOffset;
                binding.dim.setAlpha(xOffset);
                binding.arrowButton.setRotation(+180 * xOffset - 180);
                binding.blur.setAlpha(xOffset / 0.5f);
                binding.searchBase.setTranslationY(-500f + 500 * xOffset);
                binding.searchView.clearFocus();
                binding.appsList.setPadding(paddingHorizontal, appListPaddingTop100, paddingHorizontal, paddingBottom150);
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    binding.arrowButton.setRotation(180);
                    binding.blur.setAlpha(0f);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        binding.scrollBar.setupWithRecyclerView(
                binding.appsList,
                (position) ->
                {
                    UserApp item = appListManager.getUserApp(position);
                    if (item.getPackageName().isEmpty())
                        return new FastScrollItemIndicator.Text("");
                    char cap =  item.getLabelNew().toUpperCase().charAt(0);
                    String capString = item.getLabelNew().toUpperCase().substring(0, 1);
                    try
                    {
                        Integer.parseInt(String.valueOf(cap));
                        capString = "#";
                    }
                    catch (NumberFormatException ignored)
                    {
                        //todo: convert chinese to pinyin
                        //return new FastScrollItemIndicator.Icon("some drawable");
                    }

                    return new FastScrollItemIndicator.Text(capString);
                }
        );
        binding.thumb.setupWithFastScroller(binding.scrollBar);




        //tile list
        //***************************************************************************************************************************************************
        ChipsLayoutManager chips = ChipsLayoutManager.newBuilder(requireContext())
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .setChildGravity(Gravity.CENTER)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .setMaxViewsInRow(2)
                .setScrollingEnabled(true)
                .build();
        binding.tilesList.setLayoutManager(chips);
        binding.tilesList.setAdapter(appListManager.getFactorManager().adapter);
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        binding.tilesList.setPadding(paddingHorizontal, paddingTop, width / 5, paddingBottom300);




        //search bar
        //***************************************************************************************************************************************************
        binding.searchBase.setTranslationY(-500f);
        binding.searchView.setOnCloseListener(() -> {
            binding.appsList.setPadding(paddingHorizontal, appListPaddingTop100, paddingHorizontal, paddingBottom150);
            return false;
        });
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                binding.appsList.setPadding(paddingHorizontal, appListPaddingTop100, paddingHorizontal, paddingBottomOnSearch);
                String queryText = newText.toLowerCase().trim();
                appListManager.findPosition(binding.appsList, queryText);
                return true;
            }
        });
        binding.searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> binding.appsList.setPadding(paddingHorizontal, appListPaddingTop100, paddingHorizontal, paddingBottom150));




        //menu button
        //***************************************************************************************************************************************************
        binding.menuButton.setOnClickListener(view ->
        {
            boolean isDisplayingHidden = appListManager.isDisplayingHidden();

            PopupMenu popup = new PopupMenu(requireContext(), binding.menuButton);
            popup.getMenuInflater().inflate(R.menu.app_menu, popup.getMenu());

            MenuItem displayMode = popup.getMenu().getItem(0);
            MenuItem options = popup.getMenu().getItem(1);
            MenuItem wallpaperOption = popup.getMenu().getItem(2);

            //show hidden apps
            displayMode.setTitle(isDisplayingHidden ? "My apps" : "Hidden apps");
            displayMode.setOnMenuItemClickListener(item ->
            {
                binding.appsList.setAdapter(isDisplayingHidden?
                        appListManager.setDisplayHidden(false) : appListManager.setDisplayHidden(true));
                return true;
            });

            //launch settings
            options.setOnMenuItemClickListener(item ->
            {
                //todo: launch settings fragment
                return true;
            });
            popup.show();

            //change system wallpaper
            wallpaperOption.setOnMenuItemClickListener(item ->
            {
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, "Select Wallpaper"));
                return true;
            });
        });



        //handle tile list over-pull to open widget screen
        //***************************************************************************************************************************************************
        binding.tilesList.addOnOverPulledListener(new OnOverPullListener()
        {
            @Override
            public void onOverPulledTop(float v)
            {

                binding.widgetBlur.setBlurEnabled(true);
                binding.widgetBackground.setAlpha(binding.widgetBackground.getAlpha() + v);
                Log.d("widget", "alpha: " + binding.widgetBackground.getAlpha());
                if (binding.widgetBackground.getAlpha() >= WIDGET_SCREEN_THRESHOLD)
                {
                    binding.widgetBackground.animate().alpha(1).start();
                    binding.tilesList.animate().alpha(0).setDuration(100).start();
                    isWidgetMode = true;

                    requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .setReorderingAllowed(true)
                                .replace(R.id.widget_fragment_container, widgetFragment)
                                .addToBackStack(null)
                                .commit();
                }
            }

            @Override
            public void onOverPulledBottom(float v)
            {

            }

            @Override
            public void onRelease()
            {
                if (!isWidgetMode)
                {
                    binding.widgetBlur.setBlurEnabled(false);
                    binding.widgetBackground.animate().alpha(0).start();
                }
            }
        });
    }


    //setup wallpaper
    private void checkLiveWallpaper()
    {
        //todo: handle blur preferences

        //static wallpaper
        if (requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                wm.getWallpaperInfo() == null)
        {

            binding.image.setImageDrawable(wm.getDrawable());
            isLiveWallpaper = false;

            binding.image.setVisibility(View.VISIBLE);
            binding.blur.setVisibility(View.VISIBLE);
            binding.searchBase.setCardBackgroundColor(Color.TRANSPARENT);
            binding.searchBlur.setOverlayColor(Color.parseColor("#4DFFFFFF"));
            binding.searchBlur.setBlurEnabled(true);
            binding.blur.setupWith(binding.backgroundHost)
                    .setFrameClearDrawable(wm.getDrawable())
                    .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                    .setBlurRadius(15f)
                    .setBlurAutoUpdate(true)
                    .setHasFixedTransformationMatrix(true)
                    .setBlurEnabled(true);

            binding.searchBlur.setupWith(binding.rootContent)
                    .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                    .setBlurRadius(20f)
                    .setBlurAutoUpdate(true)
                    .setHasFixedTransformationMatrix(false)
                    .setBlurEnabled(true);

            binding.widgetBlur.setupWith(binding.backgroundHost)
                    .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                    .setBlurRadius(20f)
                    .setBlurAutoUpdate(true)
                    .setHasFixedTransformationMatrix(true)
                    .setBlurEnabled(true);
        }
        else //live wallpaper
        {
            binding.image.setVisibility(View.GONE);
            binding.blur.setVisibility(View.GONE);
            binding.searchBase.setCardBackgroundColor(Color.BLACK);
            binding.searchBlur.setOverlayColor(Color.TRANSPARENT);
            binding.searchBlur.setBlurEnabled(false);
            binding.blur.setBlurEnabled(false);
            binding.widgetBlur.setBlurEnabled(false);

            isLiveWallpaper = true;
        }
    }
}