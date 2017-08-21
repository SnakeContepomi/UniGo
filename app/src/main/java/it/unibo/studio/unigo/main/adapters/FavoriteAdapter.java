package it.unibo.studio.unigo.main.adapters;


import android.app.Activity;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.main.adapteritems.QuestionAdapterItem;

public class FavoriteAdapter extends QuestionAdapter
{
    public FavoriteAdapter(List<QuestionAdapterItem> questionList, Activity activity)
    {
        super(questionList, activity);
        this.backupList = new ArrayList<>();
    }

    @Override
    public void refreshQuestion(QuestionAdapterItem newQItem)
    {
        super.refreshQuestion(newQItem);
        backupList.set(getQuestionPosition(newQItem.getQuestionKey(), backupList), newQItem);
    }

    public void removeQuestion(String questionKey)
    {
        int position = getQuestionPosition(questionKey);

        questionList.remove(position);

        int originalPosition;
        for (originalPosition = 0; originalPosition < backupList.size(); originalPosition++)
            if (questionKey.equals(backupList.get(originalPosition).getQuestionKey()))
                break;
        backupList.remove(originalPosition);
        notifyItemRemoved(position);
    }

    public void addToBackupList(QuestionAdapterItem qItem)
    {
        backupList.add(0, qItem);
    }

    public void resetFilter()
    {
        this.questionList = backupList;
        notifyDataSetChanged();
    }

    private int getQuestionPosition(String questionKey, List<QuestionAdapterItem> list)
    {
        for (int i = 0; i < list.size(); i++)
            if (questionKey.equals(list.get(i).getQuestionKey()))
                return i;
        return -1;
    }
}