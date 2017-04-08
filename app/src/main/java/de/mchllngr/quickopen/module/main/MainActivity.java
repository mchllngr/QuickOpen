package de.mchllngr.quickopen.module.main;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.dgreenhalgh.android.simpleitemdecoration.linear.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mchllngr.quickopen.R;
import de.mchllngr.quickopen.base.BaseActivity;
import de.mchllngr.quickopen.model.ApplicationModel;
import de.mchllngr.quickopen.module.about.AboutActivity;
import de.mchllngr.quickopen.module.settings.SettingsActivity;
import de.mchllngr.quickopen.service.NotificationService;

/**
 * {@link android.app.Activity} for handling the selection of applications.
 *
 * @author Michael Langer (<a href="https://github.com/mchllngr" target="_blank">GitHub</a>)
 */
public class MainActivity extends BaseActivity<MainView, MainPresenter>
        implements MainView, MaterialSimpleListAdapter.Callback, MainAdapter.StartDragListener {

    /**
     * {@link android.support.design.widget.CoordinatorLayout} from the layout for showing the
     * {@link Snackbar}.
     *
     * @see Snackbar
     */
    @BindView(R.id.coordinator_layout) CoordinatorLayout coordinatorLayout;
    /**
     * {@link Toolbar} for this {@link android.app.Activity}.
     */
    @BindView(R.id.toolbar) Toolbar toolbar;
    /**
     * {@link android.support.v7.widget.RecyclerView} for showing list of items.
     */
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    /**
     * {@link android.support.design.widget.FloatingActionButton} for adding items.
     */
    @BindView(R.id.fab) FloatingActionButton fab;
    /**
     * Represents the red background behind a swipeable item.
     */
    @BindView(R.id.swipe_background) FrameLayout swipeBackground;

    /**
     * {@link MainAdapter} for updating shown items in {@code recyclerView}.
     */
    private MainAdapter adapter;
    /**
     * {@link MaterialDialog} for showing the installed application-list.
     */
    private MaterialDialog applicationDialog;
    /**
     * {@link MaterialDialog} for showing the loading-process for the list of installed applications.
     */
    private MaterialDialog progressDialog;

    /**
     * {@link Snackbar} for showing the undo-remove-button.
     */
    private Snackbar snackbar;
    /**
     * {@link ItemTouchHelper} for moving and swiping in {@link RecyclerView}.
     */
    private ItemTouchHelper itemTouchHelper;
    /**
     * Indicates whether the Reorder-Mode is enabled or disabled.
     */
    private boolean reorderMode;

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        initRecyclerView();

        startNotificationService();

        fab.setOnClickListener(view -> getPresenter().openApplicationList());
    }

    /**
     * Initialises the {@code recyclerView}.
     */
    private void initRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MainAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        recyclerView.addItemDecoration(new DividerItemDecoration(
                ContextCompat.getDrawable(this, R.drawable.recycler_view_item_divider)
        ));

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return !reorderMode;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                moveItem(
                        viewHolder.getAdapterPosition(),
                        target.getAdapterPosition()
                );

                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                getPresenter().removeItem(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!reorderMode) {
                    // set the red background one swiped item
                    swipeBackground.setY(viewHolder.itemView.getTop());
                    if (isCurrentlyActive) {
                        swipeBackground.setVisibility(View.VISIBLE);
                    } else {
                        swipeBackground.setVisibility(View.GONE);
                    }
                } else
                    swipeBackground.setVisibility(View.GONE);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        getPresenter().loadItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(!reorderMode); // reorder
        menu.getItem(1).setVisible(!reorderMode); // about
        menu.getItem(2).setVisible(!reorderMode); // settings
        menu.getItem(3).setVisible(reorderMode); // reorder_cancel
        menu.getItem(4).setVisible(reorderMode); // reorder_accept

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reorder:
                if (adapter != null)
                    getPresenter().onReorderIconClick(adapter.getItems());
                return true;
            case R.id.about:
                AboutActivity.start(this);
                return true;
            case R.id.settings:
                SettingsActivity.start(this);
                return true;
            case R.id.reorder_cancel:
                getPresenter().onReorderCancelIconClick();
                return true;
            case R.id.reorder_accept:
                if (adapter != null)
                    getPresenter().onReorderAcceptIconClick(adapter.getItems());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    /**
     * Starts the {@link NotificationService}.
     */
    private void startNotificationService() {
        startService(new Intent(this, NotificationService.class));
    }

    @Override
    public void onMaterialListItemSelected(int index, MaterialSimpleListItem item) {
        getPresenter().onApplicationSelected(index);

        if (applicationDialog != null)
            applicationDialog.dismiss();
    }

    @Override
    public void showApplicationListDialog(MaterialSimpleListAdapter adapter) {
        applicationDialog = new MaterialDialog.Builder(this)
                .title(R.string.application_list_dialog_title)
                .adapter(adapter, null)
                .show();
    }

    @Override
    public MaterialSimpleListAdapter.Callback getApplicationChooserCallback() {
        return this;
    }

    @Override
    public void showProgressDialog() {
        hideProgressDialog();

        progressDialog = new MaterialDialog.Builder(this)
                .content(R.string.progress_dialog_please_wait)
                .progress(true, 0)
                .show();
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    @Override
    public void setReorderMode(boolean enable) {
        reorderMode = enable;
        invalidateOptionsMenu();

        if (adapter != null)
            adapter.setReorderMode(enable);
    }

    @Override
    public void updateItems(List<ApplicationModel> items) {
        if (adapter != null)
            adapter.updateItems(items);
    }

    @Override
    public void addItem(int position, ApplicationModel applicationModel) {
        if (adapter != null)
            adapter.add(position, applicationModel);
    }

    @Override
    public void removeItem(int position) {
        if (adapter != null)
            adapter.remove(adapter.get(position));
    }

    @Override
    public void moveItem(int fromPosition, int toPosition) {
        if (adapter != null)
            adapter.move(fromPosition, toPosition);
    }

    @Override
    public void showAddItemsButton() {
        fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideAddItemsButton() {
        fab.setVisibility(View.GONE);
    }

    @Override
    public void showUndoButton() {
        hideUndoButton();

        snackbar = Snackbar
                .make(coordinatorLayout, R.string.snackbar_undo_remove, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_undo_remove_action, view -> {
                    getPresenter().undoRemove();
                    snackbar.dismiss();
                });
        snackbar.getView().setBackgroundResource(R.color.snackbar_background_color);

        snackbar.show();
    }

    @Override
    public void hideUndoButton() {
        if (snackbar != null)
            snackbar.dismiss();
    }

    @Override
    public void onOpenApplicationListError() {
        Log.d("DEBUG_TAG", "MainActivity#onOpenApplicationListError()"); // FIXME delete
        // TODO show error msg
    }

    @Override
    public void showMaxItemsError() {
        Log.d("DEBUG_TAG", "MainActivity#showMaxItemsError()"); // FIXME delete
        // TODO show error msg
    }

    @Override
    public void onStartDrag(MainAdapter.ViewHolder viewHolder) {
        if (itemTouchHelper != null)
            itemTouchHelper.startDrag(viewHolder);
    }
}