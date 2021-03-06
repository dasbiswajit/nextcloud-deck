package it.niedermann.nextcloud.deck.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.api.IResponseCallback;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.model.full.FullStack;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util.WrappedLiveData;
import it.niedermann.nextcloud.deck.ui.board.BoardCreateDialogFragment;
import it.niedermann.nextcloud.deck.ui.helper.dnd.CrossTabDragAndDrop;
import it.niedermann.nextcloud.deck.ui.login.LoginDialogFragment;
import it.niedermann.nextcloud.deck.ui.stack.StackAdapter;
import it.niedermann.nextcloud.deck.ui.stack.StackCreateDialogFragment;
import it.niedermann.nextcloud.deck.ui.stack.StackFragment;
import it.niedermann.nextcloud.deck.util.ViewUtil;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MENU_ID_ABOUT = -1;
    private static final int MENU_ID_ADD_BOARD = -2;
    private static final int MENU_ID_ADD_ACCOUNT = -2;
    private static final int ACTIVITY_ABOUT = 1;
    private static final long NO_BOARDS = -1;

    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.navigationView)
    NavigationView navigationView;
    @BindView(R.id.stackLayout)
    TabLayout stackLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    private SharedPreferences sharedPreferences;
    private StackAdapter stackAdapter;
    private LoginDialogFragment loginDialogFragment;
    private SyncManager syncManager;

    private List<Account> accountsList = new ArrayList<>();
    private Account account;
    private boolean accountChooserActive = false;

    private List<Board> boardsList;
    private LiveData<List<Board>> boardsLiveData;
    private Observer<List<Board>> boardsLiveDataObserver;
    private long currentBoardId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        syncManager = new SyncManager(getApplicationContext(), this);
        stackAdapter = new StackAdapter(getSupportFragmentManager());

        //TODO limit this call only to lower API levels like KitKat because they crash without
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //TODO replace nulls
        new CrossTabDragAndDrop().register(this, viewPager, null, null);

//        viewPager.setOnDragListener((View v, DragEvent dragEvent) -> {
//            Log.d("Deck", "Drag: "+ dragEvent.getAction());
//            if(dragEvent.getAction() == 4)
//                Log.d("Deck", dragEvent.getAction() + "");
//
//            View view = (View) dragEvent.getLocalState();
//            RecyclerView owner = (RecyclerView) view.getParent();
//            CardAdapter cardAdapter = (CardAdapter) owner.getAdapter();
//
//            switch(dragEvent.getAction()) {
//                case DragEvent.ACTION_DRAG_LOCATION:
//                    Point size = new Point();
//                    getWindowManager().getDefaultDisplay().getSize(size);
//                    if(dragEvent.getX() <= 20) {
//                        Log.d("Deck", dragEvent.getAction() + " moved left");
//                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
//                    } else if(dragEvent.getX() >= size.x - 20) {
//                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
//                        Log.d("Deck", dragEvent.getAction() + " moved right");
//                    }
//                    int viewUnderPosition = owner.getChildAdapterPosition(owner.findChildViewUnder(dragEvent.getX(), dragEvent.getY()));
//                    if(viewUnderPosition != -1) {
//                        Log.d("Deck", dragEvent.getAction() + " moved something...");
//                        cardAdapter.moveItem(owner.getChildLayoutPosition(view), viewUnderPosition);
//                    }
//                    break;
//                case DragEvent.ACTION_DROP:
//                    view.setVisibility(View.VISIBLE);
//                    break;
//            }
//            return true;
//        });


        syncManager.hasAccounts().observe(MainActivity.this, (Boolean hasAccounts) -> {
            if (hasAccounts != null && hasAccounts) {
                syncManager.readAccounts().observe(MainActivity.this, (List<Account> accounts) -> {
                    if (accounts != null) {
                        accountsList = accounts;
                        int lastAccount = sharedPreferences.getInt(getString(R.string.shared_preference_last_account), 0);
                        if (accounts.size() > lastAccount) {
                            this.account = accounts.get(lastAccount);
                            currentBoardId = sharedPreferences.getLong(getString(R.string.shared_preference_last_board_for_account_) + this.account.getId(), NO_BOARDS);
                            SingleAccountHelper.setCurrentAccount(getApplicationContext(), this.account.getName());
                            setHeaderView();
                            syncManager = new SyncManager(getApplicationContext(), MainActivity.this);
                            ViewUtil.addAvatar(this, navigationView.getHeaderView(0).findViewById(R.id.drawer_current_account), this.account.getUrl(), this.account.getUserName());
                            // TODO show spinner
                            syncManager.synchronize(new IResponseCallback<Boolean>(this.account) {
                                @Override
                                public void onResponse(Boolean response) {
                                    //nothing
                                }
                            });
                            boardsLiveData = syncManager.getBoards(this.account.getId());
                            boardsLiveDataObserver = (List<Board> boards) -> {
                                boardsList = boards;
                                buildSidenavMenu();
                            };
                            boardsLiveData.observe(MainActivity.this, boardsLiveDataObserver);
                        }
                    }
                });
            } else {
                loginDialogFragment = new LoginDialogFragment();
                loginDialogFragment.show(MainActivity.this.getSupportFragmentManager(), "NoticeDialogFragment");
            }
        });

        navigationView.getHeaderView(0).findViewById(R.id.drawer_header_view).setOnClickListener(v -> {
            this.accountChooserActive = !this.accountChooserActive;
            if (accountChooserActive) {
                buildSidenavAccountChooser();
            } else {
                buildSidenavMenu();
            }
        });

        fab.setOnClickListener((View view) -> {
            Snackbar.make(coordinatorLayout, "Creating cards is not yet supported.", Snackbar.LENGTH_LONG).show();
        });
    }

    public void onAccountChoose(SingleSignOnAccount account) {
        getSupportFragmentManager().beginTransaction().remove(loginDialogFragment).commit();
        Account acc = new Account();
        acc.setName(account.name);
        acc.setUserName(account.username);
        acc.setUrl(account.url);
        final WrappedLiveData<Account> accountLiveData = this.syncManager.createAccount(acc);
        accountLiveData.observe(this, (Account ac) -> {
            if (accountLiveData.hasError()) {
                try {
                    accountLiveData.throwError();
                } catch (SQLiteConstraintException ex) {
                    Snackbar.make(coordinatorLayout, "Account bereits hinzugefügt", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(coordinatorLayout, "Account hinzugefügt", Snackbar.LENGTH_SHORT).show();
            }
        });

        SingleAccountHelper.setCurrentAccount(getApplicationContext(), account.name);
    }

    public void onCreateStack(String stackName) {
        Stack s = new Stack();
        s.setTitle(stackName);
        s.setBoardId(currentBoardId);
        //TODO: returns liveData of the created stack (once!) as desired
        // original to do: should return ID of the created stack, so one can immediately switch to the new board after creation
        syncManager.createStack(account.getId(), s);
    }

    public void onCreateBoard(String title, String color) {
        Board b = new Board();
        b.setTitle(title);
        String colorToSet = color.startsWith("#") ? color.substring(1) : color;
        b.setColor(colorToSet);
        //TODO: returns liveData of the created board (once!) as desired
        // original to do: on createBoard: should return ID of the created board, so one can immediately switch to the new board after creation
        syncManager.createBoard(account.getId(), b);
    }

    private void buildSidenavMenu() {
        navigationView.setItemIconTintList(null);
        Menu menu = navigationView.getMenu();
        menu.clear();
        SubMenu boardsMenu = menu.addSubMenu(getString(R.string.simple_boards));
        int index = 0;
        for (Board board : boardsList) {
            boardsMenu.add(Menu.NONE, index++, Menu.NONE, board.getTitle()).setIcon(ViewUtil.getTintedImageView(this, R.drawable.circle_grey600_36dp, "#" + board.getColor()));
        }
        boardsMenu.add(Menu.NONE, MENU_ID_ADD_BOARD, Menu.NONE, getString(R.string.add_board)).setIcon(R.drawable.ic_add_grey_24dp);
        menu.add(Menu.NONE, MENU_ID_ABOUT, Menu.NONE, getString(R.string.about)).setIcon(R.drawable.ic_info_outline_grey_24dp);
        if(currentBoardId == NO_BOARDS && boardsList.size() > 0) {
            Board currentBoard = boardsList.get(0);
            currentBoardId = currentBoard.getId();
            displayStacksForBoard(currentBoard, this.account);
        } else {
            for (Board board : boardsList) {
                if (currentBoardId == board.getId()) {
                    displayStacksForBoard(board, this.account);
                    break;
                }
            }
        }
    }

    private void buildSidenavAccountChooser() {
        Menu menu = navigationView.getMenu();
        menu.clear();
        int index = 0;
        for (Account account : this.accountsList) {
            menu.add(Menu.NONE, index++, Menu.NONE, account.getName()).setIcon(R.drawable.ic_person_grey600_24dp);
        }
        menu.add(Menu.NONE, MENU_ID_ADD_ACCOUNT, Menu.NONE, getString(R.string.add_account)).setIcon(R.drawable.ic_person_add_black_24dp);
    }

    /**
     * Displays the Stacks for the boardsList by index
     *
     * @param board Board
     */
    private void displayStacksForBoard(Board board, Account account) {
        if (toolbar != null) {
            toolbar.setTitle(board.getTitle());
        }
        syncManager.getStacksForBoard(account.getId(), board.getLocalId()).observe(MainActivity.this, (List<FullStack> fullStacks) -> {
            if (fullStacks != null) {
                stackAdapter.clear();
                for (FullStack stack : fullStacks) {
                    stackAdapter.addFragment(StackFragment.newInstance(board.getLocalId(), stack.getStack().getLocalId(), account), stack.getStack().getTitle());
                }
                runOnUiThread(() -> {
                    viewPager.setAdapter(stackAdapter);
                    stackLayout.setupWithViewPager(viewPager);
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.card_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_card_list_add_column:
                StackCreateDialogFragment alertdFragment = new StackCreateDialogFragment();
                alertdFragment.show(getSupportFragmentManager(), getString(R.string.create_stack));
                break;
            case R.id.action_card_list_board_details:
                Snackbar.make(coordinatorLayout, "Bord details has not been implemented yet.", Snackbar.LENGTH_LONG).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (accountChooserActive) {
            switch (item.getItemId()) {
                case MENU_ID_ADD_ACCOUNT:
                    loginDialogFragment = new LoginDialogFragment();
                    loginDialogFragment.show(MainActivity.this.getSupportFragmentManager(), "NoticeDialogFragment");
                    break;
                default:
                    boardsLiveData.removeObserver(boardsLiveDataObserver);
                    this.account = accountsList.get(item.getItemId());
                    SingleAccountHelper.setCurrentAccount(getApplicationContext(), this.account.getName());
                    setHeaderView();

                    boardsLiveData = syncManager.getBoards(this.account.getId());
                    boardsLiveDataObserver = (List<Board> boards) -> {
                        boardsList = boards;
                        accountChooserActive = false;
                        buildSidenavMenu();
                    };
                    boardsLiveData.observe(MainActivity.this, boardsLiveDataObserver);
                    if(boardsList.size() > 0) {
                        displayStacksForBoard(boardsList.get(0), this.account);
                    }

                    // Remember last account
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(getString(R.string.shared_preference_last_account), item.getItemId());
                    editor.apply();
            }
        } else {
            switch (item.getItemId()) {
                case MENU_ID_ABOUT:
                    Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
                    startActivityForResult(aboutIntent, ACTIVITY_ABOUT);
                    break;
                case MENU_ID_ADD_BOARD:
                    BoardCreateDialogFragment alertdFragment = new BoardCreateDialogFragment();
                    alertdFragment.show(getSupportFragmentManager(), getString(R.string.create_board));
                    break;
                default:
                    Board selectedBoard = boardsList.get(item.getItemId());
                    currentBoardId = selectedBoard.getId();
                    displayStacksForBoard(selectedBoard, account);

                    // Remember last board for this account
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong(getString(R.string.shared_preference_last_board_for_account_) + this.account.getId(), currentBoardId);
                    editor.apply();
            }
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setHeaderView() {
        ViewUtil.addAvatar(this, navigationView.getHeaderView(0).findViewById(R.id.drawer_current_account), account.getUrl(), account.getUserName());
        ((TextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_username_full)).setText(account.getName());
    }
}
