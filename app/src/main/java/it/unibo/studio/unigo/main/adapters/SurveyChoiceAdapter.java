package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;

public class SurveyChoiceAdapter extends Adapter<SurveyChoiceAdapter.ChoiceHolder>
{
    private List<String> choiceList;

    class ChoiceHolder extends RecyclerView.ViewHolder
    {
        Context context;
        ImageView removeBtn;
        TextView txtChoice;

        ChoiceHolder(View v)
        {
            super(v);
            context = v.getContext();
            removeBtn = (ImageView) v.findViewById(R.id.btnSurvList);
            txtChoice = (TextView) v.findViewById(R.id.txtSurvList);
        }
    }

    public SurveyChoiceAdapter()
    {
        choiceList = new ArrayList<>();
    }

    @Override
    public ChoiceHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ChoiceHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_survey_choice, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return choiceList.size();
    }

    @Override
    public void onBindViewHolder(final ChoiceHolder holder, final int position)
    {
        holder.txtChoice.setText(choiceList.get(position));
        holder.removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                removeElement(holder.getAdapterPosition());
            }
        });
    }

    private void removeElement(int pos)
    {
        choiceList.remove(pos);
        notifyDataSetChanged();
    }

    public void addElement(String choice)
    {
        choiceList.add(choice);
        notifyDataSetChanged();
    }

    public List<String> getChoiceList()
    {
        return choiceList;
    }
}