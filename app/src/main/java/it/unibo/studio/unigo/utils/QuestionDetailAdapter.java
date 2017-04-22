package it.unibo.studio.unigo.utils;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import it.unibo.studio.unigo.R;

public class QuestionDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_ANSWER = 2;
    private List<QuestionDetailAdapterItem> answerList;
    private QuestionAdapterItem question;
    private String user_name;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class questionHolder extends RecyclerView.ViewHolder
    {
        Context context;
        // Campi del Recycler Item Answer
        TextView txt1;

        questionHolder(View v)
        {
            super(v);
            context = v.getContext();
            txt1 = (TextView) v.findViewById(R.id.itemq_title);
        }
    }

    static class answerHolder extends RecyclerView.ViewHolder
    {
        Context context;
        // Campi del Recycler Item Answer
        CardView card;
        RoundedImageView imgProfile;
        TextView txt1, txt2;

        answerHolder(View v)
        {
            super(v);
            context = v.getContext();

            card = (CardView) v.findViewById(R.id.cardViewQuestionDetail);
            imgProfile = (RoundedImageView) v.findViewById(R.id.itema_user);
            txt1 = (TextView) v.findViewById(R.id.itema_txt1);
            txt2 = (TextView) v.findViewById(R.id.itema_txt2);
        }
    }

    public QuestionDetailAdapter(List<QuestionDetailAdapterItem> answerList, QuestionAdapterItem question, String user_name)
    {
        this.answerList = answerList;
        this.question = question;
        this.user_name = user_name;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if (viewType == 1)
            return new questionHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item_question, parent, false));
        else
            return new answerHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_answer, parent, false));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position)
    {
        final QuestionDetailAdapterItem qd_item = answerList.get(position);

        switch (holder.getItemViewType())
        {
            case TYPE_QUESTION:
                questionHolder qh = (questionHolder) holder;
                qh.txt1.setText(question.getQuestion().desc);
                break;

            case TYPE_ANSWER:
                answerHolder ah = (answerHolder) holder;
                Picasso.with(ah.imgProfile.getContext()).load(qd_item.getPhoto()).fit().into(ah.imgProfile);
                ah.txt1.setText(qd_item.getAnswer().user_key);
                ah.txt2.setText(qd_item.getAnswer().desc);
                break;
        }
    }

    // Se la posizione è la prima, l'oggetto viene gestito come Question, altrimenti come Answer
    @Override
    public int getItemViewType(int position)
    {
        return position == 0 ? TYPE_QUESTION : TYPE_ANSWER;
    }

    @Override
    public int getItemCount()
    {
        return answerList.size();
    }
}