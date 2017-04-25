package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;
import it.unibo.studio.unigo.main.adapteritems.DetailAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Comment;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_ANSWER = 2;
    private List<DetailAdapterItem> answerList;
    private QuestionAdapterItem question;
    private String user_name;

    private static class questionHolder extends RecyclerView.ViewHolder
    {
        Context context;
        TextView txtName;

        questionHolder(View v)
        {
            super(v);
            context = v.getContext();
            txtName = (TextView) v.findViewById(R.id.cardq_name);
        }
    }

    private static class answerHolder extends RecyclerView.ViewHolder
    {
        Context context;

        MaterialLetterIcon imgProfile;
        LinearLayout btnComment;
        ExpandableLayout expandableLayout;
        RecyclerView recyclerViewComment;
        CommentAdapter cAdapter;
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
            recyclerViewComment = (RecyclerView) v.findViewById(R.id.recyclerViewComment);
            recyclerViewComment.setLayoutManager(new LinearLayoutManager(context));
        }
    }

    public DetailAdapter(List<DetailAdapterItem> answerList, QuestionAdapterItem question, String user_name)
    {
        this.answerList = answerList;
        this.question = question;
        this.user_name = user_name;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if (viewType == 1)
            return new questionHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.question_detail, parent, false));
        else
            return new answerHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_answer, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position)
    {
        final DetailAdapterItem qd_item = answerList.get(position);

        switch (holder.getItemViewType())
        {
            case TYPE_QUESTION:
                questionHolder qh = (questionHolder) holder;
                qh.txtName.setText(question.getQuestion().desc);
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
                // Query recupero commenti
                initCommentList(ah, answerList.get(position).getAnswerKey());
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

    // Metodo per recupereare tutti i commenti di una relativa risposta
    private void initCommentList(final answerHolder ah, String answer_key)
    {
        final List<Comment> commentList = new ArrayList<>();

        Util.getDatabase().getReference("Question").child(question.getQuestionKey()).child("answers").child(answer_key).child("comments").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                while (iterator.hasNext())
                {
                    final DataSnapshot comment = iterator.next();

                    commentList.add(comment.getValue(Comment.class));
                    if (!iterator.hasNext())
                    {
                        ah.cAdapter = new CommentAdapter(commentList);
                        ah.recyclerViewComment.setAdapter(ah.cAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
}