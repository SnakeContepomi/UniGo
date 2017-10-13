package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.DetailActivity;
import it.unibo.studio.unigo.main.FullSizeImageActivity;

class DetailPictureAdapter extends Adapter<DetailPictureAdapter.ImageHolder>
{
    private final String IMAGE_PATH = "path";
    private final String LOAD_FROM_FILE = "fromFile";
    private List<String> pictureList;

    class ImageHolder extends RecyclerView.ViewHolder
    {
        Context context;
        ImageView detailPic;

        ImageHolder(View v)
        {
            super(v);
            context = v.getContext();
            detailPic = (ImageView) v.findViewById(R.id.detailPic);
        }
    }

    DetailPictureAdapter(List<String> pictureList)
    {
        this.pictureList = pictureList;
    }

    @Override
    public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new ImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_detail_picture, parent, false));
    }

    @Override
    public void onBindViewHolder(final ImageHolder holder, int position)
    {
        // Inizio dell'animazione di attesa del download dell'immagine da Internet
        ((Animatable) holder.detailPic.getDrawable()).start();

        // Download dell'immagine
        Picasso.with(holder.context).load(pictureList.get(holder.getAdapterPosition())).noPlaceholder().into(holder.detailPic, new Callback() {
            @Override
            public void onSuccess() {holder.detailPic.setVisibility(View.VISIBLE); }

            @Override
            public void onError()
            {
                Toast.makeText(holder.context, "Holy Fuuuck!", Toast.LENGTH_SHORT).show();
                holder.detailPic.setVisibility(View.GONE);
            }
        });
        // ***onFailure*** impostare immagine quando la connessione non è presente, o togliere quell'obrobrio che gira

        // ClickListener per visualizzare a pieno schermo l'immagine
        holder.detailPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(holder.context, FullSizeImageActivity.class);
                intent.putExtra(IMAGE_PATH, pictureList.get(holder.getAdapterPosition()));
                intent.putExtra(LOAD_FROM_FILE, false);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((DetailActivity) holder.context, holder.detailPic, holder.detailPic.getTransitionName());
                holder.context.startActivity(intent, options.toBundle());
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return pictureList.size();
    }
}