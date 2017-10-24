package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.SurveyAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.User;

public class SurveyAdapter extends Adapter<SurveyAdapter.SurveyHolder>
{
    private List<SurveyAdapterItem> surveyList;

    class SurveyHolder extends RecyclerView.ViewHolder
    {
        Context context;
        MaterialLetterIcon survImg;
        TextView survName, survDate, survTitle;
        Button survBtn;

        SurveyHolder(View v)
        {
            super(v);
            context = v.getContext();
            survImg = (MaterialLetterIcon) v.findViewById(R.id.survCardImg);
            survName = (TextView) v.findViewById(R.id.survCardName);
            survDate = (TextView) v.findViewById(R.id.survCardDate);
            survTitle = (TextView) v.findViewById(R.id.survCardTitle);
            survBtn = (Button) v.findViewById(R.id.survCardBtn);
        }
    }

    public SurveyAdapter()
    {
        surveyList = new ArrayList<>();
    }

    @Override
    public SurveyHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new SurveyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_survey, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return surveyList.size();
    }

    @Override
    public void onBindViewHolder(final SurveyHolder holder, final int position)
    {
        holder.survDate.setText(Util.formatDate(surveyList.get(position).getSurvey().date));
        holder.survTitle.setText(surveyList.get(position).getSurvey().title);

        // Viene recuperata l'immagine profilo dell'utente che ha effettauto il sondaggio
        Util.getDatabase().getReference("User").child(surveyList.get(position).getSurvey().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final User user = dataSnapshot.getValue(User.class);

                holder.survName.setText(user.name + " " + user.lastName);
                Picasso.with(holder.context).load(user.photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(holder.survImg, new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError()
                    {
                        holder.survImg.setLetter(user.name);
                        holder.survImg.setShapeColor(Util.getLetterBackgroundColor(holder.context, user.name));
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        holder.survBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Toast.makeText(holder.context, "Helloh!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addElement(SurveyAdapterItem surveyItem)
    {
        surveyList.add(surveyItem);
        notifyDataSetChanged();
    }

    // Metodo che restituisce la posizione del sondaggio nella lista, in base alla chiave fornita
    public int getSurveyPosition(String surveyKey)
    {
        for(int i = 0; i < surveyList.size(); i++)
            if (surveyList.get(i).getSurveyKey().equals(surveyKey))
                return i;
        return -1;
    }
}