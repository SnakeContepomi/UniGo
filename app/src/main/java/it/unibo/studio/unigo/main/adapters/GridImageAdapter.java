package it.unibo.studio.unigo.main.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.FullSizeImageActivity;
import it.unibo.studio.unigo.main.PostActivity;
import it.unibo.studio.unigo.main.adapteritems.GridViewItem;

public class GridImageAdapter extends BaseAdapter
{
    private final String IMAGE_PATH = "path";
    private final String LOAD_FROM_FILE = "fromFile";

    private Context context;
    private final List<String> imagePathList;

    public GridImageAdapter(Context context, List<String> imagePathList)
    {
        this.context = context;
        this.imagePathList = imagePathList;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        GridViewItem container;
        final ImageView imgPhoto, removeIcon;
        TextView txtTitle;

        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.layout_item_photo, null);

        container = (GridViewItem) view.findViewById(R.id.itemPhotoLayout);
        imgPhoto = (ImageView) view.findViewById(R.id.itemPhotoImage);
        removeIcon = (ImageView) view.findViewById(R.id.itemPhotoIcon);
        txtTitle = (TextView) view.findViewById(R.id.itemPhotoTxt);

        // Inizializzazione immagine di sfondo
        Picasso.with(context).load(new File(imagePathList.get(i))).into(imgPhoto);

        // Inizializzazione nome immagine
        txtTitle.setText(imagePathList.get(i).substring(imagePathList.get(i).lastIndexOf("/")+1));

        // Inizializzazione della logica del tasto di rimozione
        // Inizializzazione immagine di sfondo
        removeIcon.setTag(i);
        removeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem((Integer) view.getTag());
            }
        });

        // Inizializzazione click Listener sul layout che permette di aprire l'activity FullImage
        container.setTag(i);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, FullSizeImageActivity.class);
                intent.putExtra(IMAGE_PATH, imagePathList.get((Integer) view.getTag()));
                intent.putExtra(LOAD_FROM_FILE, true);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation((PostActivity)context, imgPhoto, imgPhoto.getTransitionName());
                context.startActivity(intent, options.toBundle());
            }
        });

        return view;
    }

    @Override
    public int getCount() {
        return imagePathList.size();
    }

    @Override
    public Object getItem(int i) {
        return imagePathList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    private void removeItem(int position)
    {
        imagePathList.remove(position);
        notifyDataSetChanged();
    }
}