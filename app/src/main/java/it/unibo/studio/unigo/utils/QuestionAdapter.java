package it.unibo.studio.unigo.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.firebase.Question;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.ViewHolder> {
    private List<Question> questionList;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        // Campi del Recycler Item Question
        public TextView txtTitle, txtCourse, txtDesc, txtDate;
        public RoundedImageView imgProfile;
        public ImageView imgIcon;

        public ViewHolder(LinearLayout v)
        {
            super(v);
            txtTitle = (TextView) v.findViewById(R.id.itemq_title);
            txtCourse = (TextView) v.findViewById(R.id.itemq_course);
            txtDesc = (TextView) v.findViewById(R.id.itemq_desc);
            txtDate = (TextView) v.findViewById(R.id.itemq_date);
            imgProfile = (RoundedImageView) v.findViewById(R.id.itemq_user);
        }
    }

    public QuestionAdapter(List<Question> questionList)
    {
        this.questionList = questionList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public QuestionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item_question, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Question q = questionList.get(position);

        holder.txtTitle.setText(q.title);
        holder.txtCourse.setText(q.course);
        holder.txtDesc.setText(q.desc);
        holder.txtDate.setText(q.date);
        Picasso.with(holder.imgProfile.getContext())
                .load("https://firebasestorage.googleapis.com/v0/b/unigo-569da.appspot.com/o/profile_pic%2Fempty_profile_pic.png?alt=media&token=f7967e62-d479-40b8-a6fa-4675f5b1ecf0")
                .into(holder.imgProfile);
    }

    @Override
    public int getItemCount()
    {
        return questionList.size();
    }
}