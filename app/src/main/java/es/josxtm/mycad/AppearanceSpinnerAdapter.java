package es.josxtm.mycad;

import android.database.DataSetObserver;
import android.view.*;
import android.widget.*;
import es.josxtm.mycad.ui.CadAppearance;
import static es.josxtm.mycad.ui.CadAppearance.Role.PANEL;

/** Decorates native dropdown rows without changing data, IDs, enabled items or selection. */
final class AppearanceSpinnerAdapter extends BaseAdapter {
 final SpinnerAdapter source;CadAppearance palette;
 AppearanceSpinnerAdapter(SpinnerAdapter source,CadAppearance palette){this.source=source;this.palette=palette;}
 public int getCount(){return source.getCount();}
 public Object getItem(int position){return source.getItem(position);}
 public long getItemId(int position){return source.getItemId(position);}
 public int getItemViewType(int position){return source.getItemViewType(position);}
 public int getViewTypeCount(){return source.getViewTypeCount();}
 public boolean hasStableIds(){return source.hasStableIds();}
 public boolean isEmpty(){return source.isEmpty();}
 public boolean areAllItemsEnabled(){return !(source instanceof ListAdapter)||((ListAdapter)source).areAllItemsEnabled();}
 public boolean isEnabled(int position){return !(source instanceof ListAdapter)||((ListAdapter)source).isEnabled(position);}
 public void registerDataSetObserver(DataSetObserver observer){source.registerDataSetObserver(observer);}
 public void unregisterDataSetObserver(DataSetObserver observer){source.unregisterDataSetObserver(observer);}
 public CharSequence[] getAutofillOptions(){return source.getAutofillOptions();}
 private View style(View row){if(row!=null){row.setBackgroundColor(palette.color(PANEL));AppearanceStyler.tree(row,palette,palette.color(PANEL));}return row;}
 public View getView(int position,View reuse,ViewGroup parent){return style(source.getView(position,reuse,parent));}
 public View getDropDownView(int position,View reuse,ViewGroup parent){return style(source.getDropDownView(position,reuse,parent));}
}
