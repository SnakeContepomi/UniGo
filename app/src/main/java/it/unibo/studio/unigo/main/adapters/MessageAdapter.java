package it.unibo.studio.unigo.main.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.akashandroid90.imageletter.MaterialLetterIcon;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import it.unibo.studio.unigo.R;
import it.unibo.studio.unigo.main.adapteritems.MessageAdapterItem;
import it.unibo.studio.unigo.utils.Util;
import it.unibo.studio.unigo.utils.firebase.Message;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private List<MessageAdapterItem> messageList;
    private List<Boolean> msgListDetailToggle;
    private String photoUrl, name;
    private String idLastMsgRead;

    private class ViewHolderSender extends RecyclerView.ViewHolder
    {
        LinearLayout llSender;
        TextView txtSenderMsg, txtSenderDate;
        MaterialLetterIcon imgLastRead;

        ViewHolderSender(View v)
        {
            super(v);
            llSender = (LinearLayout) v.findViewById(R.id.llSender);
            txtSenderMsg = (TextView) v.findViewById(R.id.txtSenderMsg);
            txtSenderDate = (TextView) v.findViewById(R.id.txtSenderDate);
            imgLastRead = (MaterialLetterIcon) v.findViewById(R.id.imgLastRead);
        }
    }

    private class ViewHolderRecipient extends RecyclerView.ViewHolder
    {
        LinearLayout llRecipient;
        TextView txtRecipientMsg, txtRecipientDate;
        MaterialLetterIcon imgRecipientPhoto;

        ViewHolderRecipient(View v)
        {
            super(v);
            llRecipient = (LinearLayout) v.findViewById(R.id.llRecipient);
            txtRecipientMsg = (TextView) v.findViewById(R.id.txtRecipientMsg);
            txtRecipientDate = (TextView) v.findViewById(R.id.txtRecipientDate);
            imgRecipientPhoto = (MaterialLetterIcon) v.findViewById(R.id.imgRecipientPhoto);
        }
    }

    public MessageAdapter(String photoUrl, String name)
    {
        this.messageList = new ArrayList<>();
        this.msgListDetailToggle = new ArrayList<>();
        this.photoUrl = photoUrl;
        this.name = name;
        idLastMsgRead = "";

    }

    // Metodo che permette di attribuire un layout diverso a seconda del mittende del messaggio
    @Override
    public int getItemViewType(int position)
    {
        Message msg = messageList.get(position).getMessage();

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
            return new ViewHolderSender(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sender, parent, false));
        else
            return new ViewHolderRecipient(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_recipient, parent, false));
    }

    @Override
    public int getItemCount()
    {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        Message msg = messageList.get(position).getMessage();

        switch (holder.getItemViewType())
        {
            case 0:
                final ViewHolderSender senderHolder = (ViewHolderSender)holder;

                senderHolder.txtSenderMsg.setText(msg.message);
                senderHolder.txtSenderDate.setText(Util.formatDate(msg.date));

                if (msgListDetailToggle.get(position))
                    senderHolder.txtSenderDate.setVisibility(View.VISIBLE);
                else
                    senderHolder.txtSenderDate.setVisibility(View.GONE);

                if (messageList.get(position).getMsgKey().equals(idLastMsgRead))
                    senderHolder.imgLastRead.setVisibility(View.VISIBLE);
                else
                    senderHolder.imgLastRead.setVisibility(View.GONE);

                // Se non è presente una connessione o l'utente non ha impostato un'immagine profilo, viene visualizzata la lettera corrispondente al nome utente
                Picasso.with(senderHolder.imgLastRead.getContext()).load(photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(senderHolder.imgLastRead, new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError()
                    {
                        senderHolder.imgLastRead.setLetter(name);
                        senderHolder.imgLastRead.setShapeColor(Util.getLetterBackgroundColor(senderHolder.imgLastRead.getContext(), name));
                    }
                });

                senderHolder.llSender.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        if (senderHolder.txtSenderDate.getVisibility() == View.VISIBLE)
                            senderHolder.txtSenderDate.setVisibility(View.GONE);
                        else
                            senderHolder.txtSenderDate.setVisibility(View.VISIBLE);
                    }
                });
                break;

            case 1:
                final ViewHolderRecipient recipientHolder = (ViewHolderRecipient) holder;

                recipientHolder.txtRecipientMsg.setText(msg.message);
                recipientHolder.txtRecipientDate.setText(Util.formatDate(msg.date));

                // Viene utilizzata una lista di boolean per mappare la visibilità dell'icona del destinatario
                if (msgListDetailToggle.get(position))
                {
                    recipientHolder.imgRecipientPhoto.setVisibility(View.VISIBLE);
                    recipientHolder.txtRecipientDate.setVisibility(View.VISIBLE);
                }
                else
                {
                    recipientHolder.imgRecipientPhoto.setVisibility(View.INVISIBLE);
                    recipientHolder.txtRecipientDate.setVisibility(View.GONE);
                }

                // Se non è presente una connessione o l'utente non ha impostato un'immagine profilo, viene visualizzata la lettera corrispondente al nome utente
                Picasso.with(recipientHolder.imgRecipientPhoto.getContext()).load(photoUrl).placeholder(R.drawable.empty_profile_pic).fit().into(recipientHolder.imgRecipientPhoto, new Callback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onError()
                    {
                        recipientHolder.imgRecipientPhoto.setLetter(name);
                        recipientHolder.imgRecipientPhoto.setShapeColor(Util.getLetterBackgroundColor(recipientHolder.imgRecipientPhoto.getContext(), name));
                    }
                });

                // clickListener che mostra/nasconde la data di invio del messaggio
                recipientHolder.llRecipient.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        if (recipientHolder.txtRecipientDate.getVisibility() == View.VISIBLE)
                            recipientHolder.txtRecipientDate.setVisibility(View.GONE);
                        else
                            recipientHolder.txtRecipientDate.setVisibility(View.VISIBLE);
                    }
                });
                break;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads)
    {
        if (payloads.isEmpty())
            onBindViewHolder(holder, holder.getAdapterPosition());

        else
        {
            ViewHolderSender senderHolder = (ViewHolderSender) holder;
            if (!(boolean) payloads.get(0))
                senderHolder.imgLastRead.setVisibility(View.GONE);
            else
            {
                senderHolder.imgLastRead.setVisibility(View.VISIBLE);
                idLastMsgRead = messageList.get(position).getMsgKey();
            }
        }
    }

    // Metodo utilizato per inserire un messaggio in coda alla lista
    // 1) Se il messaggio in questione è quello del mittente, viene semplicemente visualizzato
    // 2) Se il messaggio proviene dal destinatario della conversazione, viene visualizzata la relativa immagine profilo
    //    solamente nell'ultimo messaggio da parte sua. Per mostrare/nascondere immagine profilo/data, viene utilizzata
    //    una lista di boolean, msgListeDetailToggle
    public void addElement(Message msg, String msgKey)
    {
        // Se il messaggio il primo messaggio della lista, allora l'icona utente viene visualizzata
        if (messageList.size() == 0)
        {
            msgListDetailToggle.add(true);
            messageList.add(new MessageAdapterItem(msg, msgKey));
        }
        // Altrimenti se *non* è il primo della conversazione...
        else
        {
            // Si controlla se il messaggio precedente ha lo stesso mittente del messaggio da inserire, in caso positivo
            // viene nascosta la penultima immagine profilo (con relativa data di invio del messaggio) e mostrata quella dell'ultimo messaggio
            if (messageList.get(messageList.size() - 1).getMessage().sender_id.equals(msg.sender_id))
            {
                msgListDetailToggle.set(msgListDetailToggle.size() - 1, false);
                notifyItemChanged(msgListDetailToggle.size() - 1);
            }
            msgListDetailToggle.add(true);
            messageList.add(new MessageAdapterItem(msg, msgKey));
        }
        notifyItemInserted(messageList.size() - 1);
    }

    public void setLastMsgRead(int nrUnreadMsg)
    {
        int i;

        for(i = messageList.size() - 1; i >= 0; i--)
            if (messageList.get(i).getMessage().sender_id.equals(Util.encodeEmail(Util.getCurrentUser().getEmail())))
                if (nrUnreadMsg > 0)
                    nrUnreadMsg--;
                else
                    break;

        if (i != -1)
        {
            if ((!idLastMsgRead.equals("")) && (!idLastMsgRead.equals(messageList.get(i).getMsgKey())))
                notifyItemChanged(getElementByKey(idLastMsgRead), false);
            notifyItemChanged(getElementByKey(messageList.get(i).getMsgKey()), true);
        }
    }

    private int getElementByKey(String key)
    {
        for(int i = messageList.size() - 1; i >= 0; i--)
            if (messageList.get(i).getMsgKey().equals(key))
                return i;
        return -1;
    }
}