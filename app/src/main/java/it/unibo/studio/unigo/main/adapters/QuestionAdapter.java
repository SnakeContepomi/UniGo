package it.unibo.studio.unigo.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.DetailActivity;
import it.unibo.studio.unigo.main.MainActivity;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.utils.Util;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> implements Filterable
{
    private final int UPDATE_CODE_QUESTION = 1;
    private final int UPDATE_CODE_FAVORITE = 2;

    private Filter mFilter = new ItemFilter();
    private List<QuestionAdapterItem> questionList;
    private boolean isFiltered;
    private Activity activity;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        Context context;
        CardView layout;
        MaterialLetterIcon imgProfile;
        TextView txtName, txtCourse, txtTitle, txtDate, txtRating, txtAnswer;
        LinearLayout rating;
        ImageView imgRating, imgFavorite;

        ViewHolder(CardView v)
        {
            super(v);
            context = v.getContext();
            layout = v;
            imgProfile = (MaterialLetterIcon ) v.findViewById(R.id.itemq_user);
            txtName = (TextView) v.findViewById(R.id.itemq_name);
            txtDate = (TextView) v.findViewById(R.id.itemq_date);
            txtCourse = (TextView) v.findViewById(R.id.itemq_course);
            txtTitle = (TextView) v.findViewById(R.id.itemq_title);
            rating = (LinearLayout) v.findViewById(R.id.itemq_rating);
            imgRating = (ImageView) v.findViewById(R.id.itemq_imgrating);
            txtRating = (TextView) v.findViewById(R.id.itemq_nrating);
            imgFavorite = (ImageView) v.findViewById(R.id.itemq_imgfavorite);
            txtAnswer = (TextView) v.findViewById(R.id.itemq_nanswer);
        }
    }

    // Classe per filtrare la lista dell'Adapter
    private class ItemFilter extends Filter
    {
        // Il filtro restituisce gli elementi che contengono la chiave di ricerca
        // nei campi Titolo, Descrizione e Materia
        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            String filterString = constraint.toString().toLowerCase();
            questionList = Util.getQuestionList();
            List<QuestionAdapterItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            for(int i = 0; i < questionList.size(); i++)
                if (questionList.get(i).getQuestion().title.toLowerCase().contains(filterString)
                        || questionList.get(i).getQuestion().course.toLowerCase().contains(filterString)
                        || questionList.get(i).getQuestion().desc.toLowerCase().contains(filterString))
                    filteredList.add(0, questionList.get(i));

            results.values = filteredList;
            results.count = filteredList.size();

            return results;
        }

        // Metodo per aggiornare graficamente la lista
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            questionList = (ArrayList<QuestionAdapterItem>) results.values;
            Collections.sort(questionList, new Comparator<QuestionAdapterItem>() {
                @Override
                public int compare(QuestionAdapterItem qItem1, QuestionAdapterItem qItem2)
                {
                    return new CompareToBuilder().append(qItem2.getQuestionKey(), qItem1.getQuestionKey()).toComparison();
                }
            });
            notifyDataSetChanged();
        }
    }

    public QuestionAdapter(List<QuestionAdapterItem> questionList, Activity activity)
    {
        this.questionList = questionList;
        this.activity = activity;
        isFiltered = false;
    }

    @Override
    public QuestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ViewHolder((CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_question, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        final QuestionAdapterItem qItem = questionList.get(position);

        initQuestion(holder, qItem);
        initActionRating(holder, qItem);
        initActionFavorite(holder, qItem.getQuestionKey());

        // Viene inizializzato il numero di risposte relative alla domanda in questione
        if (qItem.getQuestion().answers != null)
            holder.txtAnswer.setText(String.valueOf(qItem.getQuestion().answers.size()));
        else
            holder.txtAnswer.setText("0");

        // Al click della card viene aperta l'activity che ne mostra i dettagli
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, DetailActivity.class);
                intent.putExtra("question_key", qItem.getQuestionKey());
                activity.startActivityForResult(intent, MainActivity.REQUEST_CODE_DETAIL);
            }
        });
    }

    // Aggiornamento parziale di uno o più elementi della recyclerview in realtime (rating, commenti, favourite)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position, List<Object> payloads)
    {
        // Aggiornamento totale
        if (payloads.isEmpty())
            onBindViewHolder(holder, position);
        // Aggiornamento parziale
        else if (payloads.get(0) instanceof Integer)
            switch ((Integer) payloads.get(0))
            {
                // Aggiornamento di tutte le Action della cardQuestion (Rating, Favorite e Answers)
                case UPDATE_CODE_QUESTION:
                    // Action Rating
                    holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorIconGray)));
                    holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorIconGray)));
                    if (questionList.get(position).getQuestion().ratings != null)
                    {
                        holder.txtRating.setText(String.valueOf(questionList.get(position).getQuestion().ratings.size()));
                        if (questionList.get(position).getQuestion().ratings.keySet().contains(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                        {
                            holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                            holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                            holder.rating.setClickable(false);
                        }
                    }

                    // Favorite
                    updateFavorite(holder, position);

                    // Numero risposte
                    if (questionList.get(position).getQuestion().answers != null)
                        holder.txtAnswer.setText(String.valueOf(questionList.get(position).getQuestion().answers.size()));
                    else
                        holder.txtAnswer.setText("0");
                    break;

                // Aggiornamento parziale del solo stato (favorite) della domanda corrente
                case UPDATE_CODE_FAVORITE:
                    updateFavorite(holder, position);
                    break;

                default:
                    break;
            }
    }

    @Override
    public int getItemCount()
    {
        return questionList.size();
    }

    @Override
    public Filter getFilter()
    {
        return mFilter;
    }

    // Metodo che recupera le informazioni relative a ciascuna domanda
    private void initQuestion(final ViewHolder holder, QuestionAdapterItem qItem)
    {
        holder.txtDate.setText(Util.formatDate(qItem.getQuestion().date));
        holder.txtCourse.setText(qItem.getQuestion().course);
        holder.txtTitle.setText(qItem.getQuestion().title);

        // Viene recuperata l'immagine profilo dell'utente che ha effettauto la domanda
        Util.getDatabase().getReference("User").child(qItem.getQuestion().user_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                holder.txtName.setText(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                if (!Util.isNetworkAvailable(holder.context) || dataSnapshot.child("photoUrl").getValue(String.class).equals(holder.context.getResources().getString(R.string.empty_profile_pic_url)))
                {
                    holder.imgProfile.setLetter(dataSnapshot.child("name").getValue(String.class));
                    holder.imgProfile.setShapeColor(Util.getLetterBackgroundColor(holder.context, dataSnapshot.child("name").getValue(String.class)));
                }
                else
                    Picasso.with(holder.imgProfile.getContext()).load(dataSnapshot.child("photoUrl").getValue(String.class)).fit().into(holder.imgProfile);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che inizializza la logica del pulsante "Rating" relativo alla domanda in questione
    // Il pulsante Rating è cliccabile soltanto una volta
    private void initActionRating(final ViewHolder holder, final QuestionAdapterItem qItem)
    {
        // Click Listener relativo alla Action "Rating" (cuore)
        holder.rating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Util.getDatabase().getReference("Question").child(qItem.getQuestionKey()).child("ratings").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).setValue(true);
                holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.txtRating.setText(String.valueOf(Integer.valueOf(String.valueOf(holder.txtRating.getText())) + 1));
                holder.rating.setClickable(false);
            }
        });

        // Al caricamento di ogni domanda, viene controllato se l'utente ha già votato la domanda
        // (un utente può votare la domanda una sola volta)
        holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorIconGray)));
        holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorIconGray)));
        if (qItem.getQuestion().ratings != null)
        {
            // Inizializzazione del numero di rating della domanda corrente
            holder.txtRating.setText(String.valueOf(qItem.getQuestion().ratings.size()));

            if (qItem.getQuestion().ratings.keySet().contains(Util.encodeEmail(Util.getCurrentUser().getEmail())))
            {
                holder.imgRating.setImageTintList(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.txtRating.setTextColor(ColorStateList.valueOf(holder.context.getResources().getColor(R.color.colorPrimary)));
                holder.rating.setClickable(false);
            }
        }
        else
            holder.txtRating.setText("0");
    }

    // Metodo che verifica se la domanda appartiene all'elenco dei preferiti dell'utente
    // e abilita il clickListener per poterla inserire
    private void initActionFavorite(final ViewHolder holder, String questionKey)
    {
        final DatabaseReference favoriteReference = Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(questionKey);

        // Viene controllato se la domanda è presente all'interno dei preferiti dell'utente
        favoriteReference.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                // Se la domanda risulta tra i preferiti, viene evidenziata
                if (dataSnapshot.getValue() != null)
                    holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorAmber)));
                else
                    holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorIconGray)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        // Pulsante che consente di aggiungere una domanda ai preferiti
        holder.imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                favoriteReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        // Se la domanda non è nei preferiti, essa viene aggiunta ed evidenziata
                        if (dataSnapshot.getValue() == null)
                        {
                            favoriteReference.setValue(true);
                            holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorAmber)));
                        }
                        // Se la domanda è già nei preferiti, essa viene rimossa
                        else
                        {
                            favoriteReference.removeValue();
                            holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorIconGray)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });
            }
        });
    }

    // Metodo utilizzato per aggiornare l'inserimento dell'elemento in posizione "position" nella recyclerview
    public void updateElement(int position)
    {
        if (!isFiltered)
            notifyItemInserted(position);
    }

    // Metodo utilizzato per aggiornare i campi "rating", "favorite" e "answers" della domanda corrente, ad ogni eventuale cambiamento
    public void refreshQuestion(QuestionAdapterItem newQItem)
    {
        int position = getQuestionPosition(newQItem.getQuestionKey());

        if (position != -1)
        {
            questionList.set(position, newQItem);
            notifyItemChanged(position, UPDATE_CODE_QUESTION);
        }
    }

    // Metodo utilizzato per aggiornare solo il campo "favorite" della domanda corrente, ogni volta che viene chiusa l'activity Detail
    public void refreshFavorite(String questionKey)
    {
        int position = getQuestionPosition(questionKey);

        if (position != -1)
            notifyItemChanged(position, UPDATE_CODE_FAVORITE);
    }

    // Metodo per aggiornare graficamente il campo Favorite
    private void updateFavorite(final ViewHolder holder, int position)
    {
        Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("favorites").child(questionList.get(position).getQuestionKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() != null)
                    holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorAmber)));
                else
                    holder.imgFavorite.setImageTintList(ColorStateList.valueOf(holder.imgFavorite.getContext().getResources().getColor(R.color.colorIconGray)));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    // Metodo che reinizializza la lista delle domande presente nell'Adapter, con quella presente in Util
    // (ogni volta che viene chiusa la SearchView)
    public void resetFilter()
    {
        questionList = Util.getQuestionList();
        notifyDataSetChanged();
    }

    // Metodo utilizzato per mantenere aggiornato lo stato della SearchView (Aperta/Chiusa)
    public void setFilterState(boolean state)
    {
        isFiltered = state;
    }

    // Data una chiave di una domanda, viene restituita la posizione della stessa all'interno della lista
    private int getQuestionPosition(String questionKey)
    {
        for (int i = 0; i < questionList.size(); i++)
            if (questionKey.equals(questionList.get(i).getQuestionKey()))
                return i;
        return -1;
    }
}