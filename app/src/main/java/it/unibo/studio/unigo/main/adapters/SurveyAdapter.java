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
import com.afollestad.materialdialogs.MaterialDialog;
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
    public void onBindViewHolder(final SurveyHolder holder, int position)
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
        if (!surveyList.get(position).isInitialized())
        {
            for(View v : surveyList.get(position).getGraphLegend())
                holder.survLegendLayout.addView(v);
            holder.survChart.setPieChartData(surveyList.get(position).getPieChartData());
            surveyList.get(position).setInitialized();
        }

        // Se non è stato espresso nessun voto per nessuna opzione, viene nascosto il grafico
        if (Integer.valueOf(surveyList.get(position).getPieChartData().getCenterText2()) == 0)
        {
            holder.survChart.setVisibility(View.GONE);
            holder.survLegendLayout.setVisibility(View.GONE);
        }
        // Alla pressione di una fetta del grafico, vengono indicati tutti i nomi dei relativi votanti
        holder.survChart.setOnValueTouchListener(new PieChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int arcIndex, SliceValue value)
            {
                getUsersVotes(holder.getAdapterPosition(), value.getColor());
            }

            @Override
            public void onValueDeselected() { }
        });

        // Controllo iniziale per verificare se l'utente ha già votato un sondaggio
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
                // Vengono mappate in una lista temporanea tutte le opzioni del sondaggio
                List<String> surveyChoices = new ArrayList<>();
                for(String choice : surveyList.get(holder.getAdapterPosition()).getSurvey().choices.keySet())
                    surveyChoices.add(Util.decodeEmail(choice));
                new MaterialDialog.Builder(context)
                        .title(R.string.survey_alert_title)
                        .items(surveyChoices)
                        .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                // Viene aggiunto il voto nella voce selezionata del relativo sondaggio
                                Util.getDatabase().getReference("Survey").child(surveyList.get(holder.getAdapterPosition()).getSurveyKey())
                                        .child("choices").child(Util.encodeEmail(text.toString())).child(Util.encodeEmail(Util.getCurrentUser().getEmail())).setValue(true);
                                // Viene memorizzato nella tabella User il sondaggio a cui è stato inserito il voto
                                Util.getDatabase().getReference("User").child(Util.encodeEmail(Util.getCurrentUser().getEmail())).child("surveys_voted")
                                        .child(surveyList.get(holder.getAdapterPosition()).getSurveyKey()).setValue(true);
                                Toast.makeText(context, R.string.survey_vote_confirmed, Toast.LENGTH_SHORT).show();
                                surveyList.get(holder.getAdapterPosition()).getSurvey().choices.get(Util.encodeEmail(text.toString())).put(Util.encodeEmail(Util.getCurrentUser().getEmail()), true);

                                holder.survCardVoteBtn.setEnabled(false);
                                holder.survCardVoteBtn.setTextColor(ContextCompat.getColor(context, R.color.md_grey_500));
                                updateGraphData(holder, holder.getAdapterPosition(), Util.encodeEmail(text.toString()));
                                return true;
                            }
                        })
                        .positiveText(R.string.survey_alert_confirm)
                        .show();
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
        initializeGraphData(surveyItem);
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

    private void initializeGraphData(SurveyAdapterItem surveyItem)
    {
        // Sondaggio preso in esame
        Survey survey = surveyItem.getSurvey();
        // Numero di voti totale su tutte le opzioni
        int totalVotes = 0;
        // Lista dei possibili colori da assegnare alle varie opzioni di un sondaggio (max. 5 opzioni)
        List<Integer> colorList = new ArrayList<>();
        int[] colors = context.getResources().getIntArray(R.array.colors);
        for(int i : colors)
            colorList.add(i);
        // Mappa usata per associare il  numero di voti (e il colore utilizzato) con l'opzione del sondaggio
        HashMap<String, SliceValue> sliceMap = new HashMap<>();

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

            // Viene assegnata una porzione di grafico:
            // viene assegnato un colore casuale tra quelli disponibili
            int randomColor = (int) (Math.random() * colorList.size());
            SliceValue slice = new SliceValue(nVotes,colorList.get(randomColor));
            if (nVotes == 0)
                slice.setLabel("");
            sliceMap.put(choice.getKey(), slice);

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
            legendName.setText(Util.decodeEmail(choice.getKey()));
            legendName.setLines(1);
            legendName.setEllipsize(TextUtils.TruncateAt.END);
            horizontalContainer.addView(legendItem);
            horizontalContainer.addView(legendName);
            surveyItem.setGraphLegend(horizontalContainer);

            // Viene rimosso il colore utilizzato, in modo da utilizzarlo una sola volta per grafico
            colorList.remove(randomColor);
        }

        surveyItem.setGraphSlice(sliceMap);
        // Le informazioni ricavate dai voti vengono memorizzate in una classe di tipo PiechartData
        PieChartData data = new PieChartData(new ArrayList<>(sliceMap.values()));

        // Personalizzazione grafica del diagramma a torta
        data.setHasLabels(true);
        data.setValueLabelBackgroundAuto(false);
        data.setValueLabelBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
        data.setHasCenterCircle(true);
        //data.setHasLabelsOnlyForSelected(false);  // default false
        //data.setHasLabelsOutside(false);          // default false
        //data.setSlicesSpacing(24);                // Distanzia le porzioni di grafico tra loro

        data.setCenterText1("Totale");
        data.setCenterText1FontSize(14);
        data.setCenterText2(String.valueOf(totalVotes));
        data.setCenterText2FontSize(14);

        surveyItem.setPieChartData(data);
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

    // Metodo che permette di aggiornare il grafico non appena
    private void updateGraphData(SurveyHolder holder, int pos, String choice)
    {
        surveyList.get(pos).getGraphSlice().get(choice).setLabel(String.valueOf((int) surveyList.get(pos).getGraphSlice().get(choice).getValue() + 1));
        surveyList.get(pos).getGraphSlice().get(choice).setTarget(surveyList.get(pos).getGraphSlice().get(choice).getValue() + 1);

        holder.survChart.getPieChartData().setValues(new ArrayList<>(surveyList.get(pos).getGraphSlice().values()));
        holder.survChart.startDataAnimation(750);

        // Se un sondaggio riceve il primo voto in assoluto,
        // bisognerà rendere visibile il grafico e la relativa legenda
        if (holder.survChart.getPieChartData().getCenterText2().equals("0"))
        {
            holder.survChart.getPieChartData().setCenterText2("1");
            holder.survChart.setVisibility(View.VISIBLE);
            holder.survLegendLayout.setVisibility(View.VISIBLE);
        }
        else
            holder.survChart.getPieChartData().setCenterText2(String.valueOf(Integer.valueOf(holder.survChart.getPieChartData().getCenterText2()) + 1));
    }

    // Metodo che restituisce la lista di tutti gli utenti che hanno votato la scelta selezionata
    // Nota: il colore viene utilizzato come identificativo della scelta selezionata, dato che è univoco nel grafico
    private void getUsersVotes(int pos, int color)
    {
        String choiceKey = "";

        for(Map.Entry<String,SliceValue> slice : surveyList.get(pos).getGraphSlice().entrySet())
            if (slice.getValue().getColor() == color)
            {
                choiceKey = slice.getKey();
                break;
            }

        final List<String> mailList = new ArrayList<>(surveyList.get(pos).getSurvey().choices.get(choiceKey).keySet());
        final List<String> nameList = new ArrayList<>();
        if (mailList.contains("empty"))
            mailList.remove("empty");

        for(String mail : mailList)
        {
            Util.getDatabase().getReference("User").child(mail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    nameList.add(dataSnapshot.child("name").getValue(String.class) + " " + dataSnapshot.child("lastName").getValue(String.class));
                    if (mailList.size() == nameList.size())
                        new MaterialDialog.Builder(context)
                                .title("Risposta votata da:")
                                .items(nameList)
                                .positiveText(R.string.alert_dialog_confirm)
                                .show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
    }
}