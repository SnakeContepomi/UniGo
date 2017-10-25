package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
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
        Button survBtn;

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
            survBtn = (Button) v.findViewById(R.id.survCardBtn);
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
        generateData(holder.survChart, position);
        holder.survChart.setOnValueTouchListener(new PieChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int arcIndex, SliceValue value)
            {
                Toast.makeText(holder.context, "Selected: " + value, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onValueDeselected() { }
        });

        // Pulsante che permette di votare un sondaggio (1 voto per sondaggio)
        holder.survBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (holder.expandableLayout.isExpanded())
                {
                    holder.expandableLayout.collapse();
                    holder.survBtn.setText(context.getString(R.string.survey_list_open));
                }
                else
                {
                    holder.expandableLayout.expand();
                    holder.survBtn.setText(context.getString(R.string.survey_list_close));
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

    private void generateData(PieChartView chart, int pos)
    {
        // Sondaggio preso in esame
        Survey survey = surveyList.get(pos).getSurvey();
        // Lista del nuemro di voti per ciascuna opzione del sondaggio
        List<SliceValue> values = new ArrayList<>();
        // Numero di voti totale su tutte le opzioni
        int totalVotes = 0;
        // Lista dei possibili colori da assegnare alle varie opzioni di un sondaggio (max. 5 opzioni)
        List<Integer> colorList = new ArrayList<>();
        colorList.add(Color.parseColor("#33B5E5"));
        colorList.add(Color.parseColor("#AA66CC"));
        colorList.add(Color.parseColor("#99CC00"));
        colorList.add(Color.parseColor("#FFBB33"));
        colorList.add(Color.parseColor("#FF4444"));

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
                int randomColor = (int) (Math.random() * colorList.size());
                values.add(new SliceValue(nVotes,colorList.get(randomColor)));
                colorList.remove(randomColor);
            }
        }

        // Se non è stato espresso nessun voto per nessuna opzione, viene nascosto il grafico
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

            chart.setPieChartData(data);
        }
        else
            chart.setVisibility(View.GONE);
    }
}