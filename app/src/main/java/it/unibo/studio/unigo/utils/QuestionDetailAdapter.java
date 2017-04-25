package it.unibo.studio.unigo.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.squareup.picasso.Picasso;

import net.cachapa.expandablelayout.ExpandableLayout;

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
    private static class questionHolder extends RecyclerView.ViewHolder
    {
        Context context;
        // Campi del Recycler Item Answer
        TextView txt1;

        questionHolder(View v)
        {
            super(v);
            context = v.getContext();
            txt1 = (TextView) v.findViewById(R.id.cardq_name);
        }
    }

    private static class answerHolder extends RecyclerView.ViewHolder
    {
        Context context;
        // Campi del Recycler Item Answer
        MaterialLetterIcon imgProfile;
        LinearLayout btnComment;
        ExpandableLayout expandableLayout;
        TextView txtName, txtDesc;

        answerHolder(View v)
        {
            super(v);
            context = v.getContext();
            imgProfile = (MaterialLetterIcon) v.findViewById(R.id.carda_userPhoto);
            txtName = (TextView) v.findViewById(R.id.carda_name);
            txtDesc = (TextView) v.findViewById(R.id.carda_desc);
            btnComment = (LinearLayout) v.findViewById(R.id.carda_comment);
            expandableLayout = (ExpandableLayout) v.findViewById(R.id.expandable_layout);
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
            return new questionHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_question, parent, false));
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
                final answerHolder ah = (answerHolder) holder;
                Picasso.with(ah.imgProfile.getContext()).load(qd_item.getPhoto()).fit().into(ah.imgProfile);
                ah.txtName.setText(qd_item.getAnswer().user_key);
                ah.txtDesc.setText(qd_item.getAnswer().desc);
                ah.btnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        ah.expandableLayout.toggle();
                    }
                });
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