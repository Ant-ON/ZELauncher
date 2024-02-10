package ru.zdevs.zelauncher;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ApplicationList
{
    private static ArrayList<ActivityInfo> sAppList;

    @NonNull
    public static ArrayList<ActivityInfo> get(@NonNull PackageManager pm)
    {
        if (sAppList != null)
            return sAppList;

        final Intent template = new Intent(Intent.ACTION_MAIN);
        template.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> list = pm.queryIntentActivities(template, PackageManager.MATCH_ALL);
        sAppList = new ArrayList<>(list.size());
        for (ResolveInfo ri : list)
        {
            if (!"ru.zdevs.zelauncher".equals(ri.activityInfo.packageName))
                sAppList.add(ri.activityInfo);
        }
        sort();
        return sAppList;
    }

    public static void add(@NonNull PackageManager pm, @NonNull String pkg)
    {
        if (sAppList == null)
            return;

        final Intent template = new Intent(Intent.ACTION_MAIN);
        template.addCategory(Intent.CATEGORY_LAUNCHER);
        template.setPackage(pkg);

        final List<ResolveInfo> list = pm.queryIntentActivities(template, PackageManager.MATCH_ALL);
        for (ResolveInfo ri : list)
            sAppList.add(ri.activityInfo);

        sort();
    }

    public static void delete(@NonNull String pkg)
    {
        if (sAppList == null)
            return;

        for (ActivityInfo ai : sAppList)
            if (pkg.equals(ai.packageName))
            {
                sAppList.remove(ai);
                break;
            }
    }

    public static void clear()
    {
        if (sAppList != null)
            sAppList.clear();
    }

    @NonNull
    public static Intent getLaunchIntent(@NonNull ActivityInfo ai)
    {
        final Intent template = new Intent(Intent.ACTION_MAIN);
        template.addCategory(Intent.CATEGORY_LAUNCHER);
        template.setPackage(ai.packageName);
        template.setClassName(ai.packageName, ai.name);
        return template;
    }

    private static void sort()
    {
        sAppList.sort(new Comparator<ActivityInfo>() {
            public int compare(ActivityInfo ai1, ActivityInfo ai2)
            {
                return ai1.name.compareToIgnoreCase(ai2.name);
            }
        });
    }
}
