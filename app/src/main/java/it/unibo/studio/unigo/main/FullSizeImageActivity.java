package it.unibo.studio.unigo.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import it.unibo.studio.unigo.R;

// Intent richiesti dall'activity! Dichiarati dentro initComponents
// IMAGE_PATH = path (o url) dell'immagine da caricare
// LOAD_FROM_FILE = boolean, true se bisogna caricare un file dalla memoria, false se deve essere caricato tramite url
public class FullSizeImageActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_size_image);
        initComponents();
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    private void initComponents()
    {
        final String IMAGE_PATH = "path";
        final String LOAD_FROM_FILE = "fromFile";

        String path = getIntent().getStringExtra(IMAGE_PATH);
        boolean loadFromFile = getIntent().getBooleanExtra(LOAD_FROM_FILE, true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.fullImageToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        ImageView img = (ImageView) findViewById(R.id.fullImage);

        // Caricamento dell'immagine da file
        if (loadFromFile)
        {
            Bitmap myBitmap = BitmapFactory.decodeFile(path);
            img.setImageBitmap(myBitmap);
        }
        // Caricamento dell'immagine da un URL
        else
        {
            ActivityCompat.postponeEnterTransition(this);
            Picasso.with(this).load(path).noFade().into(img, new Callback() {
                @Override
                public void onSuccess()
                {
                    ActivityCompat.startPostponedEnterTransition(FullSizeImageActivity.this);
                }

                @Override
                public void onError()
                {
                    ActivityCompat.startPostponedEnterTransition(FullSizeImageActivity.this);
                }
            });
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorDarkBlack));
    }
}