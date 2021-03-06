package com.alxgrk.blendedbackground;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.alxgrk.blendedbackground.color.ColorPair;
import com.alxgrk.blendedbackground.color.DominatingBitmapColors;
import com.alxgrk.blendedbackground.color.Gradient;
import com.alxgrk.blendedbackground.util.UserDefinedColor;

import java.util.Observable;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

@Builder(builderMethodName = "hiddenBuilder")
@ToString
public class BlendedBackground extends Observable {

    private static final String TAG = "BlendedBackground";

    public static BlendedBackgroundBuilder builder(Context context, View parent) {
        String refTag = context.getResources().getString(R.string.bb_ref_tag);
        return hiddenBuilder().context(context).refTag(refTag).parentWidth(parent.getWidth()).parentHeight(parent.getHeight());
    }

    public static final class NoReferenceFoundException extends RuntimeException {
        NoReferenceFoundException() {
            super("No reference found. Add view:tag=\"@string/refTag\" to your layout xml " +
                    "or set both app:upper and app:lower.");
        }
    }

    private final Context context;

    @Getter
    private final ColorPair colors =  new ColorPair(Color.TRANSPARENT, Color.TRANSPARENT);

    @Getter
    private final String refTag;

    @Getter
    private View referencedView;

    /** MODIFIABLE ATTRIBUTES **/

    private UserDefinedColor upper;

    public @ColorInt int getUpper() {
        return upper.getColor() == null ? upper.getColor() : Color.TRANSPARENT;
    }

    public void setUpper(@ColorInt int color) {
        upper = new UserDefinedColor(color);
        colors.setUpper(color);
        setChanged();
        notifyObservers();
    }

    private UserDefinedColor lower;

    public @ColorInt int getLower() {
        return lower.getColor() == null ? lower.getColor() : Color.TRANSPARENT;
    }

    public void setLower(@ColorInt int color) {
        lower = new UserDefinedColor(color);
        colors.setLower(color);
        setChanged();
        notifyObservers();
    }

    @Getter
    private int parentWidth = 0;

    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
        setChanged();
        notifyObservers();
    }

    @Getter
    private int parentHeight = 0;

    public void setParentHeight(int parentHeight) {
        this.parentHeight = parentHeight;
        setChanged();
        notifyObservers();
    }

    @Getter
    private boolean upperBlendIn = false;

    public void setUpperBlendIn(boolean upperBlendIn) {
        this.upperBlendIn = upperBlendIn;
        setChanged();
        notifyObservers();
    }

    @Getter
    private boolean lowerBlendIn = false;

    public void setLowerBlendIn(boolean lowerBlendIn) {
        this.lowerBlendIn = lowerBlendIn;
        setChanged();
        notifyObservers();
    }

    @Getter
    private boolean invert = false;

    public void setInvert(boolean invert) {
        this.invert = invert;
        colors.invert();
        setChanged();
        notifyObservers();
    }

    @Getter
    private Gradient.GradientType gradientType = Gradient.GradientType.LINEAR;

    public void setGradientType(@NonNull Gradient.GradientType gradientType) {
        this.gradientType = gradientType;
        setChanged();
        notifyObservers();
    }

    public Drawable get() {
        Gradient gradient = new Gradient(parentWidth, parentHeight, colors.getUpper(), colors.getLower(), gradientType);
        return gradient.get();
    }

    public Drawable updateReferencedView(View newReference) {
        referencedView = newReference;

        return update();
    }

    private Drawable update() {
        if(null == referencedView) {
            if(!upper.isDefined() || !lower.isDefined())
                throw new NoReferenceFoundException();
        } else {
            calculateBackgroundFor(referencedView, colors);
        }

        applyUserDefinitions(colors);

        return get();
    }

    private void calculateBackgroundFor(@NonNull View child, @NonNull ColorPair colors) {
        Log.d(TAG, "Found " + getIdName(child) + " as corresponding view.");

        Drawable source;
        if (child instanceof ImageView) {
            source = ((ImageView) child).getDrawable();
        } else {
            source = child.getBackground();
        }

        if (null != source) {
            DominatingBitmapColors dominatingBitmapColors = new DominatingBitmapColors(((BitmapDrawable) source).getBitmap());
            ColorPair tmp = dominatingBitmapColors.getColors();

            colors.setUpper(tmp.getUpper());
            colors.setLower(tmp.getLower());

            Log.d(TAG, "Dominant colors: " + colors);
        } else {
            Log.e(TAG, "Could not retrieve image from specified view. Fallback to transparent background.");
        }
    }

    private String getIdName(@NonNull View view) {
        String idName;
        try {
            idName = context.getResources().getResourceName(view.getId());
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Child " + view + " does not have a user defined id.");
            idName = view.toString();
        }
        return idName;
    }

    private void applyUserDefinitions(@NonNull ColorPair colors) {
        Integer upperColor = upper.getColor();
        if(null != upperColor) {
            if(upperBlendIn) {
                colors.blendUpper(upperColor);
            } else {
                colors.setUpper(upperColor);
            }
        }

        Integer lowerColor = lower.getColor();
        if(null != lowerColor) {
            if(lowerBlendIn) {
                colors.blendLower(lowerColor);
            } else {
                colors.setLower(lowerColor);
            }
        }

        if(invert) {
            colors.invert();
        }
    }
}
