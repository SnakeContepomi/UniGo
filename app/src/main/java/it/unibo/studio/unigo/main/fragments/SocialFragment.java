package it.unibo.studio.unigo.main.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.l4digital.fastscroll.FastScrollRecyclerView;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapters.UserAdapter;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class SocialFragment extends Fragment
{
    private List<User> userList;

    LinearLayout loadingLayout;
    private FastScrollRecyclerView mRecyclerView;
    private UserAdapter mAdapter;
    private DividerItemDecoration divider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_social, container, false);
        initComponents(v);
        initUserList();
        return v;
    }

    private void initComponents(View v)
    {
        userList = new ArrayList<>();

        loadingLayout = (LinearLayout) v.findViewById(R.id.l_social_loading);
        mRecyclerView = (FastScrollRecyclerView ) v.findViewById(R.id.recyclerViewSocial);
        // Impostazione di ottimizzazione da usare se gli elementi non comportano il ridimensionamento della RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        // Inizializzazione adapter della lista degli utenti
        mAdapter = new UserAdapter(userList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFastScrollEnabled(true);

        divider = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(v.getContext().getDrawable(R.drawable.item_divider));

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
                    Log.d("Prova", "user");
                    final DataSnapshot user = iterator.next();
                    User u = user.getValue(User.class);
                    userList.add(u);
                    if (!iterator.hasNext())
                    {
                        sortUserList();
                        mAdapter.notifyDataSetChanged();
                        setRecyclerViewVisibility(true);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

    }

    // Metodo per caricare la lista degli utenti nella RecyclerView
    private void sortUserList()
    {
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2)
            {
                return new CompareToBuilder().append(u1.name, u2.name).append(u1.lastName, u2.lastName).toComparison();
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