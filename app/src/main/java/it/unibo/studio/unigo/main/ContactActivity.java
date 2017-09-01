package it.unibo.studio.unigo.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.l4digital.fastscroll.FastScrollRecyclerView;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.UserAdapterItem;
import it.unibo.studio.unigo.main.adapters.ContactAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class ContactActivity extends AppCompatActivity
{
    private List<UserAdapterItem> userList;

    private Toolbar toolbar;
    private MaterialSearchView searchView;
    private LinearLayout loadingLayout;
    private FastScrollRecyclerView mRecyclerView;
    private ContactAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        overridePendingTransition(R.anim.activity_open_translate_from_bottom, R.anim.activity_no_animation);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        initComponents();
        initUserList();
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.activity_no_animation, R.anim.activity_close_translate_to_bottom);
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        if (searchView.isSearchOpen())
            searchView.closeSearch();
        else
            super.onBackPressed();
    }

    private void initComponents()
    {
        userList = new ArrayList<>();

        // Inizializzazione Toolbar
        toolbar = (Toolbar) findViewById(R.id.contact_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.menu_item_search);
        searchView = (MaterialSearchView) findViewById(R.id.search_view_contact);
        searchView.setMenuItem(toolbar.getMenu().getItem(0));
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                mAdapter.getFilter().filter(newText);
                return false;
            }
        });
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() { }

            // Alla chiusura della SearchView, viene ripristinata la lista iniziale dei contatti
            @Override
            public void onSearchViewClosed()
            {
                searchView.setQuery("", false);
            }
        });

        loadingLayout = (LinearLayout) findViewById(R.id.contact_loading);
        mRecyclerView = (FastScrollRecyclerView ) findViewById(R.id.contact_recyclerViewSocial);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Inizializzazione adapter della lista degli utenti
        mAdapter = new ContactAdapter(userList, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFastScrollEnabled(true);

        setRecyclerViewVisibility(false);
    }

    // Metodo utilizzato per caricare la lista di utenti registrati al corso nella recyclerview
    public void initUserList()
    {
        Util.getDatabase().getReference("User").orderByChild("courseKey").equalTo(Util.CURRENT_COURSE_KEY).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                while (iterator.hasNext())
                {
                    DataSnapshot child = iterator.next();
                    if (!child.getKey().equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                        userList.add(new UserAdapterItem(child.getValue(User.class), child.getKey()));
                    if (!iterator.hasNext())
                    {
                        sortUserList();
                        mAdapter.notifyDataSetChanged();
                        setRecyclerViewVisibility(true);
                        toolbar.setSubtitle(getResources().getString(R.string.contact_subtitle, mAdapter.getItemCount()));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo per ordinare la lista degli utenti prima sul campo nome, poi sul cognome
    private void sortUserList()
    {
        Collections.sort(userList, new Comparator<UserAdapterItem>() {
            @Override
            public int compare(UserAdapterItem u1, UserAdapterItem u2)
            {
                return new CompareToBuilder().append(u1.getUser().name, u2.getUser().name).append(u1.getUser().lastName, u2.getUser().lastName).toComparison();
            }
        });
    }

    // Metodo utilizzato per nascondere/mostrare la recyclerview
    private void setRecyclerViewVisibility(boolean b)
    {
        if (b)
        {
            loadingLayout.setVisibility(View.GONE);
            mRecyclerView.scrollToPosition(0);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        else
        {
            mRecyclerView.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.VISIBLE);
        }
    }
}