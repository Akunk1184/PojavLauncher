package net.kdt.pojavlaunch.customcontrols.buttons;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.handleview.*;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.*;
import org.lwjgl.glfw.*;

public class ControlButton extends androidx.appcompat.widget.AppCompatButton implements OnLongClickListener
{
    private Paint mRectPaint;
    
    protected GestureDetector mGestureDetector;
    protected ControlData mProperties;
    protected SelectionEndHandleView mHandleView;

    protected boolean mModifiable = false;
    protected boolean mCanTriggerLongClick = true;

    protected boolean mChecked = false;

    public ControlButton(ControlLayout layout, ControlData properties) {
        super(layout.getContext());
        setPadding(4, 4, 4, 4);
        
        mGestureDetector = new GestureDetector(getContext(), new SingleTapConfirm());

        setOnLongClickListener(this);

        setProperties(properties);
        setModified(false);

        mHandleView = new SelectionEndHandleView(this);
        
        final TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        
        mRectPaint = new Paint();
        mRectPaint.setColor(value.data);
        mRectPaint.setAlpha(128);
    }

    public HandleView getHandleView() {
        return mHandleView;
    }

    public ControlData getProperties() {
        return mProperties;
    }

    public void setProperties(ControlData properties) {
        setProperties(properties, true);
    }

    public void setProperties(ControlData properties, boolean changePos) {
        mProperties = properties;

        properties.update();

        // com.android.internal.R.string.delete
        // android.R.string.
        setText(properties.name);
        if (changePos) {
            setTranslationX(moveX = properties.x);
            setTranslationY(moveY = properties.y);
        }

        if (properties.specialButtonListener == null) {
            // A non-special button or inside custom controls screen so skip listener
        } else if (properties.specialButtonListener instanceof View.OnClickListener) {
            setOnClickListener((View.OnClickListener) properties.specialButtonListener);
            // setOnLongClickListener(null);
            // setOnTouchListener(null);
        } else if (properties.specialButtonListener instanceof View.OnTouchListener) {
            // setOnLongClickListener(null);
            setOnTouchListener((View.OnTouchListener) properties.specialButtonListener);
        } else {
            throw new IllegalArgumentException("Field " + ControlData.class.getName() + ".specialButtonListener must be View.OnClickListener or View.OnTouchListener, but was " +
                properties.specialButtonListener.getClass().getName());
        }

        setLayoutParams(new FrameLayout.LayoutParams((int) properties.getWidth(), (int) properties.getHeight() ));
    }

    public void setBackground(){
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(mProperties.bgColor);
        gd.setStroke(computeStrokeWidth(mProperties.strokeWidth), mProperties.strokeColor);
        gd.setCornerRadius(computeCornerRadius(mProperties.cornerRadius));

        setBackground(gd);
    }

    public int computeStrokeWidth(float widthInPercent){
        float maxSize = Math.max(mProperties.getWidth(), mProperties.getHeight());
        return (int)((maxSize/2) * (widthInPercent/100));
    }

    public float computeCornerRadius(float radiusInPercent){
        float minSize = Math.min(mProperties.getWidth(), mProperties.getHeight());
        return (minSize/2) * (radiusInPercent/100);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);

        mProperties.setWidth(params.width);
        mProperties.setHeight(params.height);
        setBackground();

        
        // Re-calculate position
        mProperties.update();
        setTranslationX(mProperties.x);
        setTranslationY(mProperties.y);
        
        setModified(true);
    }

    @Override
    public void setTranslationX(float x) {
        super.setTranslationX(x);

        if (!mProperties.isDynamicBtn) {
            mProperties.x = x;
            mProperties.dynamicX = Float.toString(x / CallbackBridge.physicalWidth) + " * ${screen_width}";
            setModified(true);
        }
    }

    @Override
    public void setTranslationY(float y) {
        super.setTranslationY(y);

        if (!mProperties.isDynamicBtn) {
            mProperties.y = y;
            mProperties.dynamicY = Float.toString(y / CallbackBridge.physicalHeight) + " * ${screen_height}";
            setModified(true);
        }
    }
    
    public void updateProperties() {
        setProperties(mProperties);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mChecked) {
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), mProperties.cornerRadius, mProperties.cornerRadius, mRectPaint);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mCanTriggerLongClick && mModifiable) {
            if (mHandleView.isShowing()) {
                mHandleView.hide();
            } else {
                if (getParent() != null) {
                    ((ControlLayout) getParent()).hideAllHandleViews();
                }
                
                try {
                    mHandleView.show(this);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }
        
        return mCanTriggerLongClick;
    }


    private void setHolding(boolean isDown) {
        if (mProperties.holdAlt) {
            CallbackBridge.holdingAlt = isDown;
            MainActivity.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_LEFT_ALT,0,isDown);
            System.out.println("holdingAlt="+CallbackBridge.holdingAlt);
        } if (mProperties.keycodes[0] == LWJGLGLFWKeycode.GLFW_KEY_CAPS_LOCK) {
            CallbackBridge.holdingCapslock = isDown;
            //MainActivity.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_CAPS_LOCK,0,isDown);
            System.out.println("holdingCapslock="+CallbackBridge.holdingCapslock);
        } if (mProperties.holdCtrl) {
            CallbackBridge.holdingCtrl = isDown;
            MainActivity.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_LEFT_CONTROL,0,isDown);
            System.out.println("holdingCtrl="+CallbackBridge.holdingCtrl);
        } if (mProperties.keycodes[0] == LWJGLGLFWKeycode.GLFW_KEY_NUM_LOCK) {
            CallbackBridge.holdingNumlock = isDown;
            //MainActivity.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_NUM_LOCK,0,isDown);
            System.out.println("holdingNumlock="+CallbackBridge.holdingNumlock);
        } if (mProperties.holdShift) {
            CallbackBridge.holdingShift = isDown;
            MainActivity.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_LEFT_SHIFT,0,isDown);
            System.out.println("holdingShift="+CallbackBridge.holdingShift);
        } 
    }

    protected float moveX, moveY;
    protected float downX, downY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mModifiable) {
            mCanTriggerLongClick = false;
            if (event.getAction() == MotionEvent.ACTION_MOVE && CallbackBridge.isGrabbing() && mProperties.passThruEnabled) {
                MinecraftGLView v = ((ControlLayout) this.getParent()).findViewById(R.id.main_game_render_view);
                if (v != null) {
                    v.dispatchTouchEvent(event);
                    return true;
                }
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: // 0
                case MotionEvent.ACTION_POINTER_DOWN: // 5
                    if(!mProperties.isToggle){
                        setHolding(true);
                        sendKeyPresses(event, true);
                    }
                    break;
                case MotionEvent.ACTION_UP: // 1
                case MotionEvent.ACTION_CANCEL: // 3
                case MotionEvent.ACTION_POINTER_UP: // 6
                    if(mProperties.isToggle){
                        mChecked = !mChecked;
                        invalidate();
                        setHolding(mChecked);
                        sendKeyPresses(event, mChecked);
                        break;
                    }
                    setHolding(false);
                    sendKeyPresses(event,false);
                    break;
                default:
                    return false;
            }
            return true;

        } else {
            if (mGestureDetector.onTouchEvent(event)) {
                mCanTriggerLongClick = true;
                onLongClick(this);
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_DOWN:
                    mCanTriggerLongClick = true;
                    downX = event.getX();
                    downY = event.getY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    mCanTriggerLongClick = false;
                    moveX += event.getX() - downX;
                    moveY += event.getY() - downY;

                    if (!mProperties.isDynamicBtn) {
                        setTranslationX(moveX);
                        setTranslationY(moveY);
                    }
                    break;
            }
        }

        return super.onTouchEvent(event);
    }

    public void sendKeyPresses(MotionEvent event, boolean isDown){
        for(int keycode : mProperties.keycodes){
            if(keycode >= 0){
                MainActivity.sendKeyPress(keycode, CallbackBridge.getCurrentMods(), isDown);
            }else {
                super.onTouchEvent(event);
            }
        }
    }

    public void setModifiable(boolean isModifiable) {
        mModifiable = isModifiable;
    }
    
    private void setModified(boolean modified) {
        if (getParent() != null) {
            ((ControlLayout) getParent()).setModified(modified);
        }
    }
}