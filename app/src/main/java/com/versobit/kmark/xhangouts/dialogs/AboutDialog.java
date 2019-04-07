/*
 * Copyright (C) 2015-2016 Kevin Mark
 *
 * This file is part of XHangouts.
 *
 * XHangouts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XHangouts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XHangouts.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.xhangouts.dialogs;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.versobit.kmark.xhangouts.BuildConfig;
import com.versobit.kmark.xhangouts.R;

public final class AboutDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "fragment_dialog_about";

    @Override @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);

        TextView txtVersion = (TextView)v.findViewById(R.id.dialog_about_version);
        TextView txtDonate = (TextView)v.findViewById(R.id.dialog_about_donate);
        TextView txtImadev = (TextView)v.findViewById(R.id.dialog_about_imadev);
        Button btnGithub = (Button)v.findViewById(R.id.dialog_about_github);
        Button btnXda = (Button)v.findViewById(R.id.dialog_about_xda);
        TextView txtLicense = (TextView)v.findViewById(R.id.dialog_about_license);

        txtVersion.setText(BuildConfig.VERSION_NAME);
        txtDonate.setMovementMethod(new DonateLinkMovementMethod());
        txtImadev.setMovementMethod(LinkMovementMethod.getInstance());
        btnGithub.setOnClickListener(onButtonClick);
        btnXda.setOnClickListener(onButtonClick);
        txtLicense.setOnClickListener(onLicenseClick);

        return new AlertDialog.Builder(getActivity(), getTheme())
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
    }

    // Show a "Thank You!" toast on link click
    private final class DonateLinkMovementMethod extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer,  MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        Toast.makeText(getActivity(), R.string.dialog_about_thanks, Toast.LENGTH_LONG).show();
                        link[0].onClick(widget);
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }

                    return true;
                } else {
                    Selection.removeSelection(buffer);
                }
            }

            return super.onTouchEvent(widget, buffer, event);
        }
    }

    private final View.OnClickListener onButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String uri;
            switch (v.getId()) {
                case R.id.dialog_about_github:
                    uri = getActivity().getString(R.string.dialog_about_github_link);
                    break;
                case R.id.dialog_about_xda:
                    uri = getActivity().getString(R.string.dialog_about_xda_link);
                    break;
                default:
                    return;
            }
            getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        }
    };

    private final View.OnClickListener onLicenseClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new LicenseDialog().show(getFragmentManager(), LicenseDialog.FRAGMENT_TAG);
        }
    };
}
