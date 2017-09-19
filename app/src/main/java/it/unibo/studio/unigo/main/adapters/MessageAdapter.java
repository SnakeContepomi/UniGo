package it.unibo.studio.unigo.main.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.squareup.picasso.Picasso;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Message;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private List<Message> messageList;

    private class ViewHolderSender extends RecyclerView.ViewHolder
    {
        TextView txtSenderMsg, txtSenderDate;
        MaterialLetterIcon imgLastRead;

        ViewHolderSender(View v)
        {
            super(v);
            txtSenderMsg = (TextView) v.findViewById(R.id.txtSenderMsg);
            txtSenderDate = (TextView) v.findViewById(R.id.txtSenderDate);
            imgLastRead = (MaterialLetterIcon) v.findViewById(R.id.imgLastRead);
        }
    }

    private class ViewHolderRecipient extends RecyclerView.ViewHolder
    {
        TextView txtSenderMsg, txtSenderDate;

        ViewHolderRecipient(View v)
        {
            super(v);
            txtSenderMsg = (TextView) v.findViewById(R.id.txtRecipientMsg);
            txtSenderDate = (TextView) v.findViewById(R.id.txtRecipientDate);
        }
    }

    public MessageAdapter(List<Message> messageList)
    {
        this.messageList = messageList;
    }

    // Metodo che permette di attribuire un layout diverso a seconda del mittende del messaggio
    @Override
    public int getItemViewType(int position)
    {
        Message msg = messageList.get(position);

        if (Util.decodeEmail(msg.sender_id).equals(Util.getCurrentUser().getEmail()))
            return 0;
        else
            return 1;
    }

    // Metodo che permette di attribuire un layout diverso a seconda del mittende del messaggio
    // Se il messaggio appartiene all'utilizzatore dell'app, viene restituito il codice relativo al layout 'Sender',
    // altrimenti viene restituito il codice relativo al destinatario della conversazione 'Recipient'
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if (viewType == 0)
            return new ViewHolderSender(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_sender, parent, false));
        else
            return new ViewHolderRecipient(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_recipient, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        Message msg = messageList.get(position);

        switch (holder.getItemViewType())
        {
            case 0:
                ((ViewHolderSender)holder).txtSenderMsg.setText(msg.message);
                ((ViewHolderSender)holder).txtSenderDate.setText(Util.formatDate(msg.date));
                break;
            case 1:
                ((ViewHolderRecipient)holder).txtSenderMsg.setText(msg.message);
                ((ViewHolderRecipient)holder).txtSenderDate.setText(Util.formatDate(msg.date));
                break;
        }
    }
}