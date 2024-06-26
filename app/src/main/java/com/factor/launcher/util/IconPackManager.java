package com.factor.launcher.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.*;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class IconPackManager
{
    private Context mContext;

    public void setContext (Context c)
    {
        mContext = c;
    }

    public class IconPack
    {
        public String packageName;
        public String name;

        private boolean mLoaded = false;
        private final HashMap<String, String> mPackagesDrawables = new HashMap<>();

        private final List<Bitmap> mBackImages = new ArrayList<>();
        private Bitmap mMaskImage = null;
        private Bitmap mFrontImage = null;
        private float mFactor = 1.0f;
        private int totalIcons;

        Resources iconPackRes = null;

        public void load()
        {
            // load appfilter.xml from the icon pack package
            PackageManager pm = mContext.getPackageManager();
            try
            {
                XmlPullParser xpp = null;

                iconPackRes = pm.getResourcesForApplication(packageName);
                int appFilterId = iconPackRes.getIdentifier("appfilter", "xml", packageName);
                if (appFilterId > 0)
                {
                    xpp = iconPackRes.getXml(appFilterId);
                }
                else
                {
                    // no resource found, try to open it from assests folder
                    try
                    {
                        InputStream appFilterStream = iconPackRes.getAssets().open("appfilter.xml");

                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        xpp = factory.newPullParser();
                        xpp.setInput(appFilterStream, "utf-8");
                    }
                    catch (IOException e1)
                    {
                        //Ln.d("No appfilter.xml file");
                    }
                }

                if (xpp != null)
                {
                    int eventType = xpp.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT)
                    {
                        if(eventType == XmlPullParser.START_TAG)
                        {
                            if (xpp.getName().equals("iconback"))
                            {
                                for(int i=0; i<xpp.getAttributeCount(); i++)
                                {
                                    if (xpp.getAttributeName(i).startsWith("img"))
                                    {
                                        String drawableName = xpp.getAttributeValue(i);
                                        Bitmap iconback = loadBitmap(drawableName);
                                        if (iconback != null)
                                            mBackImages.add(iconback);
                                    }
                                }
                            }
                            else if (xpp.getName().equals("iconmask"))
                            {
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                                {
                                    String drawableName = xpp.getAttributeValue(0);
                                    mMaskImage = loadBitmap(drawableName);
                                }
                            }
                            else if (xpp.getName().equals("iconupon"))
                            {
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                                {
                                    String drawableName = xpp.getAttributeValue(0);
                                    mFrontImage = loadBitmap(drawableName);
                                }
                            }
                            else if (xpp.getName().equals("scale"))
                            {
                                // mFactor
                                if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor"))
                                {
                                    mFactor = Float.parseFloat(xpp.getAttributeValue(0));
                                }
                            }
                            else if (xpp.getName().equals("item"))
                            {
                                String componentName = null;
                                String drawableName = null;

                                for(int i=0; i<xpp.getAttributeCount(); i++)
                                {
                                    if (xpp.getAttributeName(i).equals("component"))
                                    {
                                        componentName = xpp.getAttributeValue(i);
                                    }
                                    else if (xpp.getAttributeName(i).equals("drawable"))
                                    {
                                        drawableName = xpp.getAttributeValue(i);
                                    }
                                }
                                if (!mPackagesDrawables.containsKey(componentName)) {
                                    mPackagesDrawables.put(componentName, drawableName);
                                    totalIcons = totalIcons + 1;
                                }
                            }
                        }
                        eventType = xpp.next();
                    }
                }
                mLoaded = true;
            }
            catch (PackageManager.NameNotFoundException e)
            {
                //Ln.d("Cannot load icon pack");
            }
            catch (XmlPullParserException e)
            {
                //Ln.d("Cannot parse icon pack appfilter.xml");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Bitmap loadBitmap(String drawableName)
        {
            int id = iconPackRes.getIdentifier(drawableName, "drawable", packageName);
            if (id > 0)
            {
                Drawable bitmap = iconPackRes.getDrawable(id, null);
                if (bitmap instanceof BitmapDrawable)
                    return ((BitmapDrawable)bitmap).getBitmap();
            }
            return null;
        }

        private Drawable loadDrawable(String drawableName)
        {
            int id = iconPackRes.getIdentifier(drawableName, "drawable", packageName);
            if (id > 0)
            {
                return iconPackRes.getDrawable(id, null);
            }
            return null;
        }

        public Drawable getDrawableIconForPackage(String appPackageName, Drawable defaultDrawable) {
            if (!mLoaded)
                load();

            PackageManager pm = mContext.getPackageManager();

            Intent launchIntent = pm.getLaunchIntentForPackage(appPackageName);

            String componentName = null;

            if (launchIntent != null) componentName = pm.getLaunchIntentForPackage(appPackageName).getComponent().toString();

            String drawable = mPackagesDrawables.get(componentName);

            if (drawable != null)
            {
                return loadDrawable(drawable);
            }

            else
            {
                // try to get a resource with the component filename
                if (componentName != null)
                {
                    int start = componentName.indexOf("{")+1;
                    int end = componentName.indexOf("}",  start);
                    if (end > start)
                    {
                        drawable = componentName.substring(start,end).toLowerCase(Locale.getDefault()).replace(".","_").replace("/", "_");
                        if (iconPackRes.getIdentifier(drawable, "drawable", packageName) > 0)
                            return loadDrawable(drawable);
                    }
                }
            }
            return defaultDrawable;
        }


        @SuppressWarnings("unused")
        public Bitmap getIconForPackage(String appPackageName, Bitmap defaultBitmap)
        {
            if (!mLoaded)
                load();

            PackageManager pm = mContext.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(appPackageName);
            String componentName = null;
            if (launchIntent != null)
                componentName = pm.getLaunchIntentForPackage(appPackageName).getComponent().toString();
            String drawable = mPackagesDrawables.get(componentName);
            if (drawable != null)
            {
                Bitmap BMP = loadBitmap(drawable);
                if (BMP == null) {
                    return generateBitmap(defaultBitmap);
                } else {
                    return BMP;
                }
            }
            else
            {
                // try to get a resource with the component filename
                if (componentName != null)
                {
                    int start = componentName.indexOf("{")+1;
                    int end = componentName.indexOf("}",  start);
                    if (end > start)
                    {
                        drawable = componentName.substring(start,end).toLowerCase(Locale.getDefault()).replace(".","_").replace("/", "_");
                        if (iconPackRes.getIdentifier(drawable, "drawable", packageName) > 0)
                            return loadBitmap(drawable);
                    }
                }
            }
            return generateBitmap(defaultBitmap);
        }

        @SuppressWarnings("unused")
        public int getTotalIcons() {
            return totalIcons;
        }


        private Bitmap generateBitmap(Bitmap defaultBitmap)
        {
            // if no support images in the icon pack return the bitmap itself
            if (mBackImages.isEmpty()) return defaultBitmap;

            Random r = new Random();
            int backImageInd = r.nextInt(mBackImages.size());
            Bitmap backImage = mBackImages.get(backImageInd);
            int w = backImage.getWidth();
            int h = backImage.getHeight();

            // create a bitmap for the result
            Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas mCanvas = new Canvas(result);

            // draw the background first
            mCanvas.drawBitmap(backImage, 0, 0, null);

            // create a mutable mask bitmap with the same mask
            Bitmap scaledBitmap;
            if (defaultBitmap.getWidth() > w || defaultBitmap.getHeight()> h) {
                scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap, (int)(w * mFactor), (int)(h * mFactor), false);
            } else {
                scaledBitmap = Bitmap.createBitmap(defaultBitmap);
            }

            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            if (mMaskImage != null)
            {
                // draw the scaled bitmap with mask
                maskCanvas.drawBitmap(mMaskImage,0, 0, new Paint());

                // paint the bitmap with mask into the result
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                mCanvas.drawBitmap(scaledBitmap, ((float)(w - scaledBitmap.getWidth()))/2, ((float)(h - scaledBitmap.getHeight()))/2, null);
                mCanvas.drawBitmap(mutableMask, 0, 0, paint);
                paint.setXfermode(null);
            }
            else // draw the scaled bitmap with the back image as mask
            {
                maskCanvas.drawBitmap(backImage,0, 0, new Paint());

                // paint the bitmap with mask into the result
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                mCanvas.drawBitmap(scaledBitmap, ((float)(w - scaledBitmap.getWidth()))/2, ((float)(h - scaledBitmap.getHeight()))/2, null);
                mCanvas.drawBitmap(mutableMask, 0, 0, paint);
                paint.setXfermode(null);

            }

            // paint the front
            if (mFrontImage != null)
            {
                mCanvas.drawBitmap(mFrontImage, 0, 0, null);
            }

            // store the bitmap in cache
//            BitmapCache.getInstance(mContext).putBitmap(key, result);

            // return it
            return result;
        }

        @Override
        public boolean equals(@Nullable Object obj)
        {
            if (obj instanceof IconPack)
            {
                return ((IconPack) obj).packageName.equals(this.packageName);
            }
            else return false;
        }
    }

    private ArrayList<IconPack> iconPacks = null;

    public ArrayList<IconPack> getAvailableIconPacks(boolean forceReload)
    {
        if (iconPacks == null || forceReload)
        {
            iconPacks = new ArrayList<>();

            // find apps with intent-filter "com.gau.go.launcherex.theme" and return build the HashMap
            PackageManager pm = mContext.getPackageManager();

            List<ResolveInfo> adwLauncherThemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);
            List<ResolveInfo> goLauncherThemes = pm.queryIntentActivities(new Intent("com.gau.go.launcherex.theme"), PackageManager.GET_META_DATA);

            // merge those lists
            List<ResolveInfo> resolveInfo = new ArrayList<>(adwLauncherThemes);
            resolveInfo.addAll(goLauncherThemes);

            for(ResolveInfo ri  : resolveInfo)
            {
                IconPack ip = new IconPack();
                ip.packageName = ri.activityInfo.packageName;

                ApplicationInfo ai;
                try
                {
                    ai = pm.getApplicationInfo(ip.packageName, PackageManager.GET_META_DATA);
                    ip.name  = mContext.getPackageManager().getApplicationLabel(ai).toString();
                    if (!iconPacks.contains(ip))
                        iconPacks.add(ip);
                }
                catch (PackageManager.NameNotFoundException e)
                {
                    // shouldn't happen
                    e.printStackTrace();
                }
            }
        }
        return iconPacks;
    }

    public IconPack getIconPackWithName(String packageName)
    {
        PackageManager pm = mContext.getPackageManager();
        IconPack targetPack = new IconPack();
        targetPack.packageName = packageName;
        if (iconPacks.contains(targetPack))
        {
            ApplicationInfo ai;
            try
            {
                ai = pm.getApplicationInfo(targetPack.packageName, PackageManager.GET_META_DATA);
                targetPack.name  = mContext.getPackageManager().getApplicationLabel(ai).toString();
                return targetPack;
            }
            catch (PackageManager.NameNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }
}