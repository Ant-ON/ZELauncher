package ru.zdevs.zelauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MenuActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
{
    public static final String EXTRA_SET_APP = "SET_APP";
    public static final String EXTRA_ACTIVITY = "ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        final GridView gv = findViewById(R.id.gv);
        gv.setAdapter(new ApplicationAdapter(this));
        gv.setOnItemClickListener(this);
        gv.setOnItemLongClickListener(this);

        final View ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndRemoveTask();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
    {
        final ActivityInfo ai = (ActivityInfo) adapter.getItemAtPosition(position);

        final Intent intent = getIntent();
        if (intent.getIntExtra(EXTRA_SET_APP, -1) != -1)
        {
            intent.putExtra(EXTRA_ACTIVITY, ai.packageName);
            setResult(RESULT_OK, intent);
        } else
            startActivity(ApplicationList.getLaunchIntent(ai));
        finishAndRemoveTask();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id)
    {
        final ActivityInfo ai = (ActivityInfo) adapter.getItemAtPosition(position);

        final PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                final int id = item.getItemId();
                try
                {
                    final Intent intent;
                    if (id == R.id.uninstall)
                        intent = new Intent(Intent.ACTION_DELETE);
                    else
                        intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + ai.packageName));
                    startActivity(intent);
                } catch (ActivityNotFoundException E)
                {
                    Toast.makeText(MenuActivity.this, E.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        popup.inflate(R.menu.context_app);
        popup.show();
        return true;
    }

    public static class ApplicationAdapter extends BaseAdapter
    {
        @NonNull
        private final LayoutInflater mInflater;
        @NonNull
        private final PackageManager mPM;
        @NonNull
        private final ArrayList<ActivityInfo> mList;

        public ApplicationAdapter(@NonNull Context ctx)
        {
            mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPM = ctx.getPackageManager();
            mList = new ArrayList<>(ApplicationList.get(mPM));
        }

        @Override
        public int getCount()
        {
            return mList.size();
        }

        @Override
        public ActivityInfo getItem(int position)
        {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent)
        {
            final ActivityInfo item = getItem(position);

            if (view == null)
            {
                view = mInflater.inflate(R.layout.item_application, parent, false);
            }

            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(item.loadIcon(mPM));
            ((TextView) view.findViewById(R.id.name)).setText(item.loadLabel(mPM));

            return view;
        }
    }
}
