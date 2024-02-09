package ru.zdevs.zelauncher;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DesktopActivity extends Activity implements View.OnClickListener, View.OnLongClickListener
{
    private static final int REQ_SET_APP = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_desktop);

        ImageView iv = findViewById(R.id.ivThumb);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                final Intent intent = new Intent("org.coolreader.READ");
                intent.putExtra("FILE_TO_OPEN", (String)v.getTag());
                intent.putExtra("EXIT_BY_BACK", true);
                startActivity(intent);
            }
        });

        final TableLayout tl = findViewById(R.id.tlMenu);
        for (int i=0; i<3; i++)
        {
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 0, 1));
            for (int j=0; j<3; j++)
            {
                final ImageView v = new ImageView(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableLayout.LayoutParams.MATCH_PARENT, 1);
                v.setLayoutParams(lp);
                //v.setImageDrawable(getDrawable(R.drawable.ic_add));
                v.setBackground(getDrawable(R.drawable.bg_button));
                v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
                v.setTag(R.id.tlMenu, i * 3 + j);
                tr.addView(v);
                registerForContextMenu(v);
            }
            tl.addView(tr);
        }

        new LoadApplicationButtons().execute();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        updateCurrentBook();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQ_SET_APP && resultCode == RESULT_OK)
        {
            new SetApplicationButtons(data.getIntExtra(MenuActivity.EXTRA_SET_APP, 0), data.getStringExtra(MenuActivity.EXTRA_ACTIVITY)).execute();
        }
    }

    private void updateCurrentBook()
    {
        final ImageView iv = findViewById(R.id.ivThumb);
        final Uri u = new Uri.Builder().authority("org.coolreader").path("reading/1/" + iv.getMeasuredWidth() + "/" + iv.getMeasuredHeight()).build();

        final ContentResolver cr = getContentResolver();
        ContentProviderClient client = null;
        try
        {
            client = cr.acquireUnstableContentProviderClient(u.getAuthority());
            Cursor c = client.query(u, null, null, null, null);
            if (c != null)
            {
                if (c.moveToNext())
                {
                    ((TextView) findViewById(R.id.tvTitle)).setText(c.getString(0));
                    ((TextView) findViewById(R.id.tvAutor)).setText(c.getString(1));
                    ((TextView) findViewById(R.id.tvProgress)).setText(c.getString(2));
                    final byte[] thumb = c.getBlob(3);
                    if (thumb != null) {
                        final Bitmap bmp = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
                        iv.setImageBitmap(bmp);
                    } else
                        iv.setImageDrawable(getDrawable(R.drawable.bg_button));
                    iv.setTag(c.getString(4));
                }
                c.close();
            }
        } catch (Exception E)
        {
            E.printStackTrace();
        } finally
        {
            if (client != null)
                client.close();
        }
    }

    @Override
    public void onClick(View v)
    {
        Object tag = v.getTag();
        if (tag instanceof ActivityInfo)
            startActivity(ApplicationList.getLaunchIntent((ActivityInfo) tag));
        else if (tag instanceof String)
            startActivity(new Intent(this, MenuActivity.class));
    }

    @Override
    public boolean onLongClick(View v)
    {
        final Integer tag = (Integer) v.getTag(R.id.tlMenu);
        final PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                final int id = item.getItemId();
                if (id == R.id.set_app)
                {
                    final Intent intent = new Intent(DesktopActivity.this, MenuActivity.class);
                    intent.putExtra(MenuActivity.EXTRA_SET_APP, (int) tag);
                    startActivityForResult(intent, REQ_SET_APP);
                } else
                    new SetApplicationButtons(tag, id == R.id.clear ? "" : "menu").execute();
                return true;
            }
        });
        popup.inflate(R.menu.context_button);
        popup.show();
        return true;
    }

    private class LoadApplicationButtons extends AsyncTask<Void, Void, ApplicationButton[]>
    {
        @Override
        protected ApplicationButton[] doInBackground(Void... v)
        {
            final ApplicationButton[] btns = new ApplicationButton[9];

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(DesktopActivity.this);

            final PackageManager pm = getPackageManager();
            final ArrayList<ActivityInfo> apps = ApplicationList.get(pm);
            for (int i=0; i<9; i++)
            {
                String app = settings.getString("app" + i, null);
                if (app == null)
                {
                    if (i == 6)
                        app = "ru.zdevs.zarchiver.pro";
                    else if (i == 7)
                        app = "org.coolreader";
                    else if (i == 8)
                        app = "menu";
                }

                if ("menu".equals(app))
                {
                    btns[i] = new ApplicationButton(getDrawable(R.drawable.ic_menu));
                } else if (app != null && !app.isEmpty())
                {
                    for (ActivityInfo ai : apps)
                        if (app.equals(ai.packageName))
                        {
                            btns[i] = new ApplicationButton(ai, ai.loadUnbadgedIcon(pm));
                            break;
                        }
                }
            }

            return btns;
        }

        @Override
        protected void onPostExecute(ApplicationButton[] result)
        {
            final TableLayout tl = findViewById(R.id.tlMenu);
            for (int i=0; i<3; i++)
            {
                final TableRow tr = (TableRow) tl.getChildAt(i);
                for (int j=0; j<3; j++)
                {
                    final ImageView iv = (ImageView) tr.getChildAt(j);

                    final ApplicationButton ab = result[i*3 + j];
                    if (ab != null)
                    {
                        iv.setImageDrawable(ab.drawable);
                        iv.setTag(ab.menu ? "menu" : ab.activity);
                    } else
                        iv.setImageDrawable(getDrawable(R.drawable.ic_add));
                }
            }
        }
    }

    private class SetApplicationButtons extends AsyncTask<Void, Void, Boolean> {
        private final int mButton;
        @Nullable
        private final String mPackageName;

        SetApplicationButtons(int btn, @Nullable String packageName)
        {
            mButton = btn;
            mPackageName = packageName;
        }

        @Override
        protected Boolean doInBackground(Void... v)
        {
            if (mPackageName == null)
                return false;

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(DesktopActivity.this);
            settings.edit().putString("app" + mButton, mPackageName).apply();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (result)
                new LoadApplicationButtons().execute();
        }
    }

    private static class ApplicationButton
    {
        final boolean menu;
        @Nullable
        final ActivityInfo activity;
        @Nullable
        final Drawable drawable;

        ApplicationButton(@NonNull ActivityInfo ai, @Nullable Drawable d)
        {
            menu = false;
            activity = ai;
            drawable = d;
        }

        ApplicationButton(@Nullable Drawable d)
        {
            menu = true;
            activity = null;
            drawable = d;
        }
    }
}