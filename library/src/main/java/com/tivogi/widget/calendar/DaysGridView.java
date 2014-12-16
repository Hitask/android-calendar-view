package com.tivogi.widget.calendar;

import hirondelle.date4j.DateTime;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class DaysGridView extends View {
	public class DayCell {
		private DateTime mDateTime;
		protected RectF mBound;
		private String mDayOfMonth;
		protected Paint mTextPaint;
		private PointF mTextOrigin;
		protected Paint mPaint;
		private boolean mPressed;
		private boolean mFocussed;
		private boolean mSelected;
		private boolean mHasDownEvent;
		private RectF mTodayRect;

		public DayCell() {
			mTextOrigin = new PointF();
			mBound = new RectF();
			mTodayRect = new RectF();
			mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
			mTextPaint.setTextSize(convertSpToPx(16));
			mPaint = new Paint();
			mPaint.setStyle(Style.FILL);
		}

		private void calculateTextOrigin() {
			mTextOrigin.x = mBound.centerX() - mTextPaint.measureText(mDayOfMonth) / 2;
			mTextOrigin.y = mBound.centerY() + mTextPaint.getTextSize() / 2;
		}

		protected void draw(Canvas canvas) {
			drawBackground(canvas);
			if (isToday()) {
				canvas.drawRect(mTodayRect, sTodayPaint);
			}
			drawDateText(canvas);
			if (isSelected()) drawSelected(canvas);
		}

		protected void drawBackground(Canvas canvas) {
			mPaint.setColor(isFromActiveMonth() ? DEFAULT_BACKGROUND_ACTIVE_COLOR : DEFAULT_BACKGROUND_INACTIVE_COLOR);
			canvas.drawRect(mBound, mPaint);
		}

		protected void drawDateText(Canvas canvas) {
			canvas.drawText(mDayOfMonth, mTextOrigin.x, mTextOrigin.y, mTextPaint);
		}

		protected void drawSelected(Canvas canvas) {
			mPaint.setColor(DEFAULT_SELECTED_COLOR);
			canvas.drawRect(mBound, mPaint);
		}

		public DateTime getDateTime() {
			return mDateTime;
		}

		public boolean isFocussed() {
			return mFocussed;
		}

		public boolean isFromActiveMonth() {
			return mDateTime.getMonth() == mActiveMonth;
		}

		public boolean isPressed() {
			return mPressed;
		}

		public boolean isSelected() {
			return mSelected;
		}

		public boolean isToday() {
			return mDateTime.isSameDayAs(DateTime.today(TimeZone.getDefault()));
		}

		public boolean isWeekend() {
			final int weekDay = mDateTime.getWeekDay();
			return weekDay == 1 || weekDay == 7;
		}

		protected void onClick() {
			onDayCellClick(this);
		}

		public boolean onTouchEvent(MotionEvent event) {
			if (!mBound.contains(event.getX(), event.getY())) return false;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mHasDownEvent = true;
				break;

			case MotionEvent.ACTION_CANCEL:
				mHasDownEvent = false;
				break;
			case MotionEvent.ACTION_UP:
				if (mHasDownEvent) onClick();
				mHasDownEvent = false;
				break;

			default:
				break;
			}
			return true;
		}

		public void setBound(RectF bound) {
			mBound.set(bound);
			mTodayRect.set(mBound);
			float delta = sTodayPaint.getStrokeWidth() / 2;
			mTodayRect.inset(delta, delta);
			calculateTextOrigin();
		}

		public void setDate(DateTime dateTime) {
			mDateTime = dateTime;
			mDayOfMonth = String.valueOf(mDateTime.getDay());
			int textColor = Color.GRAY;
			if (isFromActiveMonth()) {
				// TODO move to attributes of CalendarView
				textColor = isWeekend() ? Color.RED : Color.BLACK;
			}
			mTextPaint.setColor(textColor);
			calculateTextOrigin();
		}

		public void setFocussed(boolean focussed) {
			mFocussed = focussed;
		}

		public void setPressed(boolean pressed) {
			mPressed = pressed;
		}

		public void setSelected(boolean selected) {
			if (getCalendarView().isSelectable()) mSelected = selected;
		}
	}

	private static class UpdateDaysCellsTask extends AsyncTask<Void, Void, Void> {

		private WeakReference<DaysGridView> mWeakReference;

		public UpdateDaysCellsTask(DaysGridView view) {
			mWeakReference = new WeakReference<DaysGridView>(view);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			DaysGridView daysGridView = mWeakReference.get();
			if (daysGridView != null) {
				daysGridView.getCalendarView().setProgressVisible(true);
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			DaysGridView daysGridView = mWeakReference.get();
			if (daysGridView != null) {
				daysGridView.updateDaysCellsImpl();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			DaysGridView daysGridView = mWeakReference.get();
			if (daysGridView != null) {
				daysGridView.invalidate();
				daysGridView.getCalendarView().setProgressVisible(false);
			}
		}
	}

	protected static class WeekRow {
		protected static final int DAYS_COUNT = 7;
		protected DayCell[] mDays = new DayCell[DAYS_COUNT];

		protected void draw(Canvas canvas) {
			for (DayCell day : mDays) {
				day.draw(canvas);
			}
		}

		public DayCell[] getDays() {
			return mDays;
		}
	}

	protected static final int DEFAULT_TODAY_COLOR = Color.parseColor("#90F00000");
	protected static final int DEFAULT_SELECTED_COLOR = Color.parseColor("#33000000");

	private static final int WEEKS_COUNT = 6;

	public static float convertDpToPx(float dpValue) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, Resources.getSystem().getDisplayMetrics());
	}

	public static float convertSpToPx(float spValue) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, Resources.getSystem().getDisplayMetrics());
	}

	private int mActiveMonth;
	protected static final int DEFAULT_BACKGROUND_ACTIVE_COLOR = Color.WHITE;
	protected static final int DEFAULT_BACKGROUND_INACTIVE_COLOR = Color.parseColor("#ffeeeeee");
	private static Paint sTodayPaint;

	private static final int DEFAULT_GRID_LINE_WIDTH = 1; // size in pixels

	static {
		sTodayPaint = new Paint();
		sTodayPaint.setStyle(Paint.Style.STROKE);
		sTodayPaint.setColor(DEFAULT_TODAY_COLOR);
		sTodayPaint.setStrokeWidth(convertDpToPx(3));
	}

	public static int getWeekHeaderColumnCount() {
		return WeekRow.DAYS_COUNT;
	}

	private WeekRow[] mWeeks = new WeekRow[WEEKS_COUNT];

	private int mGridLineWidth;

	private CalendarView<?> mCalendarView;

	public DaysGridView(Context context) {
		super(context);
		initialize();
	}

	protected DayCell createEmptyDayCell() {
		return new DayCell();
	}

	protected CalendarView<?> getCalendarView() {
		return mCalendarView;
	}

	private int getIndexForDayOfWeek(DateTime dateTime) {
		switch (dateTime.getWeekDay()) {
		case 2: // MONDAY
			return 0;
		case 3: // TURSDAY
			return 1;
		case 4: // WEDNESDAY
			return 2;
		case 5: // THIRDAY
			return 3;
		case 6: // FRIDAY
			return 4;
		case 7: // SATURDAY
			return 5;
		default:// SUNDAY
			return 6;

		}
	}

	/**
	 * Return string with localized short name for day of the week. Call this method only after date was set.
	 * 
	 * @param index
	 *            column of grid. Must be range between 0 and {@link DaysGridView#getWeekHeaderColumnCount()}
	 * @return localized value
	 * 
	 */
	public String getWeekHeaderColumnTitle(int index) {
		return mWeeks[0].mDays[index].getDateTime().format("WWW", Locale.getDefault());
	}

	public WeekRow[] getWeeks() {
		return mWeeks;
	}

	private void initialize() {
		mGridLineWidth = DEFAULT_GRID_LINE_WIDTH;
		for (int i = 0; i < WEEKS_COUNT; i++) {
			mWeeks[i] = new WeekRow();
			for (int j = 0; j < WeekRow.DAYS_COUNT; j++) {
				mWeeks[i].mDays[j] = createEmptyDayCell();
			}
		}
	}

	protected void onDayCellClick(DayCell dayCell) {
		if (!getCalendarView().isSelectable()) return;
		if (!dayCell.isFromActiveMonth()) {
			mCalendarView.setDate(dayCell.getDateTime());
			return;
		}
		for (WeekRow week : mWeeks) {
			for (DayCell day : week.mDays) {
				day.setSelected(false);
			}
		}
		dayCell.setSelected(true);
		invalidate();
		mCalendarView.onDateClick(dayCell.getDateTime());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for (WeekRow week : mWeeks) {
			week.draw(canvas);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int cellWidth = (int) (w - mGridLineWidth * (WeekRow.DAYS_COUNT - 1)) / WeekRow.DAYS_COUNT;
		int cellHeight = (int) (h - mGridLineWidth * (WEEKS_COUNT - 1)) / WEEKS_COUNT;
		RectF cellBound = new RectF(0, 0, 0, 0);
		float currentLeft = 0, currentTop = 0;
		for (int y = 0; y < WEEKS_COUNT; y++) {
			currentLeft = 0;
			for (int x = 0; x < WeekRow.DAYS_COUNT; x++) {
				cellBound.left = currentLeft;
				cellBound.right = currentLeft + cellWidth;
				cellBound.top = currentTop;
				cellBound.bottom = currentTop + cellHeight;
				mWeeks[y].mDays[x].setBound(cellBound);
				currentLeft += cellWidth + mGridLineWidth;
			}
			currentTop += cellHeight + mGridLineWidth;
		}

		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		for (WeekRow week : mWeeks) {
			for (DayCell day : week.mDays) {
				if (day.onTouchEvent(event)) {
					return true;
				}
			}
		}
		return super.onTouchEvent(event);
	}

	public void setCalendarView(CalendarView<?> calendarView) {
		mCalendarView = calendarView;
	}

	public void setDate(DateTime dateTime) {
		mActiveMonth = dateTime.getMonth();
		dateTime = DateTime.forDateOnly(dateTime.getYear(), dateTime.getMonth(), 1);
		int dayOfWeekForMonthBegin = getIndexForDayOfWeek(dateTime);
		dateTime = dateTime.minusDays(dayOfWeekForMonthBegin);
		DateTime selectedDate = mCalendarView.getSelectedDate();
		for (WeekRow week : mWeeks) {
			for (DayCell day : week.mDays) {
				day.setDate(dateTime);
				day.setSelected(selectedDate != null && selectedDate.isSameDayAs(dateTime));
				dateTime = dateTime.plusDays(1);
			}
		}
		updateDaysCells();
	}

	public final void updateDaysCells() {
		new UpdateDaysCellsTask(this).execute();
	}

	protected void updateDaysCellsImpl() {
	}
}