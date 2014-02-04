package wei.mark.standout;

import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.drawables.SemiCircleDrawable;
import wei.mark.standout.drawables.SemiCircleDrawable.Direction;
import wei.mark.standout.ui.Window;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

public abstract class SlideOnWindow extends StandOutWindow {

	private Class<? extends StandOutWindow> mServiceClass;
	private SparseArray<SliderWindowDetails> mSliderWindowDetails = new SparseArray<SliderWindowDetails>();
	private float mTouchStartX;
	private boolean mCloseOnRelease;
	private class SliderWindowDetails implements AnimatorListener {
		private ImageView mWindowHandle;
		private int mTranslation;
		private View mWindowView;
		private FrameLayout mContent;
		private int mButton;
		private Boolean mHideStateChanging = false;
		private int mId;
		private boolean mHidden;
		private int mPreviousWidth;
		private int mPreviousHeight;
		private float mTouchStartX;

		public SliderWindowDetails(int id, FrameLayout frame) {
			mId = id;
			int color = Color.argb(80, 0, 0, 0);
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			mWindowView = inflater.inflate(R.layout.slide_on_window, frame, true);
			mWindowView.setTag(id);
			mWindowView.addOnLayoutChangeListener(new OnLayoutChangeListener() {

				@Override
				public void onLayoutChange(View v, int left, int top, int right,
						int bottom, int oldLeft, int oldTop, int oldRight,
						int oldBottom) {
					if (mContent.getMeasuredWidth() > 0 && mTranslation == 0) {
						mTranslation = -mContent.getMeasuredWidth()
								+ mContent.getLeft();
						Window window = getWindow(mId);
						mPreviousWidth = window.getWidth();
						mPreviousHeight = window.getHeight();
						hide();
					}
				}
			});
			mContent = (FrameLayout) mWindowView.findViewById(R.id.container);
			mContent.setBackgroundColor(color);
			mWindowHandle = (ImageView) mWindowView
					.findViewById(R.id.window_handle);
			mWindowHandle.setBackground(new SemiCircleDrawable(color,
					Direction.RIGHT));


			View view = createView(inflater, mContent);
			if(null != view && null == view.getParent()) {
				mContent.addView(view);
			}
			updateIcon();
		}
		
		@Override
		public void onAnimationStart(Animator animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationRepeat(Animator animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animator animation) {
			synchronized(mHideStateChanging) {
				mHideStateChanging = false;
			}
			updateIcon();
		}
		
		@Override
		public void onAnimationCancel(Animator animation) {
			synchronized(mHideStateChanging) {
				mHideStateChanging = false;
			}
		}

		public void hide() {
			synchronized (mHideStateChanging) {
				if (mHideStateChanging || mHidden) return;
				
				mHideStateChanging = true;
			}

			mHidden = true;
			Window window = getWindow(mId);
			Log.d("AARON", "Window dimens: " + window.getWidth() + "x" + window.getHeight());
			int height = Math.min(mPreviousHeight, mWindowHandle.getMeasuredHeight());
			window.edit()
				.setAnchorPoint(0, .5f)
				.setSize(mPreviousWidth, height)
				.setPosition(mTranslation, (int) (mPreviousHeight / 2.0f))
				.commit();/*.animate().translationX(mTranslation)
					.setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(500)
					.setListener(this)
					.start();*/
			synchronized (mHideStateChanging) {				
				mHideStateChanging = false;
				updateIcon();
			}
		}

		public void show() {
			synchronized (mHideStateChanging) {
				if (mHideStateChanging) return;
				
				mHideStateChanging = true;
			}
			mHidden = false;
			getWindow(mId).edit()
				.setAnchorPoint(0, .5f)
				.setSize(mPreviousWidth, mPreviousHeight)
				.setPosition(0, (int) (mPreviousHeight / 2.0f))
				.commit();/*.animate().translationX(0)
					.setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(500)
					.setListener(this)
					.start();*/
			synchronized (mHideStateChanging) {				
				mHideStateChanging = false;
				updateIcon();
			}
		}
		
		public void toggle() {
			synchronized (mHideStateChanging) {
				if (mHideStateChanging) return;
			}
			if(isHidden()) {
				show();
			} else {
				hide();
			} 
		}

		public boolean isHidden() {
			return mHidden;
		}

		private void updateIcon() {
			if(isHidden()) {
				mWindowHandle.setImageResource(R.drawable.ic_action_open_slideon_window);
			} else {
				mWindowHandle.setImageResource(R.drawable.ic_action_close_slideon_window);				
			}
		}
	};

	public SlideOnWindow() {
	}

	/**
	 * Use this if you want to automatically set up closure notifications.
	 * @param serviceClass The class that is actually listed in the manifest. 
	 */
	public SlideOnWindow(Class<? extends StandOutWindow> serviceClass) {
		mServiceClass = serviceClass;
	}

	@Override
	final public void createAndAttachView(int id, FrameLayout frame) {
		mSliderWindowDetails .put(id, new SliderWindowDetails(id, frame));
	}
	
	public abstract View createView(LayoutInflater inflater, ViewGroup root);

	// the window will be centered
	@Override
	public StandOutLayoutParams getParams(int id, Window window) {
		return new StandOutLayoutParams(id, getWidthParam(),
				LayoutParams.MATCH_PARENT, StandOutLayoutParams.LEFT,
				StandOutLayoutParams.TOP);
	}

	public int getWidthParam() {
		// TODO Probably going to change this.
		return LayoutParams.WRAP_CONTENT;
	}

	// move the window by dragging the view
	@Override
	public int getFlags(int id) {
		return super.getFlags(id)
				| StandOutFlags.FLAG_BODY_MOVE_ENABLE_X
				| StandOutFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE
				| StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
				| StandOutFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE;
	}

	@Override
	public Intent getPersistentNotificationIntent(int id) {
		if(null != mServiceClass) {
			return StandOutWindow.getCloseIntent(this, mServiceClass, id);
		}
		return super.getPersistentNotificationIntent(id);
	}
	
	@Override
	public boolean onKeyEvent(int id, Window window, KeyEvent event) {
		if(KeyEvent.KEYCODE_BACK == event.getKeyCode() || KeyEvent.KEYCODE_HOME == event.getKeyCode()) {
			SliderWindowDetails details = mSliderWindowDetails.get(id);
			if(!details.isHidden()) {
				details.hide();
				return true;
				
			}
		}
		return super.onKeyEvent(id, window, event);
	}

	/**
	 * Internal touch handler for handling moving the window.
	 * 
	 * @see {@link View#onTouchEvent(MotionEvent)}
	 * 
	 * @param id
	 * @param window
	 * @param view
	 * @param event
	 * @return
	 */
	public boolean onTouchHandleMove(int id, Window window, View view,
			MotionEvent event) {
		StandOutLayoutParams params = window.getLayoutParams();

		// how much you have to move in either direction in order for the
		// gesture to be a move and not tap

		int totalDeltaX = window.touchInfo.lastX - window.touchInfo.firstX;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				params.y = 0;
				params.height = mSliderWindowDetails.get(id).mPreviousHeight;
				window.touchInfo.lastX = (int) event.getRawX();

				window.touchInfo.firstX = window.touchInfo.lastX;
				mTouchStartX = event.getX(); 
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) event.getRawX() - window.touchInfo.lastX;

				window.touchInfo.lastX = (int) event.getRawX();

				if (window.touchInfo.moving
						|| Math.abs(totalDeltaX) >= params.threshold) {
					mCloseOnRelease = deltaX < 0;
					window.touchInfo.moving = true;

					// if window is moveable
					if (Utils.isSet(window.flags,
							StandOutFlags.FLAG_BODY_MOVE_ENABLE_X)) {

						// update the position of the window
						if (event.getPointerCount() == 1) {
							if(Utils.isSet(window.flags,
									StandOutFlags.FLAG_BODY_MOVE_ENABLE_X)) {
								params.x += deltaX;
								Display display = mWindowManager.getDefaultDisplay();
								Point pt = new Point();
								display.getSize(pt);
								if(params.x + params.width > pt.x) {
									params.x = pt.x - params.width;
								}
							}
						}

						window.edit().setPosition(params.x, params.y).commit();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				window.touchInfo.moving = false;

				if (event.getPointerCount() == 1) {

					// bring to front on tap
					boolean tap = Math.abs(totalDeltaX) < params.threshold;
					if (tap && Utils.isSet(
									window.flags,
									StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP)) {
						SlideOnWindow.this.bringToFront(id);
					}
				}

				// bring to front on touch
				else if (Utils.isSet(window.flags,
						StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH)) {
					SlideOnWindow.this.bringToFront(id);
				}
			case MotionEvent.ACTION_CANCEL:
				if(Math.abs(totalDeltaX) < params.threshold) {
					mSliderWindowDetails.get(id).toggle();
				} else {
					if(mCloseOnRelease) {
						mSliderWindowDetails.get(id).hide();
					} else {
						mSliderWindowDetails.get(id).show();					
					}
				}
				return true;
		}

		onMove(id, window, view, event);

		return true;
	}
}
