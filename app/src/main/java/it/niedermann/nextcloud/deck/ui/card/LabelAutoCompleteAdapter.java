package it.niedermann.nextcloud.deck.ui.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import butterknife.BindView;
import butterknife.ButterKnife;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.model.Label;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;
import it.niedermann.nextcloud.deck.util.ViewUtil;

public class LabelAutoCompleteAdapter extends BaseAdapter implements Filterable {
    public static final long CREATE_ID = Long.MIN_VALUE;
    private Context context;
    private List<Label> labelList = new ArrayList<>();
    private SyncManager syncManager;
    private long accountId;
    private long boardId;
    private LifecycleOwner owner;
    private Label createLabel;
    private String createLabelText;

    public LabelAutoCompleteAdapter(@NonNull LifecycleOwner owner, Context context, long accountId, long boardId) {
        this.owner = owner;
        this.context = context;
        this.accountId = accountId;
        this.boardId = boardId;
        syncManager = new SyncManager(context, null);
        createLabel = new Label();
        createLabel.setId(CREATE_ID);
        createLabel.setBoardId(boardId);
        createLabel.setAccountId(accountId);
        createLabelText = context.getResources().getString(R.string.label_create);
        createLabel.setColor("757575");
    }

    @Override
    public int getCount() {
        return labelList.size();
    }

    @Override
    public Label getItem(int position) {
        return labelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.dropdown_item_singleline, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }

        if (position < labelList.size() - 1) {
            holder.icon.setImageDrawable(
                    ViewUtil.getTintedImageView(
                            context,
                            R.drawable.ic_plus,
                            "#" + getItem(position).getColor()
                    )
            );
        } else {
            holder.icon.setImageDrawable(
                    ViewUtil.getTintedImageView(
                            context,
                            R.drawable.ic_label_grey600_24dp,
                            "#" + getItem(position).getColor()
                    )
            );
        }
        holder.label.setText(getItem(position).getTitle());
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            final FilterResults filterResults = new FilterResults();

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    ((Fragment) owner).getActivity().runOnUiThread(() -> {
                        //FIXME: please provide the board-ID, since the labels depend on the board. according method already exists!
                        LiveData<List<Label>> labelLiveData = syncManager.searchLabelByTitle(accountId, boardId, constraint.toString());
                        Observer<List<Label>> observer = new Observer<List<Label>>() {
                            @Override
                            public void onChanged(List<Label> labels) {
                                labelLiveData.removeObserver(this);
                                createLabel.setTitle(String.format(createLabelText, constraint));
                                if (labels != null) {
                                    labels.add(createLabel);
                                    filterResults.values = labels;
                                    filterResults.count = labels.size();
                                    publishResults(constraint, filterResults);
                                } else {
                                    List<Label> createLabels = new ArrayList<>();
                                    createLabels.add(createLabel);
                                    filterResults.values = createLabels;
                                    filterResults.count = createLabels.size();
                                }
                            }
                        };
                        labelLiveData.observe(owner, observer);
                    });
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    if (!labelList.equals(results.values)) {
                        labelList = (List<Label>) results.values;
                    }
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    static class ViewHolder {
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.label)
        TextView label;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
