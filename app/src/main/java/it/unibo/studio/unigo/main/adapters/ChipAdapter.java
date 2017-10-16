package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;

public class ChipAdapter extends RecyclerView.Adapter<ChipAdapter.ImageHolder>
{
    private final int UPLOAD = 0;
    private final int DOWNLOAD = 1;
    private List<String> chipsList;
    // Variabile che stabilisce quale icona attribuire alle chips. L'adapter viene utilizzato in due contesti differenti:
    // - PostActivity, type = UPLOAD, le chips avranno l'icona 'rimuovi' per annullare l'upload del file
    // - DetailActivity, type = DOWNLOAD, le chips avranno l'icona 'scarica' per effettuare il download del file
    private int type;

    class ImageHolder extends RecyclerView.ViewHolder
    {
        Context context;
        LinearLayout layout;
        TextView text;
        ImageView img;

        ImageHolder(View v)
        {
            super(v);
            context = v.getContext();
            layout = (LinearLayout) v.findViewById(R.id.chipLayout);
            text = (TextView) v.findViewById(R.id.chipTxt);
            img = (ImageView) v.findViewById(R.id.chipImg);
        }
    }

    public ChipAdapter(int type)
    {
        chipsList = new ArrayList<>();
        this.type = type;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chip, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return chipsList.size();
    }

    @Override
    public void onBindViewHolder(final ImageHolder holder, int position)
    {
        switch (type)
        {
            case UPLOAD:
                holder.text.setText(chipsList.get(position));

                holder.img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        chipsList.remove(holder.getAdapterPosition());
                        notifyDataSetChanged();
                    }
                });
                break;

            case DOWNLOAD:
                holder.text.setText(chipsList.get(position));
                break;
        }
    }

    public void addElement(String fileName)
    {
        chipsList.add(fileName);
        notifyDataSetChanged();
    }
}