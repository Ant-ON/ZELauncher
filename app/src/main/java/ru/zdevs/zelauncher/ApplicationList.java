package ru.zdevs.zelauncher;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ApplicationList
{
    private static ArrayList<ActivityInfo> sAppList;

    @NonNull
    public static ArrayList<ActivityInfo> get(PackageManager pm)
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
        return sAppList;
    }

    @NonNull
    public static Intent getLaunchIntent(ActivityInfo ai)
    {
        final Intent template = new Intent(Intent.ACTION_MAIN);
        template.addCategory(Intent.CATEGORY_LAUNCHER);
        template.setPackage(ai.packageName);
        template.setClassName(ai.packageName, ai.name);
        return template;
    }
}
