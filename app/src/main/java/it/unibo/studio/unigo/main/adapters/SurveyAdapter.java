package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import net.cachapa.expandablelayout.ExpandableLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.SurveyAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Survey;
import it.unibo.studio.unigo.utils.firebase.User;
import lecho.lib.hellocharts.listener.PieChartOnValueSelectListener;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.PieChartView;

public class SurveyAdapter extends Adapter<SurveyAdapter.SurveyHolder>
{
    private List<SurveyAdapterItem> surveyList;
    private Context context;

    class SurveyHolder extends RecyclerView.ViewHolder
    {
        Context context;
        MaterialLetterIcon survImg;
        TextView survName, survDate, survTitle, survDesc;
        ExpandableLayout expandableLayout;
        PieChartView survChart;
        LinearLayout survLegendLayout;
        Button survCardVoteBtn, survBtnExpandToggle;

        SurveyHolder(View v)
        {
            super(v);
            context = v.getContext();
            survImg = (MaterialLetterIcon) v.findViewById(R.id.survCardImg);
            survName = (TextView) v.findViewById(R.id.survCardName);
            survDate = (TextView) v.findViewById(R.id.survCardDate);
            survTitle = (TextView) v.findViewById(R.id.survCardTitle);
            survDesc = (TextView) v.findViewById(R.id.survCardDesc);
            expandableLayout = (ExpandableLayout) v.findViewById(R.id.survExpandableLayout);
            survChart = (PieChartView) v.findViewById(R.id.survChart);
            survLegendLayout = (LinearLayout) v.findViewById(R.id.survLegendLayout);
            survCardVoteBtn = (Button) v.findViewById(R.id.survCardVoteBtn);
            survBtnExpandToggle = (Button) v.findViewById(R.id.survCardExpandableBtn);
        }
    }

    public SurveyAdapter(Context context)
    {
        surveyList = new ArrayList<>();
        this.context = context;
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
        holder.survDate.setText(Util.formatDate(surveyList.get(position).getSurvey().date));
        holder.survTitle.setText(surveyList.get(position).getSurvey().title);
        holder.survDesc.setText(surveyList.get(position).getSurvey().desc);

        // Grafico a torta
        holder.survChart.setChartRotationEnabled(false);
        generateData(holder, position);
        holder.survChart.setOnValueTouchListener(new PieChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int arcIndex, SliceValue value)
            {
                Toast.makeText(holder.context,"Votata da " + (int) value.getValue()+" persone", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onValueDeselected() { }
        });

        if (checkIsAnswered(surveyList.get(position).getSurvey().choices))
        {
            holder.survCardVoteBtn.setEnabled(false);
            holder.survCardVoteBtn.setTextColor(ContextCompat.getColor(context, R.color.md_grey_500));
        }
        else
        {
            holder.survCardVoteBtn.setEnabled(true);
            holder.survCardVoteBtn.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }
        // Pulsante che permette di votare in un sondaggio
        holder.survCardVoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

            }
        });

        // Pulsante che permette di espandere e ridurre la card-sondaggio
        holder.survBtnExpandToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (holder.expandableLayout.isExpanded())
                {
                    holder.expandableLayout.collapse();
                    holder.survBtnExpandToggle.setText(context.getString(R.string.survey_list_open));
                }
                else
                {
                    holder.expandableLayout.expand();
                    holder.survBtnExpandToggle.setText(context.getString(R.string.survey_list_close));
                }
            }
        });
    }

    public void addElement(SurveyAdapterItem surveyItem)
    {
        surveyList.add(0, surveyItem);
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

    private void generateData(SurveyHolder holder, int pos)
    {
        // Sondaggio preso in esame
        Survey survey = surveyList.get(pos).getSurvey();
        // Lista del nuemro di voti per ciascuna opzione del sondaggio
        List<SliceValue> values = new ArrayList<>();
        // Numero di voti totale su tutte le opzioni
        int totalVotes = 0;
        // Lista dei possibili colori da assegnare alle varie opzioni di un sondaggio (max. 5 opzioni)
        List<Integer> colorList = new ArrayList<>();
        int[] colors = context.getResources().getIntArray(R.array.colors);
        for(int i : colors)
            colorList.add(i);


        // Per ciascuna opzione del sondaggio, viene contato il numero di persone che hanno votato, ricordando che:
        // Chiave della HashMap = testo dell'opzione del sondaggio
        // Valore della HashMap = HashMap contenente tutti i nominativi (email) delle persone che hanno votato quell'opzione
        for (Map.Entry<String, HashMap<String, Boolean>> choice : survey.choices.entrySet())
        {
            // Contatore del numero di persone che hanno votato una particolare scelta
            int nVotes = 0;

            // Chiave della HashMap = nominativo dell'utente che ha votato quell'opzione
            // Valore della HashMap = attualmente non utilizzato
            for (Map.Entry<String, Boolean> users : choice.getValue().entrySet())
                // Se la chiave dell'utente è diversa da empty (valore default attribuito durante la creazione del sondaggio), viene contata
                if (!users.getKey().equals("empty"))
                {
                    nVotes++;
                    totalVotes++;
                }

            // Viene assegnata una porzione di grafico solamente se esiste almeno un voto per quell'opzione
            if (nVotes != 0)
            {
                // Viene assegnato un colore casuale tra quelli disponibili
                int randomColor = (int) (Math.random() * colorList.size());
                values.add(new SliceValue(nVotes,colorList.get(randomColor)));

                // Container della legenda
                LinearLayout horizontalContainer = new LinearLayout(context);
                horizontalContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                horizontalContainer.setOrientation(LinearLayout.HORIZONTAL);
                horizontalContainer.setGravity(Gravity.CENTER);

                // Viene aggiunto un elemento nella legenda per indicare a cosa corrisponde
                // la porzione di grafico con quel determinato colore
                LinearLayout legendItem = new LinearLayout(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics()));
                params.setMargins(0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()), 0);
                legendItem.setLayoutParams(params);
                legendItem.setBackgroundColor(colorList.get(randomColor));
                TextView legendName = new TextView(context);
                legendName.setText(choice.getKey());
                legendName.setLines(1);
                legendName.setEllipsize(TextUtils.TruncateAt.END);

                horizontalContainer.addView(legendItem);
                horizontalContainer.addView(legendName);
                holder.survLegendLayout.addView(horizontalContainer);
                // Viene rimosso il colore utilizzato, in modo da utilizzarlo una sola volta per grafico
                colorList.remove(randomColor);
            }
        }

        // Se almeno una persona ha votato nel sondaggio, viene preparato il grafico da visualizzare
        if (values.size() != 0)
        {
            // Le informazioni ricavate dai voti vengono memorizzate in una classe di tipo PiechartData
            PieChartData data = new PieChartData(values);

            // Personalizzazione grafica del diagramma a torta
            data.setHasLabels(true);
            data.setValueLabelBackgroundAuto(false);
            data.setValueLabelBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            data.setHasCenterCircle(true);
            //data.setHasLabelsOnlyForSelected(false);  //default false
            //data.setHasLabelsOutside(false);          //default false
            //data.setSlicesSpacing(24);                // Distanzia le porzioni di grafico tra loro

            if (totalVotes != 0)
            {
                data.setCenterText1("Totale");
                data.setCenterText1FontSize(14);
                data.setCenterText2(String.valueOf(totalVotes));
                data.setCenterText2FontSize(14);
            }

            holder.survChart.setPieChartData(data);
        }
        // Se non è stato espresso nessun voto per nessuna opzione, viene nascosto il grafico
        else
            holder.survChart.setVisibility(View.GONE);
    }

    // Metodo che verifica se l'utente ha votato una delle opzioni del sondaggio
    private boolean checkIsAnswered(HashMap<String, HashMap<String, Boolean>> choices)
    {
        for (Map.Entry<String, HashMap<String, Boolean>> choice : choices.entrySet())
            for (Map.Entry<String, Boolean> user : choice.getValue().entrySet())
                if (Util.decodeEmail(user.getKey()).equals(Util.getCurrentUser().getEmail()))
                    return true;
        return false;
    }
}