/*    Liberario
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario.adapters;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.grobox.liberario.Preferences;
import de.grobox.liberario.R;
import de.grobox.liberario.utils.DateUtils;
import de.grobox.liberario.utils.LiberarioUtils;
import de.schildbach.pte.dto.Departure;

public class DepartureAdapter extends RecyclerView.Adapter<DepartureAdapter.DepartureHolder>{

	private SortedList<Departure> departures = new SortedList<>(Departure.class, new SortedList.Callback<Departure>(){
		@Override
		public void onInserted(int position, int count) {
			notifyItemRangeInserted(position, count);
		}

		@Override
		public void onChanged(int position, int count) {
			notifyItemRangeChanged(position, count);
		}

		@Override
		public void onMoved(int fromPosition, int toPosition) {
			notifyItemMoved(fromPosition, toPosition);
		}

		@Override
		public void onRemoved(int position, int count) {
			notifyItemRangeRemoved(position, count);
		}

		@Override
		public int compare(Departure d1, Departure d2) {
			return d1.getTime().compareTo(d2.getTime());
		}

		@Override
		public boolean areItemsTheSame(Departure d1, Departure d2) {
			return d1.equals(d2);
		}

		@Override
		public boolean areContentsTheSame(Departure d_old, Departure d_new) {
			// return whether the departures' visual representations are the same or not
			return d_old.equals(d_new);
		}
	});
	private int rowLayout;

	public DepartureAdapter(List<Departure> departures, int rowLayout) {
		this.rowLayout = rowLayout;

		addAll(departures);
	}

	@Override
	public DepartureHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);

		return new DepartureHolder(v);
	}

	@Override
	public void onBindViewHolder(final DepartureHolder ui, final int position) {
		final Departure dep = getItem(position);

		ui.time.setText(DateUtils.getTime(ui.time.getContext(), dep.plannedTime));

		if(dep.predictedTime != null) {
			long delay = 0;
			if(dep.plannedTime != null) {
				delay = dep.predictedTime.getTime() - dep.plannedTime.getTime();
			}

			if(delay != 0) {
				ui.delay.setText((delay > 0 ? "+" : "") + Long.toString(delay / 1000 / 60));
			}
		}

		ui.line.removeViewAt(2);
		if(dep.line != null) {
			LiberarioUtils.addLineBox(ui.line.getContext(), ui.line, dep.line, 2);

			ui.line.getChildAt(2).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
		}

		if(dep.destination != null) {
			ui.destination.setText(dep.destination.uniqueShortName());
		}

		// show platform/position according to user preference and availability
		if(dep.position != null) {
			ui.position.setText(dep.position.name);
			ui.position.setVisibility(Preferences.getPref(ui.position.getContext(), Preferences.SHOW_EXTRA_INFO) ? View.VISIBLE : View.GONE);
		}

		// show message if available
		if(dep.message != null) {
			ui.message.setText(dep.message);
		} else {
			ui.message.setVisibility(View.GONE);
		}
	}

	@Override
	public int getItemCount() {
		return departures == null ? 0 : departures.size();
	}

	public Departure getItem(int position) {
		return departures.get(position);
	}

	public Departure getEarliestItem() {
		return departures.get(0);
	}

	public Departure getLatestItem() {
		return departures.get(departures.size() - 1);
	}

	public void addAll(final List<Departure> departures) {
		this.departures.beginBatchedUpdates();

		for(final Departure departure : departures) {
			this.departures.add(departure);
		}

		this.departures.endBatchedUpdates();
	}

	public void clear() {
		this.departures.beginBatchedUpdates();

		while(departures.size() != 0) {
			departures.removeItemAt(0);
		}

		this.departures.endBatchedUpdates();
	}

	public static class DepartureHolder extends RecyclerView.ViewHolder {
		public ViewGroup line;
		public TextView time;
		public TextView delay;
		public TextView destination;
		public TextView position;
		public TextView message;

		public DepartureHolder(View v) {
			super(v);

			line = (ViewGroup) v.findViewById(R.id.lineLayout);
			time = (TextView) v.findViewById(R.id.timeView);
			delay = (TextView) v.findViewById(R.id.delayView);
			destination = (TextView) v.findViewById(R.id.destinationView);
			position = (TextView) v.findViewById(R.id.positionView);
			message = (TextView) v.findViewById(R.id.messageView);
		}
	}
}
