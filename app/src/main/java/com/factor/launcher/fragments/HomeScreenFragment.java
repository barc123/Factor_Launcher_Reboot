package com.factor.launcher.fragments;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager.widget.ViewPager;
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.factor.launcher.R;
import com.factor.launcher.databinding.FragmentHomeScreenBinding;
import com.factor.launcher.managers.AppListManager;
import com.factor.launcher.receivers.AppActionReceiver;
import com.factor.launcher.receivers.PackageActionsReceiver;
import com.factor.launcher.util.OnBackPressedCallBack;
import eightbitlab.com.blurview.RenderScriptBlur;


public class HomeScreenFragment extends Fragment implements OnBackPressedCallBack
{
    private FragmentHomeScreenBinding binding;

    private WallpaperManager wm;


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


    private void initializeComponents()
    {
        binding.image.setImageDrawable(wm.getDrawable());
        AppListManager appListManager = new AppListManager(this.requireActivity(), binding.backgroundHost);
        PackageActionsReceiver packageActionsReceiver = new PackageActionsReceiver(appListManager);

        new AppActionReceiver(appListManager);

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");

        requireActivity().registerReceiver(packageActionsReceiver, filter);

        //home pager
        binding.homePager.addView(binding.tilesPage, 0);
        binding.homePager.addView(binding.drawerPage, 1);

        //app drawer
        binding.appsList.setLayoutManager(new LinearLayoutManager(this.getContext()));
        binding.appsList.setAdapter(appListManager.adapter);

        //tile list
        ChipsLayoutManager chips = ChipsLayoutManager.newBuilder(requireContext())
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .setChildGravity(Gravity.CENTER)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .setMaxViewsInRow(2)
                .setScrollingEnabled(true)
                .build();

        binding.tilesList.setLayoutManager(chips);
        binding.tilesList.setAdapter(appListManager.getFactorManager().adapter);




        int paddingTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 105, getResources().getDisplayMetrics());
        int paddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        int paddingBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());


        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        binding.tilesList.setPadding(paddingLeft, paddingTop, width/5, paddingBottom);


        binding.blur.setupWith(binding.backgroundHost)
                .setFrameClearDrawable(wm.getDrawable())
                .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                .setBlurRadius(15f)
                .setBlurAutoUpdate(false)
                .setHasFixedTransformationMatrix(true);






        binding.homePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                float xOffset = position + positionOffset;
                binding.dim.setAlpha(xOffset);
                binding.arrowButton.setRotation(+180 * xOffset - 180);
                binding.blur.setAlpha(xOffset / 0.5f);

                binding.searchBase.setTranslationY(-500f + 500*xOffset);
            }

            @Override
            public void onPageSelected(int position)
            {
                if (position == 0)
                {
                    binding.arrowButton.setRotation(180);
                    binding.blur.setAlpha(0f);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
            }
        });


        binding.searchBlur.setupWith(binding.rootContent)
                .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                .setBlurRadius(20f)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(false);

        binding.searchBase.setTranslationY(-500f);


        binding.searchView.setIconifiedByDefault(false);

        binding.searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                appListManager.filter(newText);
                return false;
            }
        });
    }

    @Override
    public boolean onBackPressed()
    {
        if (binding.homePager.getCurrentItem() == 1)
        {
            binding.homePager.setCurrentItem(0, true);
            return true;
        }
        else
            return false;
    }
}