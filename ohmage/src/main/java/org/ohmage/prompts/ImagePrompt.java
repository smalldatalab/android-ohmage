/*
 * Copyright (C) 2014 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.prompts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.ohmage.app.R;

import java.io.File;

/**
 * Created by cketcham on 1/24/14.
 */
public class ImagePrompt extends MediaPrompt {

    @Override
    public SurveyItemFragment getFragment() {
        return ImagePromptFragment.getInstance(this);
    }

    /**
     * A fragment which just shows the text of the message
     */
    public static class ImagePromptFragment extends PromptLauncherFragment<ImagePrompt> {

        private static final int REQUEST_CODE = 0;
        private File mFile;
        private boolean photoTaken = false;

        public static ImagePromptFragment getInstance(ImagePrompt prompt) {
            ImagePromptFragment fragment = new ImagePromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_image, container, true);
            Button launch = (Button) view.findViewById(R.id.launch);
            launch.setText(getLaunchButtonText());
            launch.setOnClickListener(this);
        }

        @Override protected String getLaunchButtonText() {
            return getString(R.string.take_image);
        }

        @Override public void onClick(View v) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mFile = MediaPrompt.getTemporaryResponseFile();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
            startActivityForResult(intent, REQUEST_CODE);
        }

        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == Activity.RESULT_OK) {
                // Sample down image quality to reduce memory reqs
                setValue(mFile);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
                options.inSampleSize = calculateInSampleSize(options, 200, 300);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
                ImageView imgView = (ImageView) this.getView().findViewById(R.id.img);
                imgView.getLayoutParams().height = 300;
                imgView.requestLayout();
                imgView.setImageBitmap(bitmap);

                ((Button) this.getView().findViewById(R.id.launch)).setVisibility(View.GONE);

                // Change the skip button to a retake button
                Button skipButton = (Button) this.getView().findViewById(R.id.skip);
                skipButton.setText("Retake Photo");
                skipButton.setVisibility(View.VISIBLE);

                Button okButton = (Button) this.getView().findViewById(R.id.ok);
                okButton.setText("Ok");
                photoTaken = true;
            }
        }

        @Override protected void onSkipPressed() {
            if(!photoTaken){
                super.onSkipPressed();
                if(mFile != null) {
                    mFile.delete();
                }
                ((Button) this.getView().findViewById(R.id.launch)).setVisibility(View.GONE);
            } else {
                // to prevent the activity from moving to the next prompt after retake
                resetAnswerState();

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                mFile = MediaPrompt.getTemporaryResponseFile();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
                startActivityForResult(intent, REQUEST_CODE);
            }
        }

        public int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }
}
