package net.sharksystem.makan.android.viewadapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.sharksystem.R;
import net.sharksystem.SharkException;
import net.sharksystem.bubble.model.BubbleMessageStorage;
import net.sharksystem.makan.MakanException;
import net.sharksystem.makan.android.MakanApp;
import net.sharksystem.makan.android.MakanViewIntent;
import net.sharksystem.makan.android.model.MakanListStorage;

public class MakanListContentAdapter extends
        RecyclerView.Adapter<MakanListContentAdapter.MyViewHolder> implements View.OnClickListener {

    private static final String LOGSTART = "MakanListContentAdapter";
    private final Context ctx;
    private BubbleMessageStorage bubbleStorage;
    private CharSequence topic = null;
    private View.OnClickListener clickListener;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView uriTextView, nameTextView;

        public MyViewHolder(View view) {
            super(view);
            uriTextView = (TextView) view.findViewById(R.id.makan_list_row_uid);
            nameTextView = (TextView) view.findViewById(R.id.makan_list_row_name);
            view.setOnClickListener(clickListener);
        }
    }

    public MakanListContentAdapter(Context ctx) throws SharkException {
        Log.d(LOGSTART, "constructor");
        this.ctx = ctx;
        this.clickListener = this;
    }

    @Override
    public MakanListContentAdapter.MyViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        Log.d(LOGSTART, "onCreateViewHolder");

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.makan_list_row, parent, false);

        return new MakanListContentAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MakanListContentAdapter.MyViewHolder holder, int position) {
        Log.d(LOGSTART, "onBindViewHolder with position: " + position);

        /*
        I assume a bug or more probably - I'm too dull to understand recycler view at all.
        Here it comes: this method is called with position 0
        But that position is never displayed.

        So: I'm going to fake it until I understand the problem
        Fix: When position 0 called - I return a dummy message

        the other calls are handled as they should but with a decreased position
         */

        if(position == 0) {
            // dummy message
            holder.uriTextView.setText("dummy-uriTextView");
            holder.nameTextView.setText("dummy-nameTextView");
            return;
        }

        // else: position > 0

        // fake position
        position--;

        // go ahead
        try {
            MakanListStorage makanListStorage = MakanApp.getMakanApp().getMakanListStorage();
            if(makanListStorage == null) {
                Log.d(LOGSTART, "fatal: no makan list storage");
                return;
            }

            MakanInformation makanInfo = makanListStorage.getMakanInfo(position);
            if(makanInfo == null) {
                Log.d(LOGSTART, "fatal: no makan information");
                return;
            }

            holder.uriTextView.setText(makanInfo.getURI());
            holder.nameTextView.setText(makanInfo.getName());
        } catch (SharkException e) {
            Log.e(LOGSTART, "cannot access message storage (yet?)");
        }
    }

    @Override
    public int getItemCount() {
        Log.d(LOGSTART, "getItemCount");

        int realSize = 0;
        try {
            realSize = MakanApp.getMakanApp().getMakanListStorage().size();
        } catch (MakanException e) {
            Log.e(LOGSTART, "cannot access message storage (yet?)");
            return 0;
        }
        int fakeSize = realSize+1;
        return fakeSize;
    }

    @Override
    public void onClick(View view) {
        Log.d(LOGSTART, "click on view recognized");

        TextView uriTextView = (TextView) view.findViewById(R.id.makan_list_row_uid);
        Log.d(LOGSTART, "uri: " + uriTextView.getText());

        TextView nameTextView = (TextView) view.findViewById(R.id.makan_list_row_name);
        Log.d(LOGSTART, "name: " + nameTextView.getText());

        MakanViewIntent intent =
                new MakanViewIntent(ctx, nameTextView.getText(), uriTextView.getText());

        ctx.startActivity(intent);
    }
}
