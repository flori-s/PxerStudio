package com.benny.pxerstudio.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.benny.pxerstudio.databinding.ColorpickerPopupBinding;
import com.benny.pxerstudio.util.ContextKt;

/**
 * Created by BennyKok on 10/14/2016.
 */

public class ColorPicker {
    private final PopupWindow popupWindow;
    private final ColorpickerPopupBinding binding;

    public ColorPicker(Context c, int startColor, SatValView.OnColorChangeListener listener) {
        binding = ColorpickerPopupBinding.inflate(LayoutInflater.from(c));
        View contentView = binding.getRoot();
        contentView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        binding.colorpickerSatValView.withHueBar(binding.colorpickerHueSeekBar);
        binding.colorpickerSatValView.withAlphaBar(binding.colorpickerAlphaSeekBar);
        binding.colorpickerSatValView.setListener(listener);
        binding.colorpickerSatValView.setColor(startColor);

        popupWindow = new PopupWindow(contentView);
        // noinspection deprecation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#040B37")));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(ContextKt.convertDpToPixel(c, 8));
        }
        popupWindow.setHeight((int) ContextKt.convertDpToPixel(c, 292));
        popupWindow.setWidth((int) ContextKt.convertDpToPixel(c, 216));
    }

    public void show(View anchor) {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        } else {
            popupWindow.showAsDropDown(anchor, -popupWindow.getWidth() / 2 + anchor.getWidth() / 2, 0);
        }
    }

    public void setColor(int color) {
        binding.colorpickerSatValView.setColor(color);
    }

    public void onConfigChanges() {
        popupWindow.dismiss();
    }
}
