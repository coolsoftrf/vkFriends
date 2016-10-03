package ru.coolsoft.vkfriends;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
implements AppBarLayout.OnOffsetChangedListener
, NavigationView.OnNavigationItemSelectedListener{

    private FrameLayout mFl;
    private TextView mContactNameLeft;
    private TextView mContactNameRight;
    private ImageView mContactImageLeft;
    private ImageView mContactImageRight;
    private Space mSpace1;
    private Space mSpace2;

    private int mTextSizeStart;
    private int mTextSizeEnd;
    private int mAppBarHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initParams();

        ImageView anchor = (ImageView) findViewById(R.id.padding);
        if (anchor!= null) {
            ViewGroup.LayoutParams lp = anchor.getLayoutParams();
            lp.height = mAppBarHeight * 3;
        }

        AppBarLayout appBar = (AppBarLayout) findViewById(R.id.app_bar);
        if (appBar != null) {
            appBar.addOnOffsetChangedListener(this);
        }

        NavigationView nav = (NavigationView) findViewById(R.id.navigation);
        if(nav != null) {
            nav.setNavigationItemSelectedListener(this);
        }
        mFl = (FrameLayout) findViewById(R.id.title);

        mContactNameLeft = (TextView) findViewById(R.id.name1);
        mContactNameRight = (TextView) findViewById(R.id.name2);
        mContactImageLeft = (ImageView) findViewById(R.id.avatar1);
        mContactImageRight = (ImageView) findViewById(R.id.avatar2);
        mSpace1 = (Space) findViewById(R.id.spaceAvatar1);
        mSpace2 = (Space) findViewById(R.id.spaceAvatar2);
    }

    private void initParams(){
        TypedArray attr = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        mAppBarHeight = attr.getDimensionPixelSize(0, 0);
        attr.recycle();

        mTextSizeStart = getResources().getDimensionPixelSize(R.dimen.text_size);
        mTextSizeEnd = getResources().getDimensionPixelSize(R.dimen.text_size_small);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset/*negative*/) {
        final int maxScroll = appBarLayout.getTotalScrollRange();

        ViewGroup.LayoutParams lp = mFl.getLayoutParams();
        lp.height = maxScroll + mAppBarHeight + verticalOffset;
        mFl.setLayoutParams(lp);

        final float fraction = (float)verticalOffset / maxScroll; //negative
        final int halfWidth = appBarLayout.getMeasuredWidth() / 2;

        final int nameWidth = halfWidth +  (int)(mAppBarHeight * fraction);
        mContactNameLeft.setMaxWidth(nameWidth);
        mContactNameRight.setMaxWidth(nameWidth);

        final float textSize = fraction * (mTextSizeStart - mTextSizeEnd) + mTextSizeStart;
        mContactNameLeft.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mContactNameRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);


        final int size = (int)((2 + fraction) * mAppBarHeight);
        lp = mContactImageLeft.getLayoutParams();
        lp.height = lp.width = size;
        mContactImageLeft.setLayoutParams(lp);

        lp = mContactImageRight.getLayoutParams();
        lp.height = lp.width = size;
        mContactImageRight.setLayoutParams(lp);


        final int space = (int)(-fraction * (halfWidth - mAppBarHeight));
        lp = mSpace1.getLayoutParams();
        lp.width = space;
        mSpace1.setLayoutParams(lp);

        lp = mSpace2.getLayoutParams();
        lp.width = space;
        mSpace2.setLayoutParams(lp);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle nav bar item clicks here.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
